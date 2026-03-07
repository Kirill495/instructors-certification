function addContact() {
    const list = document.getElementById('contactInfoList');
    const index = list.children.length;
    const row = document.createElement('div');
    row.className = 'contact-row input-group mb-2';
    row.innerHTML = `
            <select name="contactInfo[${index}].type" class="form-select" style="max-width: 160px;">
                <option value="PHONE_NUMBER">Телефон</option>
                <option value="EMAIL">E-mail</option>
                <option value="TELEGRAM">Telegram</option>
            </select>
            <input name="contactInfo[${index}].value" type="text" class="form-control" placeholder="Значение">
            <button type="button" class="btn btn-outline-danger" onclick="removeContact(this)">✕</button>
        `;
    list.appendChild(row);
}

function removeContact(btn) {
    btn.closest('.contact-row').remove();
    reindex();
}

function reindex() {
    document.querySelectorAll('#contactInfoList .contact-row').forEach((row, i) => {
        row.querySelectorAll('[name]').forEach(el => {
            el.name = el.name.replace(/contactInfo\[\d+\]/, `contactInfo[${i}]`);
        });
    });
}
