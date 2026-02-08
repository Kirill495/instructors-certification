ALTER TABLE grades
    alter column expires_in set default 0;
update grades set expires_in=0 where expires_in is null;