'use strict';

(function () {
    const listElement = document.querySelector('#service-list');
    const addForm = document.querySelector('#add-form');
    const editForm = document.forms['edit-form'];
    const editModal = document.querySelector('#edit-modal');

    function escapeHtml(unsafe) {
        return (unsafe + '')
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function registerHandlerForPollerUpdates() {
        const eventBus = new EventBus('/eventbus');
        eventBus.onopen = () => {
            eventBus.registerHandler('poller.updates', (error, message) => {
                try {
                    const statusById = JSON.parse(message.body);
                    for (let [id, status] of Object.entries(statusById)) {
                        const statusEl = document
                            .querySelector(`.service-list-item[data-id="${id}"] .service-list-item-status`);
                        if (statusEl) {
                            statusEl.classList.remove('unknown', 'ok', 'fail');
                            statusEl.classList.add(status.toLowerCase());
                            statusEl.setAttribute('title', status);
                        }
                    }
                } catch (e) {
                    console.error('Poller updates error: ', e);
                }
            });
        }
    }

    function renderServices(services) {
        const items = services.map(service => {
            return `
                <div class="service-list-item" data-id="${escapeHtml(service.id)}">
                    <div class="service-list-item-status ${escapeHtml(service.status.toLowerCase())}" title="${escapeHtml(service.status)}"></div>
                    <div class="service-list-item-info">
                        <h2 class="service-list-item-info-title">${escapeHtml(service.name)}</h2>
                        <a href="${encodeURI(service.url)}" target="_blank" rel="noopener" class="service-list-item-info-link">${escapeHtml(service.url)}</a>
                    </div>
                    <div class="service-list-item-actions">
                        <button class="service-list-item-actions-delete" data-id="${escapeHtml(service.id)}">Delete</button>
                        <button class="service-list-item-actions-edit" data-id="${escapeHtml(service.id)}">Edit</button>
                    </div>
                </div>
            `;
        });

        while (listElement.firstChild) {
            listElement.removeChild(listElement.firstChild);
        }

        listElement.insertAdjacentHTML("afterbegin", items.join(''));
    }

    function deleteService(id) {
        return fetch('/service/' + id, {
            method: 'delete',
            headers: {
                'Accept': 'application/json, text/plain, */*',
                'Content-Type': 'application/json'
            }
        });
    }

    function getService(id) {
        return fetch('/service/' + id, {
            method: 'get',
            headers: {
                'Accept': 'application/json, text/plain, */*',
                'Content-Type': 'application/json'
            }
        }).then((response) => response.json());
    }

    function loadServices() {
        let servicesRequest = new Request('/service');
        fetch(servicesRequest)
            .then((response) => response.json())
            .then(services => renderServices(services));
    }

    function saveService(url, name) {
        return fetch('/service', {
            method: 'post',
            headers: {
                'Accept': 'application/json, text/plain, */*',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({url, name})
        });
    }

    function updateService(id, url, name) {
        return fetch('/service/' + id, {
            method: 'put',
            headers: {
                'Accept': 'application/json, text/plain, */*',
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({url, name})
        });
    }

    function openEditModal(serviceId) {
        getService(serviceId).then(service => {
            editForm.elements['name'].value = service.name;
            editForm.elements['url'].value = service.url;
            editForm.setAttribute('data-id', service.id);
            editModal.classList.remove('hidden');
        });
    }

    function closeEditModal() {
        editModal.classList.add('hidden');
        editForm.reset();
    }

    addForm.addEventListener('submit', evt => {
        evt.preventDefault();

        const data = new FormData(addForm);
        let url = data.get("url");
        let name = data.get("name");

        addForm.reset();

        saveService(url, name).then(res => loadServices());
    }, false);

    editForm.addEventListener('submit', evt => {
        evt.preventDefault();

        const data = new FormData(editForm);
        let url = data.get("url");
        let name = data.get("name");
        let id = editForm.getAttribute('data-id');

        editForm.querySelector('[type="submit"]').setAttribute('disabled', 'disabled');

        updateService(id, url, name).then(res => {
            loadServices();
            closeEditModal();
        }).finally(() => {
            editForm.querySelector('[type="submit"]').removeAttribute('disabled');
        });
    }, false);

    listElement.addEventListener('click', evt => {
        let target = evt.target;
        if (target.classList.contains('service-list-item-actions-delete')) {
            let serviceId = target.getAttribute('data-id');
            if (serviceId) {
                deleteService(serviceId).then(res => loadServices());
            }
        }

        if (target.classList.contains('service-list-item-actions-edit')) {
            let serviceId = target.getAttribute('data-id');
            if (serviceId) {
                openEditModal(serviceId);
            }
        }
    }, false);

    editModal.querySelector('.modal-content-close').addEventListener('click', evt => {
        closeEditModal();
    });

    document.addEventListener('DOMContentLoaded', () => {
        loadServices();
        registerHandlerForPollerUpdates();
    }, false)
})();
