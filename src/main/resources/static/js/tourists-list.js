// ── Search ───────────────────────────────────────────────────────────────
const searchInput = document.getElementById('searchString');
const clearButton = document.getElementById('clearSearch');
let searchTimeout;

function toggleClearButton() {
    clearButton.style.display = searchInput.value.trim().length > 0 ? 'block' : 'none';
}

clearButton.addEventListener('click', function () {
    searchInput.value = '';
    toggleClearButton();
    window.location.href = '/tourists';
});

searchInput.addEventListener('input', function () {
    clearTimeout(searchTimeout);
    toggleClearButton();
    const v = this.value.trim();
    if (v.length > 0 && v.length < 3) return;
    searchTimeout = setTimeout(() => {
        window.location.href = v.length >= 3
            ? '/tourists?search=' + encodeURIComponent(v)
            : '/tourists';
    }, 500);
});

// ── Highlight ────────────────────────────────────────────────────────────
function highlightSearchTerm() {
    const term = searchInput.value.trim();
    if (term.length < 3) return;
    const regex = new RegExp('(' + escapeRegex(term) + ')', 'gi');
    document.querySelectorAll('tbody td.tourist-name, tbody td.cert-id').forEach(td => {
        td.innerHTML = td.textContent.replace(
            regex,
            '<mark style="background-color:#ffeb3b;padding:2px 4px;border-radius:2px;">$1</mark>'
        );
    });
}

function escapeRegex(s) {
    return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

// ── Sort ─────────────────────────────────────────────────────────────────
function sortTable(field) {
    const p = new URLSearchParams(window.location.search);
    const newOrder = (p.get('sort') === field && (p.get('order') || 'asc') === 'asc') ? 'desc' : 'asc';
    p.set('sort', field);
    p.set('order', newOrder);
    window.location.href = '/tourists?' + p.toString();
}

// ── Init ─────────────────────────────────────────────────────────────────
if (searchInput.value.trim().length >= 3) highlightSearchTerm();
toggleClearButton();
searchInput.focus();
searchInput.setSelectionRange(searchInput.value.length, searchInput.value.length);

// ── Infinite Scroll ──────────────────────────────────────────────────────
(function () {
    const meta = document.getElementById('pagination-meta');
    if (!meta) return;

    let currentPage      = parseInt(meta.dataset.currentPage, 10);
    const totalPages     = parseInt(meta.dataset.totalPages, 10);
    const pageSize       = meta.dataset.pageSize;
    const sortField      = meta.dataset.sortField;
    const sortOrder      = meta.dataset.sortOrder;
    const search         = meta.dataset.search;

    const sentinel       = document.getElementById('scroll-sentinel');
    const loader         = document.getElementById('loading-indicator');
    const endMsg         = document.getElementById('end-of-list');
    const tbody          = document.getElementById('tourists-tbody');

    if (!tbody || !sentinel) return;

    if (currentPage >= totalPages - 1) endMsg.style.display = 'block';

    let isFetching = false;

    function buildUrl(page) {
        const p = new URLSearchParams();
        p.set('page', page);
        p.set('size', pageSize);
        if (sortField) p.set('sort', sortField);
        if (sortOrder) p.set('order', sortOrder);
        if (search)    p.set('search', search);
        return '/api/tourists?' + p.toString();
    }

    async function loadNextPage() {
        if (isFetching || currentPage >= totalPages - 1) return;
        isFetching = true;
        loader.style.display = 'block';

        try {
            const nextPage = currentPage + 1;
            const res = await fetch(buildUrl(nextPage));
            if (!res.ok) throw new Error('Network error');
            const data = await res.json();

            data.content.forEach(tourist => {
                // Insert before the sentinel row so it stays last
                tbody.insertBefore(buildRow(tourist), sentinel);
            });

            if (searchInput.value.trim().length >= 3) highlightSearchTerm();

            currentPage = nextPage;
            if (currentPage >= data.totalPages - 1) {
                endMsg.style.display = 'block';
                observer.disconnect();
            }
        } catch (err) {
            console.error('Infinite scroll error:', err);
        } finally {
            isFetching = false;
            loader.style.display = 'none';
        }
    }

    function buildRow(tourist) {
        const tr = document.createElement('tr');
        tr.className = 'clickable-row';
        tr.onclick = () => window.location.href = `/tourists/${tourist.id}`;

        const lastAssignment = tourist.assignments?.at(-1);
        const assignmentHtml = lastAssignment
            ? `<a href="/protocols/${lastAssignment.protocolId}"
                  class="badge bg-secondary me-1 assignment-info"
                  onclick="event.stopPropagation()">
                   ${lastAssignment.grade?.title ?? ''} до ${lastAssignment.validThrough ?? ''}
               </a>`
            : '';

        tr.innerHTML = `
            <td class="cert-id">${tourist.certificationId ?? ''}</td>
            <td class="tourist-name">${tourist.fullName ?? ''}</td>
            <td>${tourist.gender ?? ''}</td>
            <td>${tourist.dateOfBirth ?? ''}</td>
            <td>${assignmentHtml}</td>
        `;
        return tr;
    }

    // Observe the sentinel relative to tbody (the real scroll container)
    const observer = new IntersectionObserver(entries => {
        if (entries[0].isIntersecting) loadNextPage();
    }, {
        root: tbody,
        rootMargin: '100px',
        threshold: 0
    });

    if (currentPage < totalPages - 1) observer.observe(sentinel);

    // Belt-and-suspenders: also fire on tbody scroll
    tbody.addEventListener('scroll', () => {
        const { scrollTop, scrollHeight, clientHeight } = tbody;
        if (scrollHeight - scrollTop - clientHeight < 100) loadNextPage();
    });
})();
// ── End Infinite Scroll ──────────────────────────────────────────────────