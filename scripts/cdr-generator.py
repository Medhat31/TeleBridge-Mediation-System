"""
TeleBridge CDR Generator Simulator

This script continuously simulates upstream network elements (MSCs and SMSCs)
by generating Call Detail Records (CDRs) encoded in a custom Hex TLV (Tag-Length-Value) format.
It queries the PostgreSQL database for active upstream nodes and writes realistic CDR files 
into their respective volume directories, which are then picked up by the Mediation Engine.
"""
import os
import time
import random
from datetime import datetime
import psycopg2

# ==========================================
# 1. PATH CONFIGURATION
# ==========================================
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))

# In Docker: /app/node_volumes (VOLUMES_DIR env var or mounted volume)
# Locally:   ../node_volumes (relative to scripts/)
BASE_VOLUMES_DIR = os.environ.get("VOLUMES_DIR", os.path.join(SCRIPT_DIR, "node_volumes"))
if not os.path.exists(BASE_VOLUMES_DIR):
    # Fallback for local execution
    REPO_ROOT = os.path.dirname(SCRIPT_DIR)
    BASE_VOLUMES_DIR = os.path.join(REPO_ROOT, "node_volumes")

# ==========================================
# 2. DATABASE CONFIGURATION
# ==========================================
def load_db_config():
    """
    Loads database configuration settings.
    
    This function attempts to resolve database credentials in the following order:
    1. Environment Variables (injected by docker-compose, e.g., DB_HOST, DB_PORT).
    2. Fallback to parsing the Java application's `db.properties` file if running locally
       outside of Docker.
       
    Returns:
        dict: A dictionary containing 'host', 'port', 'dbname', 'user', and 'password',
              or None if the configuration could not be resolved.
    """
    # Check environment variables first (set by docker-compose)
    env_host = os.environ.get("DB_HOST")
    if env_host:
        return {
            "host": env_host,
            "port": os.environ.get("DB_PORT", "5432"),
            "dbname": os.environ.get("DB_NAME", "mediation_db"),
            "user": os.environ.get("DB_USER", "telecom_user"),
            "password": os.environ.get("DB_PASSWORD", "1234")
        }

    # Fallback: parse db.properties
    repo_root = os.path.dirname(SCRIPT_DIR)
    properties_path = os.path.join(repo_root, "core", "src", "main", "resources", "db.properties")
    
    if not os.path.exists(properties_path):
        print(f"[CRITICAL] Could not find db.properties at {properties_path}")
        return None

    props = {}
    try:
        with open(properties_path, 'r') as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith('#') and '=' in line:
                    key, value = line.split('=', 1)
                    props[key.strip()] = value.strip()
    except Exception as e:
        print(f"[CRITICAL] Error reading db.properties: {e}")
        return None

    jdbc_url = props.get('db.url', '')

    try:
        clean_url = jdbc_url.replace('jdbc:postgresql://', '')
        host_port, dbname = clean_url.split('/', 1)
        
        if ':' in host_port:
            host, port = host_port.split(':', 1)
        else:
            host = host_port
            port = "5432"
            
        return {
            "host": host,
            "port": port,
            "dbname": dbname,
            "user": props.get('db.user', ''),
            "password": props.get('db.password', '')
        }
    except Exception as e:
        print(f"[CRITICAL] Failed to parse JDBC URL: {jdbc_url}. Error: {e}")
        return None

# ==========================================
# 3. HEX TLV HELPER FUNCTIONS
# ==========================================
def int_to_tlv(tag, value, num_bytes):
    """
    Encodes an integer into a Hex TLV (Tag-Length-Value) string format.
    
    Args:
        tag (str): A 2-character hex string representing the tag (e.g., '01').
        value (int): The integer value to encode.
        num_bytes (int): The fixed size in bytes the value should occupy.
        
    Returns:
        str: The encoded Hex TLV string (e.g., '01040000000A').
    """
    val_hex = format(value, f'0{num_bytes*2}x').upper()
    len_hex = format(num_bytes, '02x').upper()
    return f"{tag}{len_hex}{val_hex}"

