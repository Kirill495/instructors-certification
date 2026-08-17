// Sorting function
function sortTable(field) {
    const urlParams = new URLSearchParams(window.location.search);
    const currentSort = urlParams.get('sort');
    const currentOrder = urlParams.get('order') || 'asc';

    // Toggle order if clicking the same field, otherwise default to asc
    let newOrder = 'asc';
    if (currentSort === field && currentOrder === 'asc') {
        newOrder = 'desc';
    }

    // Build new URL with sort parameters
    urlParams.set('sort', field);
    urlParams.set('order', newOrder);
    urlParams.set('order', newOrder);

    window.location.href = '/protocols?' + urlParams.toString();
}

// Initialize Bootstrap tooltips
const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
const tooltipList = [...tooltipTriggerList].map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl));

const searchInput = document.getElementById('searchString');
const clearButton = document.getElementById('clearSearch');
let searchTimeout;

// Show/hide clear button based on input content
function toggleClearButton() {
    if (searchInput.value.trim().length > 0) {
        clearButton.style.display = 'block';
    } else {
        clearButton.style.display = 'none';
    }
}

clearButton.addEventListener('click', function() {
    searchInput.value = '';
    toggleClearButton();
    window.location.href = '/protocols';
});

searchInput.addEventListener('input', function() {
    clearTimeout(searchTimeout);
    toggleClearButton();

    const searchValue = this.value.trim();

    // If less than 3 characters, clear the search
    if (searchValue.length < 3 && searchValue.length > 0) {
        return; // Do nothing, wait for more input
    }

    // Debounce: wait 500ms after user stops typing
    searchTimeout = setTimeout(function() {
        if (searchValue.length >= 3) {
            // Redirect with search parameter
            window.location.href = '/protocols?search=' + encodeURIComponent(searchValue);
        } else if (searchValue.length === 0) {
            // Clear search - show all protocols
            window.location.href = '/protocols';
        }
    }, 500);
});

// Highlight matching text in tourist names
function highlightSearchTerm() {
    const searchTerm = searchInput.value.trim();

    // Only highlight if search term is 3+ characters
    if (searchTerm.length < 3) {
        return;
    }

    // Find all tourist name links in the table
    const nameLinks = document.querySelectorAll('tbody a.tourist-link');
    nameLinks.forEach(link => {
        const originalText = link.textContent;
        // Create case-insensitive regex to find all matches

        const regex = new RegExp('(' + escapeRegex(searchTerm) + ')', 'gi');
        // Replace matches with highlighted version

        const highlightedText = originalText.replace(regex, '<mark style="background-color: #ffeb3b; padding: 2px 4px; border-radius: 2px;">$1</mark>');
        link.innerHTML = highlightedText;

    });
}

