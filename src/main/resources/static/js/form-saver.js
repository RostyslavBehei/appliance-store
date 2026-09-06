document.addEventListener("DOMContentLoaded", function() {
    const forms = document.querySelectorAll("form.js-save-form");

    forms.forEach(form => {
        const formIdentifier = form.id || window.location.pathname;
        const prefix = `autosave_${formIdentifier}_`;

        const inputs = form.querySelectorAll("input:not([type='hidden']):not([type='password']):not([type='file']):not([type='submit']), select, textarea");

        inputs.forEach(input => {
            if (input.dataset.noSave === "true") return;

            const key = prefix + input.name;
            const savedValue = localStorage.getItem(key);

            if (savedValue !== null) {
                if (input.type === "radio" || input.type === "checkbox") {
                    if (input.value === savedValue && !form.querySelector(`input[name="${input.name}"]:checked`)) {
                        input.checked = true;
                    }
                } else {
                    if (!input.value) {
                        input.value = savedValue;
                    }
                }
            }

            const saveToStorage = () => {
                if (input.type === "radio" || input.type === "checkbox") {
                    if (input.checked) {
                        localStorage.setItem(key, input.value);
                    }
                } else {
                    localStorage.setItem(key, input.value);
                }
            };

            input.addEventListener("input", saveToStorage);
            input.addEventListener("change", saveToStorage);
        });

        form.addEventListener("submit", () => {
            inputs.forEach(input => {
                localStorage.removeItem(prefix + input.name);
            });
        });
    });
});