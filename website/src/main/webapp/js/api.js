/**
 * Core API wrapper for communicating with the backend.
 * Automatically detects the environment and handles JWT session tokens.
 */
const _contextPath = (() => {
    const path = window.location.pathname;
    const lastSlash = path.lastIndexOf('/');
    const base = lastSlash > 0 ? path.substring(0, lastSlash) : '';
    return base;
})();
const API_BASE_URL = _contextPath + '/api';

/**
 * Checks if a user session token exists in local storage.
 * If not found, and the user isn't already on the login page, redirects them to log in.
 */
function checkAuth() {
    const token = localStorage.getItem('telebridge_token');
    if (!token && !window.location.pathname.endsWith('login.html') && !window.location.pathname.endsWith('index.html')) {
        window.location.href = 'login.html';
    }
}

/**
 * Performs an asynchronous HTTP request to the backend API.
 * Automatically injects the authorization token into the headers.
 * 
 * @param {string} endpoint - The API endpoint to call (e.g., '/nodes').
 * @param {object} options - Standard fetch options (method, headers, body, etc.).
 * @returns {Promise<any>} The JSON data returned from the server.
 * @throws {Error} If the server returns an error or the session expires.
 */
async function apiFetch(endpoint, options = {}) {
    const token = localStorage.getItem('telebridge_token');

    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        headers
    });

    if (response.status === 401) {
        // Token expired or invalid
        localStorage.removeItem('telebridge_token');
        window.location.href = 'login.html?error=session_expired';
        throw new Error("Session expired. Please log in again.");
    }

    // Attempt to parse JSON response if there is content
    let data = null;
    if (response.status !== 204) {
        try {
            data = await response.json();
        } catch (e) {
            // Not a JSON response
        }
    }

    if (!response.ok) {
        const errorMsg = data && data.message ? data.message : `API Error: ${response.status} ${response.statusText}`;
        throw new Error(errorMsg);
    }

    return data;
}

/**
 * Logs the current user out by clearing the session token from local storage
 * and redirecting back to the login page.
 */
function logout() {
    // Optionally call logout API here if needed
    apiFetch('/auth/logout', { method: 'POST' }).catch(() => { });
    localStorage.removeItem('telebridge_token');
    window.location.href = 'login.html';
}

// Check auth globally on script load
checkAuth();
