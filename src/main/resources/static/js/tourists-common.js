
// Attach tourist badge click handlers (open in modal)
function attachTouristClickHandlers(scope) {
    scope.querySelectorAll('a.tourist-link').forEach(link => {
        link.addEventListener('click', function(e) {
            e.stopPropagation();
            e.preventDefault();
            openTouristViewModal(this.dataset.touristId);
        });
    });
}

async function openTouristViewModal(touristId) {
    const contentEl = document.getElementById('tvModalContent');
    contentEl.innerHTML = '<div class="text-center py-4"><div class="spinner-border" role="status"></div></div>';

    bootstrap.Modal.getOrCreateInstance(document.getElementById('touristViewModal')).show();

    fetch(`/tourists/${touristId}/fragment`)
        .then(resp => resp.text())
        .then(html => {
            contentEl.innerHTML = html;
            contentEl.querySelectorAll('.card-footer').forEach(el => el.style.display = 'none');
            const editBtn = document.getElementById('tvEditBtn');
            if (editBtn) editBtn.dataset.touristId = touristId;
            document.getElementById('tvOpenLink').href = `/tourists/${touristId}`;
            contentEl.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(el => new bootstrap.Tooltip(el));
        })
        .catch(() => {
            contentEl.innerHTML = '<div class="alert alert-danger m-2">Ошибка загрузки данных туриста</div>';
        });

}

async function submitCreateTourist() {
    const form = document.getElementById('modalTouristForm');
    const formData = new FormData(form);
    const errorEl = document.getElementById('ct-error');
    const editTouristId = form.dataset.editTouristId || null;

    const lastName = (formData.get('lastName') || '').trim();
    const firstName = (formData.get('firstName') || '').trim();
    const gender = formData.get('gender');
    const dateOfBirth = formData.get('dateOfBirth');

    if (!lastName || !firstName || !gender || !dateOfBirth) {
        errorEl.textContent = 'Заполните обязательные поля: Фамилия, Имя, Пол, Дата рождения';
        errorEl.classList.remove('d-none');
        return;
    }

    const contactInfo = [];
    let i = 0;
    while (formData.has(`contactInfo[${i}].type`)) {
        contactInfo.push({
            type: formData.get(`contactInfo[${i}].type`),
            value: formData.get(`contactInfo[${i}].value`)
        });
        i++;
    }

    const data = {
        lastName,
        firstName,
        middleName: (formData.get('middleName') || '').trim() || null,
        gender,
        dateOfBirth,
        certificationId: (formData.get('certificationId') || '').trim() || null,
        contactInfo
    };

    const url = editTouristId ? `/api/tourists/${editTouristId}` : '/api/tourists';
    const method = editTouristId ? 'PUT' : 'POST';

    try {
        const resp = await fetch(url, {
            method,
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(data)
        });

        if (!resp.ok) {
            errorEl.textContent = editTouristId ? 'Ошибка при сохранении туриста' : 'Ошибка при создании туриста';
            errorEl.classList.remove('d-none');
            return;
        }

        const tourist = await resp.json();
        const fullName = [tourist.lastName, tourist.firstName, tourist.middleName]
            .filter(Boolean).join(' ');

        if (editTouristId) {
            document.querySelectorAll(`.tourist-select option[value="${editTouristId}"]`).forEach(opt => {
                opt.textContent = fullName;
            });
        } else if (window.currentTouristSelect) {
            const option = new Option(fullName, tourist.id, true, true);
            window.currentTouristSelect.append(option).trigger('change');

            const row = window.currentTouristSelect.closest('tr');
            const certInput = row.find('input[name$=".certificationId"]');
            if (certInput.length && !certInput.val() && tourist.certificationId) {
                certInput.val(tourist.certificationId);
            }

            const viewBtn = row.find('.tourist-view-btn')[0];
            if (viewBtn) {
                viewBtn.dataset.touristId = tourist.id;
                viewBtn.classList.remove('d-none');
            }
        }

        bootstrap.Modal.getInstance(document.getElementById('touristEditModal')).hide();
    } catch (e) {
        errorEl.textContent = 'Ошибка соединения';
        errorEl.classList.remove('d-none');
    }
}

function openTouristEditModal(touristId) {
    const editModal = document.getElementById('touristEditModal');
    if (!editModal) {
        window.location.href = `/tourists/${touristId}/edit`;
        return;
    }

    const form = document.getElementById('modalTouristForm');
    document.getElementById('ct-error').classList.add('d-none');
    form.dataset.editTouristId = touristId;

    document.getElementById('touristEditModalLabel').textContent = 'Редактировать туриста';
    const submitBtn = editModal.querySelector('.modal-footer .btn-primary');
    if (submitBtn) submitBtn.innerHTML = '<i class="bi bi-save"></i> Сохранить';

    const fetchPromise = fetch(`/tourists/${touristId}/edit-fragment`).then(r => r.text());

    const viewModal = document.getElementById('touristViewModal');
    const viewModalInstance = bootstrap.Modal.getInstance(viewModal);
    const hiddenPromise = new Promise(resolve => {
        if (viewModalInstance) {
            viewModal.addEventListener('hidden.bs.modal', resolve, { once: true });
            viewModalInstance.hide();
        } else {
            resolve();
        }
    });

    Promise.all([fetchPromise, hiddenPromise])
        .then(([html]) => {
            document.getElementById('modal-form-fields').innerHTML = html;
            document.querySelectorAll('#modal-form-fields #contactInfoList .contact-row select')
                .forEach(applyContactType);
            bootstrap.Modal.getOrCreateInstance(editModal).show();
        })
        .catch((e) => {
            console.error('openTouristEditModal failed:', e);
            window.location.href = `/tourists/${touristId}/edit`;
        });
}