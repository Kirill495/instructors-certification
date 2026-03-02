let rowIndex = parseInt(document.getElementById('protocol-meta').dataset.rowCount, 10);

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
                            certificationId: tourist.certificationId  // Store certificationId in the option
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
    }).on('select2:select', function (e) {
        // When a tourist is selected, auto-fill the certification ID
        const selectedData = e.params.data;
        const certificationId = selectedData.certificationId;

        // Find the certification ID input in the same row
        const row = $(this).closest('tr');
        const certInput = row.find('input[name$=".certificationId"]');

        // Auto-fill only if the field is empty
        if (certInput.length && !certInput.val()) {
            certInput.val(certificationId || '');
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