function escapeRegex(string) {
    return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

// Show clear button if there's initial value
toggleClearButton();

attachTouristClickHandlers(document);

// Run highlighting when page loads (if there's a search term)
if (searchInput.value.trim().length >= 3) {
    highlightSearchTerm();
}

// Auto-focus search input on page load and move cursor to end
searchInput.focus();
searchInput.setSelectionRange(searchInput.value.length, searchInput.value.length);

// ── Infinite Scroll ──────────────────────────────────────────────────────
(function () {
    const meta = document.getElementById('pagination-meta');
    if (!meta) return;

    let currentPage   = parseInt(meta.dataset.currentPage, 10);
    const totalPages  = parseInt(meta.dataset.totalPages, 10);
    const pageSize    = meta.dataset.pageSize;
    const sortField   = meta.dataset.sortField;
    const sortOrder   = meta.dataset.sortOrder;
    const search      = meta.dataset.search;

    const sentinel    = document.getElementById('scroll-sentinel');
    const loader      = document.getElementById('loading-indicator');
    const endMsg      = document.getElementById('end-of-list');
    const tableCard   = document.querySelector('.table-card');

    // If everything already fits on one page, show the "all loaded" message
    if (currentPage >= totalPages - 1) {
        endMsg.style.display = 'block';
    }

    let isFetching = false;

    function buildUrl(page) {
        const params = new URLSearchParams();
        params.set('page', page);
        params.set('size', pageSize);
        if (sortField) params.set('sort', sortField);
        if (sortOrder) params.set('order', sortOrder);
        if (search)    params.set('search', search);
        return '/protocols?' + params.toString();
    }

    async function loadNextPage() {
        if (isFetching || currentPage >= totalPages - 1) return;

        isFetching = true;
        loader.style.display = 'block';

        try {
            const nextPage = currentPage + 1;
            const response = await fetch(buildUrl(nextPage), {
                headers: { 'X-Requested-With': 'XMLHttpRequest' }
            });

            if (!response.ok) throw new Error('Network response was not ok');

            const html = await response.text();

            // Backend returns the tableRows fragment — just a <tbody> with <tr>s inside.
            // Wrap in a table so DOMParser can parse table elements correctly.
            const parser = new DOMParser();
            const doc    = parser.parseFromString('<table>' + html + '</table>', 'text/html');
            const newRows = doc.querySelectorAll('tbody tr');

            if (newRows.length > 0) {
                const tbody = document.querySelector('tbody');
                newRows.forEach(row => {
                    tbody.appendChild(document.adoptNode(row));
                });

                // Re-attach tourist badge modal handlers for newly added rows
                attachTouristClickHandlers(tbody);

                // Re-initialize tooltips for newly added badges
                tbody.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(el => {
                    new bootstrap.Tooltip(el);
                });

                // Re-run search highlight on new rows
                if (searchInput.value.trim().length >= 3) {
                    highlightSearchTerm();
                }
            }

            currentPage = nextPage;

            if (currentPage >= totalPages - 1) {
                endMsg.style.display = 'block';
                observer.disconnect(); // No more pages — stop observing
            }
        } catch (err) {
            console.error('Infinite scroll fetch failed:', err);
        } finally {
            isFetching = false;
            loader.style.display = 'none';
        }
    }

    // Use IntersectionObserver to watch the sentinel element.
    // The sentinel sits outside the scrollable .table-card div, so we
    // observe relative to the viewport.  If you prefer to trigger earlier,
    // increase the rootMargin (e.g. '200px').
    const observer = new IntersectionObserver((entries) => {
        if (entries[0].isIntersecting) {
            loadNextPage();
        }
    }, {
        root: tableCard,
        rootMargin: '100px',
        threshold: 0
    });

    if (currentPage < totalPages - 1) {
        observer.observe(sentinel);
    }

    // Also trigger on scroll inside the .table-card container
    // (the card has overflow-y: auto, so the sentinel may never reach
    // the viewport while the card is scrolled internally)
    if (tableCard) {
        tableCard.addEventListener('scroll', function () {
            const { scrollTop, scrollHeight, clientHeight } = tableCard;
            // Trigger when user is within 100px of the bottom of the card
            if (scrollHeight - scrollTop - clientHeight < 100) {
                loadNextPage();
            }
        });
    }
})();
// ── End Infinite Scroll ──────────────────────────────────────────────────

// ── Scroll to highlighted protocol ──────────────────────────────────────────
(function() {
    const hash = window.location.hash;
    if (!hash || !hash.startsWith('#protocol-')) return;

    const target = document.getElementById(hash.slice(1));
    if (!target) return;

    // .table-card has overflow-y: auto, so the scrollable container is the card,
    // not the page. We must scroll it manually to center the row.
    const tableCard = document.querySelector('.table-card');
    const cardRect  = tableCard.getBoundingClientRect();
    const rowRect   = target.getBoundingClientRect();
    const offset    = rowRect.top - cardRect.top - (tableCard.clientHeight / 2) + (target.offsetHeight / 2);
    tableCard.scrollBy({ top: offset, behavior: 'smooth' });

    // Wait for scroll to finish, then flash the highlight.
    // 'smooth' scroll typically takes ~300-500ms.
    setTimeout(() => {
        target.style.backgroundColor = '#fff3cd';

        // Hold the highlight, then fade out to the striped row color
        setTimeout(() => {
            target.style.transition = 'background-color 1.5s ease';
            const isOdd = target.rowIndex % 2 !== 0;
            target.style.backgroundColor = isOdd ? 'rgba(0,0,0,0.05)' : '#ffffff';
        }, 1500);

        // Clean up all inline styles after fade completes so CSS takes over cleanly
        setTimeout(() => {
            target.style.transition = '';
            target.style.backgroundColor = '';
        }, 3200);
    }, 400);
})();
// ── End scroll to highlighted protocol ──────────────────────────────────────
// ── Excel Export ────────────────────────────────────────────────────────────
async function downloadExcel(btn, exportUrl, modal) {
    const originalHtml = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status"></span> Экспорт...';

    try {
        const response = await fetch(exportUrl);
        if (!response.ok) throw new Error('Ошибка сервера: ' + response.status);

        const blob = await response.blob();
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;

        const disposition = response.headers.get('Content-Disposition');
        let filename = 'protocols.xlsx';
        if (disposition) {
            const rfc5987 = disposition.match(/filename\*=UTF-8''([^;]+)/i);
            const plain   = disposition.match(/filename="?([^";]+)"?/);
            if (rfc5987) filename = decodeURIComponent(rfc5987[1]);
            else if (plain) filename = plain[1];
        }
        a.download = filename;

        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);

        if (modal) modal.hide();
    } catch (err) {
        alert('Не удалось экспортировать: ' + err.message);
    } finally {
        btn.disabled = false;
        btn.innerHTML = originalHtml;
    }
}

document.getElementById('exportConfirmBtn').addEventListener('click', async function () {
    const dateFrom = document.getElementById('dateFrom').value;
    const dateTill = document.getElementById('dateTill').value;

    const params = new URLSearchParams();
    if (dateFrom) params.set('dateFrom', dateFrom);
    if (dateTill) params.set('dateTill', dateTill);

    const modal = bootstrap.Modal.getInstance(document.getElementById('exportModal'));
    await downloadExcel(this, '/api/report/protocols?' + params.toString(), modal);
});

document.getElementById('exportAllBtn').addEventListener('click', async function () {
    await downloadExcel(this, '/api/report/protocols', null);
});
// ── End Excel Export ─────────────────────────────────────────────────────────