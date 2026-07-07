// Common UI utilities for the dashboard

/**
 * Shows a custom confirmation modal that matches the website's design.
 * @param {string} title The title of the modal
 * @param {string} message The confirmation message
 * @param {function} onConfirm Callback when user clicks Confirm
 */
function showConfirmModal(title, message, onConfirm) {
    // Check if modal already exists
    let modal = document.getElementById('custom-confirm-modal');
    
    if (!modal) {
        // Create modal HTML
        const modalHtml = `
            <div class="modal-overlay" id="custom-confirm-modal" style="display: none; align-items: center; justify-content: center; z-index: 9999;">
                <div class="modal-content" style="max-width: 400px;">
                    <div class="modal-header">
                        <h3 class="modal-title" id="confirm-modal-title">Confirm Action</h3>
                        <button class="modal-close" id="btn-confirm-close"><i class="fa-solid fa-times"></i></button>
                    </div>
                    <div class="modal-body">
                        <p id="confirm-modal-message" style="color: var(--text-secondary); margin-bottom: 20px;">Are you sure you want to proceed?</p>
                    </div>
                    <div class="modal-footer" style="justify-content: flex-end;">
                        <button class="btn btn-outline" id="btn-confirm-cancel">Cancel</button>
                        <button class="btn btn-danger" id="btn-confirm-ok" style="background: var(--danger); color: white; border-color: var(--danger);">Confirm</button>
                    </div>
                </div>
            </div>
        `;
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        modal = document.getElementById('custom-confirm-modal');
    }

    // Set text
    document.getElementById('confirm-modal-title').innerText = title;
    document.getElementById('confirm-modal-message').innerText = message;

    // Show modal
    modal.style.display = 'flex';
    modal.classList.add('active'); // Add active class to override visibility: hidden in CSS

    // Handle clicks
    const closeBtn = document.getElementById('btn-confirm-close');
    const cancelBtn = document.getElementById('btn-confirm-cancel');
    const okBtn = document.getElementById('btn-confirm-ok');

    // Remove old listeners by cloning
    const newOkBtn = okBtn.cloneNode(true);
    okBtn.parentNode.replaceChild(newOkBtn, okBtn);

    const closeModal = () => {
        modal.classList.remove('active');
        setTimeout(() => {
            modal.style.display = 'none';
        }, 300); // wait for fade out animation
    };

    closeBtn.onclick = closeModal;
    cancelBtn.onclick = closeModal;
    
    newOkBtn.onclick = () => {
        closeModal();
        if (typeof onConfirm === 'function') {
            onConfirm();
        }
    };
}

/**
 * Shows an elegant temporary toast notification.
 * @param {string} message The message to display
 * @param {string} type 'error', 'success', 'warning', or 'info'
 */
function showToast(message, type = 'error') {
    // Inject toast container if it doesn't exist
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        container.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 10000; display: flex; flex-direction: column; gap: 10px;';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    
    // Set colors based on type
    let bgColor = 'rgba(255, 60, 60, 0.9)'; // default error red
    let icon = '<i class="fa-solid fa-circle-exclamation"></i>';
    if (type === 'success') {
        bgColor = 'rgba(16, 185, 129, 0.9)';
        icon = '<i class="fa-solid fa-check-circle"></i>';
    } else if (type === 'warning') {
        bgColor = 'rgba(245, 158, 11, 0.9)';
        icon = '<i class="fa-solid fa-triangle-exclamation"></i>';
    } else if (type === 'info') {
        bgColor = 'rgba(59, 130, 246, 0.9)';
        icon = '<i class="fa-solid fa-info-circle"></i>';
    }

    toast.style.cssText = `
        background: ${bgColor};
        color: white;
        padding: 12px 20px;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.15);
        font-family: inherit;
        font-size: 0.9rem;
        display: flex;
        align-items: center;
        gap: 12px;
        backdrop-filter: blur(10px);
        transform: translateX(120%);
        transition: transform 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275), opacity 0.3s ease;
        opacity: 0;
    `;

    toast.innerHTML = `${icon} <span>${message}</span>`;
    container.appendChild(toast);

    // Trigger animation
    requestAnimationFrame(() => {
        toast.style.transform = 'translateX(0)';
        toast.style.opacity = '1';
    });

    // Remove after 4 seconds
    setTimeout(() => {
        toast.style.transform = 'translateX(120%)';
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}
