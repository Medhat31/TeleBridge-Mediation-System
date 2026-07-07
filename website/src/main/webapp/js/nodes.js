document.addEventListener('DOMContentLoaded', () => {

    const tbody = document.getElementById('nodes-table-body');
    let nodesList = [];

    /**
     * Fetches the complete list of configured network nodes from the API
     * and triggers a re-render of the nodes table.
     */
    async function loadNodes() {
        try {
            nodesList = await apiFetch('/nodes');
            renderTable();
        } catch (error) {
            console.error("Failed to load nodes", error);
            if (typeof showToast === 'function') showToast("Failed to load network nodes: " + error.message, 'error');
        }
    }

    /**
     * Builds the HTML table rows for the network nodes and attaches
     * event listeners for the toggle, edit, and delete buttons.
     */
    function renderTable() {
        tbody.innerHTML = '';
        nodesList.forEach((node) => {
            const tr = document.createElement('tr');
            
            const typeClass = node.nodeType === 'UPSTREAM' ? 'badge-primary' : 'badge-warning';
            
            tr.innerHTML = `
                <td><strong>${node.nodeName}</strong></td>
                <td><span class="badge ${typeClass}">${node.nodeType}</span></td>
                <td>${node.protocol}</td>
                <td>
                    <label class="toggle-switch">
                        <input type="checkbox" data-id="${node.nodeId}" class="status-toggle" ${node.active ? 'checked' : ''}>
                        <span class="toggle-slider"></span>
                    </label>
                </td>
                <td>
                    <div class="action-group">
                        <button class="btn-icon edit-btn" data-id="${node.nodeId}" title="Edit"><i class="fa-solid fa-pen"></i></button>
                        <button class="btn-icon delete-btn" data-id="${node.nodeId}" title="Delete"><i class="fa-solid fa-trash"></i></button>
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        });

        // Add event listeners for toggles
        document.querySelectorAll('.status-toggle').forEach(checkbox => {
            checkbox.addEventListener('change', async (e) => {
                const id = e.target.getAttribute('data-id');
                const isActive = e.target.checked;
                try {
                    await apiFetch(`/nodes/${id}/status?active=${isActive}`, { method: 'PUT' });
                } catch (error) {
                    if (typeof showToast === 'function') showToast("Failed to update status: " + error.message, 'error');
                    e.target.checked = !isActive; // revert
                }
            });
        });

        // Add event listeners for delete buttons
        document.querySelectorAll('.delete-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const id = e.currentTarget.getAttribute('data-id');
                showConfirmModal("Delete Node", "Are you sure you want to delete this network node?", async () => {
                    try {
                        await apiFetch(`/nodes/${id}`, { method: 'DELETE' });
                        loadNodes();
                        if (typeof showToast === 'function') showToast("Node deleted successfully.", 'success');
                    } catch (error) {
                        if (typeof showToast === 'function') showToast("Failed to delete node: " + error.message, 'error');
                    }
                });
            });
        });

        // Add event listeners for edit buttons
        document.querySelectorAll('.edit-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const id = parseInt(e.currentTarget.getAttribute('data-id'));
                const node = nodesList.find(n => n.nodeId === id);
                if (node) {
                    document.querySelector('#node-modal .modal-title').innerText = "Edit Network Node";
                    document.getElementById('node-id').value = node.nodeId;
                    document.getElementById('nodeName').value = node.nodeName;
                    document.getElementById('nodeType').value = node.nodeType;
                    document.getElementById('protocol').value = node.protocol;
                    
                    // Disable core infrastructure fields for existing nodes
                    document.getElementById('nodeType').disabled = true;
                    document.getElementById('protocol').disabled = true;

                    document.getElementById('username').value = node.username || '';
                    document.getElementById('password').value = node.password || ''; // Might be blanked by backend
                    openModal();
                }
            });
        });
    }

    // Modal Logic
    const modal = document.getElementById('node-modal');
    const btnAdd = document.getElementById('btn-add-node');
    const btnClose = document.getElementById('btn-close-modal');
    const btnCancel = document.getElementById('btn-cancel-modal');
    
    // Form fields
    const nodeForm = document.getElementById('node-form'); // Assumes we have a form inside the modal
    // If there isn't a form, we'll grab inputs by their IDs

    function openModal() {
        modal.classList.add('active');
    }

    function closeModal() {
        modal.classList.remove('active');
        document.getElementById('node-form').reset();
        document.getElementById('node-id').value = '';
        document.querySelector('#node-modal .modal-title').innerText = "Add Network Node";
        
        // Re-enable core infrastructure fields for new nodes
        document.getElementById('nodeType').disabled = false;
        document.getElementById('protocol').disabled = false;
    }

    btnAdd.addEventListener('click', () => {
        closeModal(); // Reset fields
        openModal();
    });
    btnClose.addEventListener('click', closeModal);
    btnCancel.addEventListener('click', closeModal);
    
    document.getElementById('btn-save-modal').addEventListener('click', async (e) => {
        e.preventDefault();
        const id = document.getElementById('node-id').value;
        const protocol = document.getElementById('protocol').value;
        const payload = {
            nodeName: document.getElementById('nodeName').value,
            nodeType: document.getElementById('nodeType').value,
            protocol: protocol,
            username: document.getElementById('username').value,
            password: document.getElementById('password').value
        };

        if(id) { payload.nodeId = parseInt(id); }

        try {
            await apiFetch(id ? `/nodes/${id}` : '/nodes', {
                method: id ? 'PUT' : 'POST',
                body: JSON.stringify(payload)
            });
            closeModal();
            loadNodes();
            if (typeof showToast === 'function') showToast(id ? "Node updated successfully." : "Node created successfully.", 'success');
        } catch (error) {
            if (typeof showToast === 'function') showToast("Failed to save node: " + error.message, 'error');
        }
    });

    // Initial load
    loadNodes();
});
