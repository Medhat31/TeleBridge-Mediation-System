-- Table for Administrators (Authentication)
CREATE TABLE Admins (
    admin_id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
);

-- Default Admin Account (Plaintext password as requested)
INSERT INTO Admins (name, email, password)
VALUES ('System Admin', 'admin@telebridge.local', 'admin123');

-- Table for Network Elements (Upstream & Downstream)
CREATE TABLE Nodes (
    node_id SERIAL PRIMARY KEY,
    node_name VARCHAR(100) NOT NULL,
    node_type VARCHAR(20) NOT NULL CHECK (node_type IN ('UPSTREAM', 'DOWNSTREAM')),
    protocol VARCHAR(10) NOT NULL CHECK (protocol IN ('FTP', 'SFTP')),
    ip_address VARCHAR(50) NOT NULL,
    port INT NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE
);

-- Table for Routing and Filtration Rules
CREATE TABLE Mediation_Rules (
    rule_id SERIAL PRIMARY KEY,
    source_node_id INT REFERENCES Nodes(node_id),
    destination_node_id INT REFERENCES Nodes(node_id),
    filter_zero_duration BOOLEAN DEFAULT TRUE,
    filter_emergency BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE
);

-- Example Data Injection
-- Note: The ip_address matches the Docker container names so the Java Engine
-- can resolve them automatically via Docker's internal DNS on the telecom-net network.
-- Container names use node_id (immutable PK) to prevent zombie containers on renames.
INSERT INTO Nodes (node_name, node_type, protocol, ip_address, port, username, password) 
VALUES 
('Cairo_MSC_01', 'UPSTREAM', 'FTP', 'telebridge_upstream_1', 21, 'msc_user', 'pass123'),
('Billing_Sys_Main', 'DOWNSTREAM', 'SFTP', 'telebridge_downstream_2', 22, 'billing_user', 'secure456');

INSERT INTO Mediation_Rules (source_node_id, destination_node_id, filter_zero_duration, filter_emergency)
VALUES (1, 2, TRUE, TRUE);

-- Table for Tracking Processed CDR Metrics
CREATE TABLE Daily_Metrics (
    metric_date DATE PRIMARY KEY,
    total_cdrs_processed INT DEFAULT 0
);

-- Table for Live Engine Activity Logs
CREATE TABLE Activity_Log (
    id SERIAL PRIMARY KEY,
    log_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    message TEXT NOT NULL,
    log_type VARCHAR(20) NOT NULL -- 'success', 'info', 'warning', 'error'
);
