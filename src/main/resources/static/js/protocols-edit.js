let rowIndex = parseInt(document.getElementById('protocol-meta').dataset.rowCount, 10);
window.currentTouristSelect = null;

// Initialize Select2 for existing rows on page load
$(document).ready(function() {
    initializeSelect2($('.tourist-select'));
});

// Initialize Select2 with AJAX for tourist search
function initializeSelect2(element) {
    element.select2({
        theme: 'bootstrap-5',
        ajax: {
            url: '/api/tourists/search',
            dataType: 'json',
            delay: 250,
            data: function (params) {
                return {
                    query: params.term
                };
            },
            processResults: function (data) {
                return {
                    results: data.map(function(tourist) {
                        return {
                            id: tourist.id,
                            text: tourist.fullName,
                            certificationId: tourist.certificationId
                        };
                    })
                };
            },
            cache: true
        },
        placeholder: 'Начните вводить имя...',
        minimumInputLength: 2,
        language: {
            inputTooShort: function () {
                return 'Введите минимум 2 символа';
            },
            searching: function () {
                return 'Поиск...';
            },
            noResults: function () {
                return 'Туристы не найдены';
            }
        }
    }).on('select2:open', function () {
        const selectEl = this;
        setTimeout(function () {
            const dropdown = document.querySelector('.select2-dropdown');
            if (dropdown && !dropdown.querySelector('.ct-create-footer')) {
                const footer = document.createElement('div');
                footer.className = 'ct-create-footer border-top p-1';
                footer.innerHTML = '<button type="button" class="btn btn-sm btn-outline-success w-100"><i class="bi bi-person-plus"></i> Создать нового туриста</button>';
                footer.querySelector('button').addEventListener('mousedown', function (e) {
                    e.preventDefault();
                    e.stopPropagation();
                    const searchTerm = (dropdown.querySelector('.select2-search__field')?.value || '').trim();
                    $(selectEl).select2('close');
                    openCreateTouristModal($(selectEl), searchTerm);
                });
                dropdown.appendChild(footer);
            }
        }, 0);
    }).on('select2:select', function (e) {
        const selectedData = e.params.data;
        const row = $(this).closest('tr');

        // Auto-fill certification ID if empty
        const certInput = row.find('input[name$=".certificationId"]');
        if (certInput.length && !certInput.val()) {
            certInput.val(selectedData.certificationId || '');
        }

        // Show view button and set tourist ID
        const viewBtn = row.find('.tourist-view-btn')[0];
        if (viewBtn) {
            viewBtn.dataset.touristId = selectedData.id;
            viewBtn.classList.remove('d-none');
        }
    });
}

function addRow() {
    // Hide/show empty state and table
    const emptyState = document.getElementById('emptyState');
    const tableContainer = document.querySelector('.table-container');
    const tableBody = document.getElementById('tableBody');

    if (emptyState && emptyState.style.display !== 'none') {
        emptyState.style.display = 'none';
        // Create table if it doesn't exist
        if (!tableContainer) {
            const cardBody = document.querySelector('.card-body.p-0');
            const container = document.createElement('div');
            container.className = 'table-container';
            container.innerHTML = `
                <table class="protocol-table" id="protocolTable">
                    <thead>
                        <tr>
                            <th class="row-num-cell">#</th>
                            <th class="tourist-col">Турист <span class="text-danger">*</span></th>
                            <th class="kind-col">Вид туризма <span class="text-danger">*</span></th>
                            <th class="grade-col">Звание <span class="text-danger">*</span></th>
                            <th class="cert-col">№ удостоверения</th>
                            <th class="club-col">Клуб</th>
                            <th class="delete-cell"></th>
                        </tr>
                    </thead>
                    <tbody id="tableBody"></tbody>
                </table>
            `;
            cardBody.appendChild(container);

            // Add bottom button
            const bottomButton = document.createElement('div');
            bottomButton.className = 'p-3 border-top';
            bottomButton.innerHTML = `
                <button type="button" class="btn btn-outline-primary btn-sm" onclick="addRow()">
                    <i class="bi bi-plus-circle"></i> Добавить строку
                </button>
            `;
            cardBody.appendChild(bottomButton);
        }
    }

    // Get template
    const template = document.getElementById('rowTemplate');
    let rowHtml = template.innerHTML;

    // Replace placeholders
    rowHtml = rowHtml.replaceAll('{INDEX}', rowIndex);
    rowHtml = rowHtml.replaceAll('{ROW_NUM}', rowIndex + 1);

    // Add to table body
    const tbody = document.getElementById('tableBody') || document.querySelector('#protocolTable tbody');
    tbody.insertAdjacentHTML('beforeend', rowHtml);

    // Initialize Select2 for the new tourist select
    const newSelect = $('#tourist-' + rowIndex);
    initializeSelect2(newSelect);

    rowIndex++;
}

function deleteRow(index) {
    if (confirm('Вы уверены, что хотите удалить эту строку?')) {
        const row = document.getElementById('row-' + index);

        // Destroy Select2 before removing row
        $(row).find('.tourist-select').select2('destroy');

        row.remove();

        // Show empty state if no rows left
        const tbody = document.getElementById('tableBody');
        const rows = tbody ? tbody.querySelectorAll('tr') : [];

        if (rows.length === 0) {
            const emptyState = document.getElementById('emptyState');
            const tableContainer = document.querySelector('.table-container');
            const bottomButton = document.querySelector('.p-3.border-top');

            if (emptyState) {
                emptyState.style.display = 'block';
            }
            if (tableContainer) {
                tableContainer.remove();
            }
            if (bottomButton) {
                bottomButton.remove();
            }
        }

        // Renumber remaining rows
        renumberRows();
    }
}

function openCreateTouristModal(selectEl, prefillName) {
    window.currentTouristSelect = selectEl;

    const form = document.getElementById('modalTouristForm');
    form.reset();
    delete form.dataset.editTouristId;
    document.getElementById('contactInfoList').innerHTML = '';
    document.getElementById('ct-error').classList.add('d-none');

    document.getElementById('touristEditModalLabel').textContent = 'Новый турист';
    const submitBtn = document.querySelector('#touristEditModal .modal-footer .btn-primary');
    if (submitBtn) submitBtn.innerHTML = '<i class="bi bi-person-plus"></i> Создать';

    if (prefillName) {
        const parts = prefillName.split(/\s+/);
        document.getElementById('lastName').value = parts[0];
        if (parts[1]) {
            document.getElementById('firstName').value = parts[1];
        }
        if (parts[2]) {
            document.getElementById('middleName').value = parts[2];
        }
    }

    new bootstrap.Modal(document.getElementById('touristEditModal')).show();
}

function renumberRows() {
    const rows = document.querySelectorAll('#tableBody tr');
    rows.forEach((row, index) => {
        const rowNumSpan = row.querySelector('.row-number');
        if (rowNumSpan) {
            rowNumSpan.textContent = index + 1;
        }

        const rowNumInput = row.querySelector('.row-num-input');
        if (rowNumInput) {
            rowNumInput.value = index + 1;
        }
    });
}