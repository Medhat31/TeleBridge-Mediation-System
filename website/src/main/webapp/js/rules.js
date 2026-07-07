document.addEventListener('DOMContentLoaded', () => {

    const tbody = document.getElementById('rules-table-body');
    const sourceNodeSelect = document.getElementById('sourceNodeId');
    const destNodeSelect = document.getElementById('destinationNodeId');
    
    let rulesList = [];
    let nodesList = [];

    /**
     * Fetches both the list of nodes (for dropdowns/names) and the list of rules
     * in parallel from the backend, then populates the UI.
     */
    async function loadData() {
        try {
            // Fetch both in parallel
            const [nodes, rules] = await Promise.all([
                apiFetch('/nodes'),
                apiFetch('/rules')
            ]);
            nodesList = nodes;
            rulesList = rules;
            
            populateDropdowns();
            renderTable();
        } catch (error) {
            console.error("Failed to load rules data", error);
            if (typeof showToast === 'function') showToast("Failed to load rules: " + error.message, 'error');
        }
    }

    function getNodeName(id) {
        const node = nodesList.find(n => n.nodeId === id);
        return node ? node.nodeName : `Unknown Node (${id})`;
    }

    /**
     * Fills the "Source Node" and "Destination Node" select elements
     * in the modal based on the node types.
     */
    function populateDropdowns() {
        sourceNodeSelect.innerHTML = '';
        destNodeSelect.innerHTML = '';
        
        nodesList.forEach(node => {
            const option = `<option value="${node.nodeId}">${node.nodeName} (${node.nodeType})</option>`;
            if (node.nodeType === 'UPSTREAM') {
                sourceNodeSelect.innerHTML += option;
            } else {
                destNodeSelect.innerHTML += option;
            }
        });
    }

    /**
     * Builds the HTML table rows for the mediation rules and attaches
     * event listeners for the toggle, edit, and delete buttons.
     */
    function renderTable() {
        tbody.innerHTML = '';
        rulesList.forEach((rule) => {
            const tr = document.createElement('tr');
            
            const zeroIcon = rule.filterZeroDuration 
                ? '<i class="fa-solid fa-check-circle status-icon active" title="Filtering Enabled"></i>' 
                : '<i class="fa-solid fa-circle-xmark status-icon inactive" title="Filtering Disabled"></i>';
                
            const emergIcon = rule.filterEmergency 
                ? '<i class="fa-solid fa-check-circle status-icon active" title="Filtering Enabled"></i>' 
                : '<i class="fa-solid fa-circle-xmark status-icon inactive" title="Filtering Disabled"></i>';

            tr.innerHTML = `
                <td><strong>${getNodeName(rule.sourceNodeId)}</strong></td>
                <td><span class="badge badge-success">${getNodeName(rule.destinationNodeId)}</span></td>
                <td style="text-align: center;">${zeroIcon}</td>
                <td style="text-align: center;">${emergIcon}</td>
                <td>
                    <label class="toggle-switch">
                        <input type="checkbox" class="status-toggle" data-id="${rule.ruleId}" ${rule.active ? 'checked' : ''}>
                        <span class="toggle-slider"></span>
                    </label>
                </td>
                <td>
                    <div class="action-group">
                        <button class="btn-icon edit-btn" data-id="${rule.ruleId}" title="Edit"><i class="fa-solid fa-pen"></i></button>
                        <button class="btn-icon delete-btn" data-id="${rule.ruleId}" title="Delete"><i class="fa-solid fa-trash"></i></button>
                    </div>
                </td>
            `;
            tbody.appendChild(tr);
        });

        // Toggle listeners
        document.querySelectorAll('.status-toggle').forEach(checkbox => {
            checkbox.addEventListener('change', async (e) => {
                const id = e.target.getAttribute('data-id');
                const isActive = e.target.checked;
                try {
                    await apiFetch(`/rules/${id}/status?active=${isActive}`, { method: 'PUT' });
                } catch (error) {
                    if (typeof showToast === 'function') showToast("Failed to update rule status: " + error.message, 'error');
                    e.target.checked = !isActive;
                }
            });
        });

        // Delete listeners
        document.querySelectorAll('.delete-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const id = e.currentTarget.getAttribute('data-id');
                showConfirmModal("Delete Rule", "Are you sure you want to delete this mediation rule?", async () => {
                    try {
                        await apiFetch(`/rules/${id}`, { method: 'DELETE' });
                        loadData(); // Refresh
                        if (typeof showToast === 'function') showToast("Rule deleted successfully.", 'success');
                    } catch (error) {
                        if (typeof showToast === 'function') showToast("Failed to delete rule: " + error.message, 'error');
                    }
                });
            });
        });

        // Edit listeners
        document.querySelectorAll('.edit-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const id = parseInt(e.currentTarget.getAttribute('data-id'));
                const rule = rulesList.find(r => r.ruleId === id);
                if (rule) {
                    document.querySelector('#rule-modal .modal-title').innerText = "Edit Mediation Rule";
                    document.getElementById('rule-id').value = rule.ruleId;
                    document.getElementById('sourceNodeId').value = rule.sourceNodeId;
                    document.getElementById('destinationNodeId').value = rule.destinationNodeId;
                    document.getElementById('filterZeroDuration').checked = rule.filterZeroDuration;
                    document.getElementById('filterEmergency').checked = rule.filterEmergency;
                    openModal();
                }
            });
        });
    }

    // Modal Logic
    const modal = document.getElementById('rule-modal');
    const btnAdd = document.getElementById('btn-add-rule');
    const btnClose = document.getElementById('btn-close-modal');
    const btnCancel = document.getElementById('btn-cancel-modal');
    
    function openModal() {
        modal.classList.add('active');
    }

    function closeModal() {
        modal.classList.remove('active');
        document.getElementById('rule-form').reset();
        document.getElementById('rule-id').value = '';
        document.querySelector('#rule-modal .modal-title').innerText = "Create Mediation Rule";
    }

    btnAdd.addEventListener('click', () => {
        closeModal();
        openModal();
    });
    btnClose.addEventListener('click', closeModal);
    btnCancel.addEventListener('click', closeModal);
    
    document.getElementById('btn-save-modal').addEventListener('click', async (e) => {
        e.preventDefault();
        const id = document.getElementById('rule-id').value;
        const sourceNodeId = parseInt(sourceNodeSelect.value);
        const destinationNodeId = parseInt(destNodeSelect.value);
        const filterZeroDuration = document.getElementById('filterZeroDuration').checked;
        const filterEmergency = document.getElementById('filterEmergency').checked;

        if (isNaN(sourceNodeId) || isNaN(destinationNodeId)) {
            if (typeof showToast === 'function') showToast("Please select both a Source and Destination node.", 'warning');
            return;
        }

        const payload = {
            sourceNodeId,
            destinationNodeId,
            filterZeroDuration,
            filterEmergency,
            active: true
        };

        if(id) { payload.ruleId = parseInt(id); }

        try {
            await apiFetch(id ? `/rules/${id}` : '/rules', {
                method: id ? 'PUT' : 'POST',
                body: JSON.stringify(payload)
            });
            closeModal();
            loadData();
            if (typeof showToast === 'function') showToast(id ? "Rule updated successfully." : "Rule created successfully.", 'success');
        } catch (error) {
            if (typeof showToast === 'function') showToast("Failed to save rule: " + error.message, 'error');
        }
    });

    // Initial load
    loadData();
});
