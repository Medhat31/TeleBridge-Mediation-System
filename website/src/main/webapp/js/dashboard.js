document.addEventListener('DOMContentLoaded', () => {
    
    /**
     * Fetches summary statistics from the backend (total nodes, rules, and CDRs)
     * and updates the dashboard cards with live data.
     */
    async function loadStats() {
        try {
            const [nodes, rules, metrics] = await Promise.all([
                apiFetch('/nodes'),
                apiFetch('/rules'),
                apiFetch('/metrics/cdrs-today')
            ]);
            
            const activeNodes = nodes.filter(n => n.active).length;
            const activeRules = rules.filter(r => r.active).length;
            const totalCdrs = metrics.totalCdrs || 0;
            
            document.getElementById('total-nodes').innerText = activeNodes;
            document.getElementById('total-rules').innerText = activeRules;
            document.getElementById('total-cdrs').innerText = totalCdrs.toLocaleString();
        } catch (error) {
            console.error("Failed to load stats:", error);
        }
    }
    
    /**
     * Fetches the 50 most recent system activity logs and renders them
     * into the Activity Feed section.
     */
    async function loadActivities() {
        try {
            const activities = await apiFetch('/activities?limit=50');
            const activityList = document.getElementById('activity-list');
            activityList.innerHTML = ''; // Clear existing items

            if (!activities || activities.length === 0) {
                activityList.innerHTML = '<div class="activity-item"><div class="activity-message">No recent activity logs found.</div></div>';
                return;
            }

            activities.forEach(act => {
                const div = document.createElement('div');
                div.className = 'activity-item';
                
                let iconHtml = '';
                if (act.type === 'success') iconHtml = '<i class="fa-solid fa-check-circle" style="color: var(--success)"></i>';
                else if (act.type === 'info') iconHtml = '<i class="fa-solid fa-info-circle" style="color: var(--primary)"></i>';
                else if (act.type === 'warning') iconHtml = '<i class="fa-solid fa-triangle-exclamation" style="color: var(--warning)"></i>';
                else if (act.type === 'error') iconHtml = '<i class="fa-solid fa-circle-xmark" style="color: var(--danger)"></i>';
                else iconHtml = '<i class="fa-solid fa-info-circle" style="color: var(--text-secondary)"></i>';

                // Format the timestamp (e.g. "10:42 AM")
                const timeStr = new Date(act.logTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

                div.innerHTML = `
                    <div class="activity-time">${timeStr}</div>
                    <div class="activity-icon">${iconHtml}</div>
                    <div class="activity-message">${act.message}</div>
                `;
                
                activityList.appendChild(div);
            });
        } catch (error) {
            console.error("Failed to load activities:", error);
        }
    }

    loadStats();
    loadActivities();

});
