# Publication Service — Design

Agreed 2026-08-17. Not yet implemented; see [STATUS](STATUS.md) for progress.

## Why split at all

On a codebase this size most microservice arguments (independent scaling, independent deploy) do
not apply. One argument does.

`ProtocolContent` reaches `Tourist`, and `Tourist` carries `contactinfo` — phone numbers and
Telegram details. `Protocol.status` also has a `DRAFT` value that must never be visible outside.
While publication lives inside the monolith, "do not leak a phone number" is a matter of
discipline: one careless mapper change is enough. After the split it becomes a physical guarantee —
the service has no such data in its database and cannot leak what it does not hold.

That is the reason for the boundary, and it drives every decision below.

## Data ownership

- The service gets its own Postgres **database** — not a schema in the existing one. Postgres has no
  cross-database queries without FDW, so a separate database is a real isolation boundary while a
  separate schema is merely a naming convention that any query with the right grants can cross.
- Same instance is fine to start with. Moving to a separate instance later is a config change.
- Its own Flyway with its own `flyway_schema_history`. Two Flyway instances over one schema conflict.
- The service must never read the monolith's `User` table. Shared user storage would restore exactly
  the coupling the split removes.

## Message flow

**State transfer, not event deltas.** One message carries a full snapshot of one protocol with all
its assignments. Deltas ("assignment added", "assignment removed") look cheaper but require that
the consumer receive every event in the right order and lose none — one missed event and the
replica diverges silently and permanently. A full snapshot repairs any prior history.

- **Kafka message key = protocol id.** It must be the message key, not a field in the payload:
  partitioning follows the key, and only that guarantees all messages for one protocol land in the
  same partition and are processed in send order. Without it an older snapshot can overwrite a
  newer one — a bug that reproduces once a month and takes a week to find.
- Choose the partition count up front and do not change it. Increasing partitions remaps keys and
  breaks ordering for messages in flight.
- **Consumer:** delete every row for the key, then insert from the message, **in a single database
  transaction**; acknowledge the offset only after that transaction commits. Kafka delivers
  at-least-once, so duplicates will happen — this design makes them harmless by construction rather
  than trying to detect them.
- **Deletion and un-finalization use tombstones** — a message with the key and a `null` payload,
  which the consumer treats as "delete by key, insert nothing". Without this path a deleted or
  reverted protocol stays in the public registry forever, which for an attestation registry is the
  worst kind of failure: not an outage, but a quiet lie.
- **Topic is `cleanup.policy=compact`.** The design already has the shape compaction expects — key
  is an entity id, value is full current state, `null` means delete. Compaction then makes the topic
  a replayable snapshot of the whole registry: the service database can be dropped and rebuilt from
  the topic, which turns it from something requiring backups into a cache. It also physically
  removes personal data once a tombstone is written, which plain retention would not.

## Producer side

Kafka does not solve the dual-write problem. Saving the protocol to Postgres and sending to Kafka
are two systems with no shared transaction: if the send fails after the commit the protocol is
finalized but unpublished; if the commit fails after the send, a protocol that does not exist has
been published. `@Transactional` around `kafkaTemplate.send()` does not help.

Use a **transactional outbox**: the monolith writes an outbox row in the same transaction as the
protocol, and a separate relay moves outbox rows to Kafka with acknowledgement.

## Payload

Implemented in `publication-contract` as `ProtocolSnapshot` containing a list of
`AssignmentSnapshot`. Decided 2026-08-18.

```
ProtocolSnapshot(version, protocolId, number, date, orderNumber, publishedAt, assignments)
AssignmentSnapshot(rowNum, lastName, firstName, middleName, grade, kindOfTourism, club,
                   assignmentDate, validUntil)
```

Named "snapshot" rather than "event" on purpose: the name has to defend the state-transfer
decision, or someone will start adding deltas to it.

Nesting the assignments inside the protocol is also deliberate — a flat list of assignments each
repeating a protocol id would make it possible to publish half a protocol, and the whole
idempotency scheme rests on that being impossible.

`ProtocolSnapshot` normalizes `assignments` with `List.copyOf` in its compact constructor; a record
otherwise stores the caller's mutable list by reference. A null list fails fast with an NPE from
`copyOf`, which is the wanted behaviour at the point the snapshot is built.

### What is excluded, and why

- **No internal ids** — `Tourist.id`, `Grade.id`, `KindOfTourism.id`. External consumers would start
  referencing them and they could never be changed again. `protocolId` is the exception: it is the
  idempotency key the consumer deletes by, it is never exposed through the public API, and keeping
  it in the payload (rather than reading it off the Kafka key) keeps a message a self-contained
  statement of fact — testable and dumpable without Kafka metadata.
- **No `certificationId`.** Historical Excel data shows the same person occasionally received a
  second number by mistake, so the field is not unique in practice. A non-unique field that looks
  like an identifier is worse in a public registry than no identifier at all.
- **No date of birth, gender or `contactInfo`.** This is the substance of the boundary.
- **No `ProtocolStatus`** — only `FINALIZED` is ever published, so the field would be a constant.
- **No `decisionType`.** The field exists in the entity and the schema but is never read or written
  anywhere; the original intent was to distinguish a new award from a renewal. Dead fields must not
  enter a contract — contracts are expensive to change, and it would be frozen there forever.

**Consequence, accepted knowingly:** the registry cannot distinguish namesakes. A record is
identified only by name, kind of tourism, grade and assignment date, so an external consumer may
credit one person with another's grade. The registry is a public copy of a paper document, not a
database about people.

### `validUntil` is nullable and means "no expiry"

`grades.expires_in` is a nullable column and the report query wraps it in `COALESCE(..., 0)`. Today
every grade has a term (5, 5, 10), so nothing breaks — but a term-less grade would yield
`assignmentDate.plusYears(0)`, i.e. a credential that expires the day it is issued.
`ReportService` already carries this latent bug.

So the monolith emits `null` for a grade with no term, the read-model column is nullable, and the
public API renders it as "бессрочно". `null` here means "no expiry", not "unknown" — it is a value,
not a gap.

Only `FINALIZED` protocols leave the monolith. `DRAFT` never does.

The payload carries **resolved values**, never foreign keys — the service has no `Grade` or
`KindOfTourism` tables to resolve them against. Notably the expiry date is **computed before
sending** (`assignmentDate.plusYears(grade.expiresInYears)`); shipping the year count instead would
force the service to hold the catalogue and the boundary would leak back.

**The payload is now the security boundary.** Whatever enters Kafka can leak. Never serialize the
`Protocol` JPA entity — `Tourist` would pull `contactinfo` along with it. Always an explicit DTO
with a fixed field list.

Version the payload from day one (a `version` field or a Kafka header). Under compaction the topic
holds messages written months ago and the consumer must still read them.

## Deliberate non-behaviour

Renaming a grade or changing its validity period in the monolith does **not** propagate to already
published records. Nothing republishes them, and the registry keeps the wording as of publication.

This is intended. An attestation is a historical document: the grade was awarded with that wording
and for that period. Rewriting issued credentials retroactively would be the actual bug. Recorded
here so it is not mistaken for a synchronisation defect later.

## Public API

External consumers are machines, so the monolith's `formLogin` and sessions do not apply. Start with
API keys in a header — they give a clear model of who the client is, what it may do and how to revoke
it, and quotas attach naturally. OAuth2 client credentials with JWT (`oauth2ResourceServer`) earn
their complexity once there are many clients.