def str_to_tlv(tag, text):
    """
    Encodes a standard UTF-8 string into an ASCII Hex TLV string format.
    
    Args:
        tag (str): A 2-character hex string representing the tag (e.g., '03').
        text (str): The plaintext string to encode.
        
    Returns:
        str: The encoded Hex TLV string.
    """
    val_hex = text.encode('utf-8').hex().upper()
    num_bytes = len(val_hex) // 2
    len_hex = format(num_bytes, '02x').upper()
    return f"{tag}{len_hex}{val_hex}"

# ==========================================
# 4. CDR GENERATION LOGIC (WITH TEST CASES)
# ==========================================
def generate_hex_tlv_cdr(record_id, node_name):
    """
    Generates a single CDR encoded in Hex TLV format.
    
    It applies business rules depending on whether the source node is an MSC (Voice)
    or an SMSC (Text). It also randomly injects mediation edge cases such as
    zero-duration calls and emergency numbers to test the Mediation Engine's filtration logic.
    
    Args:
        record_id (int): The sequential ID of the record within the file.
        node_name (str): The name of the upstream node generating the CDR.
        
    Returns:
        str: A fully constructed master TLV hex string representing the CDR.
    """
    
    master_tag = "A0"
    
    # Check if this node is an SMSC based on its name
    is_smsc = "smsc" in node_name.lower()

    # 01: recordId
    tlv_id = int_to_tlv("01", record_id, 4)
    
    # 02: recordType (0 for Voice MSC, 1 for SMSC)
    rec_type = 1 if is_smsc else 0
    tlv_type = int_to_tlv("02", rec_type, 1)

    # --- BASELINE NORMAL DATA ---
    dial_a = f"2010{random.randint(0, 9999999):07d}"
    dial_b = f"2011{random.randint(0, 9999999):07d}"
    direction = random.choice([0, 1]) # 0=MO, 1=MT
    
    # Quantity logic based on node type
    if is_smsc:
        quantity = 1 # 1 SMS message
    else:
        quantity = random.randint(1, 3600) # Voice duration in seconds

    # --- INJECT MEDIATION TEST SCENARIOS ---
    scenario_trigger = random.random()

    if scenario_trigger < 0.10:
        # SCENARIO 1: Emergency Call/Text (10% chance)
        dial_b = random.choice(["112", "122", "123", "180"])
    
    elif scenario_trigger < 0.20 and not is_smsc:
        # SCENARIO 2: Zero Duration Voice Call (10% chance)
        quantity = 0

    # 03 & 04: Apply the numbers
    tlv_dial_a = str_to_tlv("03", dial_a)
    tlv_dial_b = str_to_tlv("04", dial_b)

    # 05: answerTime
    timestamp = datetime.now().strftime("%Y%m%d%H%M%S")
    tlv_time = str_to_tlv("05", timestamp)

    # 06: quantity (Seconds or SMS Count)
    tlv_qty = int_to_tlv("06", quantity, 4)
    
    # 07: causeForTerm (16 = Normal Release)
    tlv_cause = int_to_tlv("07", 16, 1)

    # 08: callDirection
    tlv_direction = int_to_tlv("08", direction, 1)

    # Assemble the inner payload
    payload = tlv_id + tlv_type + tlv_dial_a + tlv_dial_b + tlv_time + tlv_qty + tlv_cause + tlv_direction

    # Wrap inside Master Tag with total length
    payload_bytes = len(payload) // 2
    master_len = format(payload_bytes, '02x').upper()

    return f"{master_tag}{master_len}{payload}"

