// ── Contact type ──────────────────────────────────────────────────────────

function applyContactType(select) {
    const row   = select.closest('.contact-row');
    const input = row.querySelector('input:not([type="hidden"])');

    if (input._phoneMask) {
        input.removeEventListener('input', input._phoneMask);
        delete input._phoneMask;
    }
    input.removeAttribute('pattern');
    input.removeAttribute('title');

    if (select.value === 'PHONE_NUMBER') {
        const phonePattern = /^\+7 \d{3} \d{3}-\d{2}-\d{2}$/;
        input.type        = 'tel';
        input.placeholder = '+7 999 123-45-67';
        input.title       = 'Формат: +7 999 123-45-67';
        if (!input.value || phonePattern.test(input.value)) {
            input.pattern = phonePattern.source.slice(1, -1); // strip ^ and $
            input._phoneMask  = () => phoneMaskInput(input);
            input.addEventListener('input', input._phoneMask);
            if (input.value) phoneMaskInput(input);
        }
    } else if (select.value === 'EMAIL') {
        input.type        = 'email';
        input.placeholder = 'example@email.com';
    } else {
        input.type        = 'text';
        input.placeholder = '@username';
    }
}

function phoneMaskInput(input) {
    let digits = input.value.replace(/\D/g, '');
    if (digits.startsWith('8')) digits = '7' + digits.slice(1);
    if (digits.length > 0 && !digits.startsWith('7')) digits = '7' + digits;
    digits = digits.slice(0, 11);

    let out = '';
    if (digits.length > 0) out = '+' + digits[0];
    if (digits.length > 1) out += ' ' + digits.slice(1, 4);
    if (digits.length > 4) out += ' ' + digits.slice(4, 7);
    if (digits.length > 7) out += '-' + digits.slice(7, 9);
    if (digits.length > 9) out += '-' + digits.slice(9, 11);

    input.value = out;
    input.setSelectionRange(out.length, out.length);
}

// ── Contact list ──────────────────────────────────────────────────────────

function addContact() {
    const list     = document.getElementById('contactInfoList');
    const index    = list.children.length;
    const template = document.getElementById('contact-type-template');

    const select = document.createElement('select');
    select.name      = `contactInfo[${index}].type`;
    select.className = 'form-select';
    select.style.maxWidth = '160px';
    select.setAttribute('onchange', 'applyContactType(this)');
    Array.from(template.options).forEach(o => {
        select.appendChild(new Option(o.text, o.value));
    });

    const input = document.createElement('input');
    input.name      = `contactInfo[${index}].value`;
    input.className = 'form-control';

    const btn = document.createElement('button');
    btn.type      = 'button';
    btn.className = 'btn btn-outline-danger';
    btn.setAttribute('onclick', 'removeContact(this)');
    btn.textContent = '✕';

    const row = document.createElement('div');
    row.className = 'contact-row input-group mb-2';
    row.append(select, input, btn);

    list.appendChild(row);
    applyContactType(select);
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

// ── Init ──────────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('#contactInfoList .contact-row select').forEach(applyContactType);
});