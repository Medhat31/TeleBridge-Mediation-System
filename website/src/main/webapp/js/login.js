/**
 * Handles the logic for the login page, including client-side validation
 * and communicating with the authentication API to receive a JWT token.
 */
document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    const emailInput = document.getElementById('emailInput');
    const passwordInput = document.getElementById('passwordInput');
    const submitBtn = loginForm.querySelector('button[type="submit"]');

    // Check URL parameters for session expiry messages
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('error') === 'session_expired') {
        showToast("Your session has expired. Please log in again.", 'warning');
    }

    // Use 'click' on the button instead of 'submit' on the form,
    // so we bypass HTML5 validation and handle everything ourselves.
    submitBtn.addEventListener('click', async (e) => {
        e.preventDefault();

        const email = emailInput.value.trim();
        const password = passwordInput.value;

        // Client-side validation with our own toast messages
        if (!email) {
            showToast("Please enter your email address.", 'warning');
            emailInput.focus();
            return;
        }
        if (!password) {
            showToast("Please enter your password.", 'warning');
            passwordInput.focus();
            return;
        }

        // Disable button to prevent double-clicks
        submitBtn.disabled = true;
        submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Signing in...';

        try {
            // Detect context path dynamically
            const path = window.location.pathname;
            const lastSlash = path.lastIndexOf('/');
            const contextPath = lastSlash > 0 ? path.substring(0, lastSlash) : '';

            const response = await fetch(contextPath + '/api/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ email, password })
            });

            if (!response.ok) {
                let errorMsg = "Invalid email or password. Please try again.";
                try {
                    const errorData = await response.json();
                    if (errorData && errorData.error) errorMsg = errorData.error;
                    else if (errorData && errorData.message) errorMsg = errorData.message;
                } catch (parseErr) { }
                throw new Error(errorMsg);
            }

            const data = await response.json();

            // Save the JWT token
            localStorage.setItem('telebridge_token', data.token);

            // Redirect to dashboard
            window.location.href = 'dashboard.html';

        } catch (error) {
            showToast(error.message, 'error');
            submitBtn.disabled = false;
            submitBtn.innerHTML = 'Sign In';
        }
    });
});