# ==========================================
# 5. DATABASE FETCH LOGIC
# ==========================================
def get_upstream_nodes(db_config):
    """
    Queries the PostgreSQL database to retrieve all active UPSTREAM network nodes.
    
    Args:
        db_config (dict): The database connection parameters.
        
    Returns:
        list: A list of dictionaries containing node 'id', 'name', and 'type',
              or an empty list if the connection fails or no nodes are found.
    """
    nodes = []
    try:
        conn = psycopg2.connect(
            host=db_config["host"], 
            port=db_config["port"], 
            dbname=db_config["dbname"], 
            user=db_config["user"], 
            password=db_config["password"]
        )
        cur = conn.cursor()
        
        # STRICT RULE: Only get active nodes where node_type is 'UPSTREAM'
        query = """
            SELECT node_id, node_name, node_type 
            FROM Nodes 
            WHERE node_type = 'UPSTREAM' AND is_active = TRUE;
        """
        cur.execute(query)
        records = cur.fetchall()
        
        for record in records:
            nodes.append({
                "id": record[0],
                "name": record[1],
                "type": record[2]
            })
            
        cur.close()
        conn.close()
    except Exception as e:
        print(f"[CRITICAL] Database connection failed: {e}")
        
    return nodes

# ==========================================
# 6. MAIN CONTINUOUS LOOP
# ==========================================
def run_generator():
    """
    Main continuous execution loop for the CDR Generator.
    
    Workflow:
    1. Connects to the database to discover active UPSTREAM nodes.
    2. For each node, generates a batch of 50-100 random CDRs.
    3. Writes the batch to a timestamped file within the node's specific volume directory.
    4. Applies proper file permissions so the FTP server containers can read/delete them.
    5. Sleeps for 15 seconds before beginning the next generation cycle.
    """
    print("=== Starting TeleBridge Continuous CDR Generator ===")
    
    db_config = load_db_config()
    if not db_config:
        print("[CRITICAL] Cannot start generator without database credentials. Exiting.")
        return

    print(f"[SYSTEM] Loaded database configuration -> {db_config['host']}:{db_config['port']}/{db_config['dbname']} (User: {db_config['user']})")
    
    while True:
        upstream_nodes = get_upstream_nodes(db_config)
        
        if not upstream_nodes:
            print("[INFO] No active UPSTREAM nodes found in database. Retrying in 15s...")
        else:
            for node in upstream_nodes:
                node_id = node["id"]
                node_name = node["name"]
                node_type = node["type"]
                
                # Dynamic Path: .../node_volumes/UPSTREAM/{node_id}
                # Uses node_id (immutable PK) so renaming a node doesn't break the path
                target_dir = os.path.join(BASE_VOLUMES_DIR, node_type, str(node_id))
                os.makedirs(target_dir, exist_ok=True)
                
                # Create the file name (uses node_name for human readability)
                timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                filename = f"{node_name}_{timestamp}"
                filepath = os.path.join(target_dir, filename)
                
                # Generate 50 to 100 records per file
                num_records = random.randint(50, 100)
                
                with open(filepath, "w") as f:
                    for i in range(1, num_records + 1):
                        cdr_record = generate_hex_tlv_cdr(i, node_name)
                        f.write(cdr_record)
                        f.write("\n")
                        
                        # Introduce a 5% chance of duplicating the exact same CDR
                        # This simulates network retries/glitches and helps test the engine's deduplication logic.
                        if random.random() < 0.05:
                            f.write(cdr_record)
                            f.write("\n")
                        
                # Use secure permissions instead of 777 (Wait, we NEED 777 so the ftp client can delete them)
                os.chmod(filepath, 0o777)
                os.chmod(target_dir, 0o777)
                
                node_identifier = "SMSC" if "smsc" in node_name.lower() else "MSC"
                print(f"[SUCCESS] Generated {num_records} {node_identifier} records -> {filepath}")
        
        print("\n[SLEEPING] Generator waiting for 15 seconds...\n")
        time.sleep(15)

if __name__ == "__main__":
    run_generator()
