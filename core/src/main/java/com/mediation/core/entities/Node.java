package com.mediation.core.entities;

/**
 * Represents a network node (upstream or downstream) in the mediation system.
 * Contains connection details and credentials needed by the transfer clients.
 */
public class Node {

    private int nodeId;
    private String nodeName;
    private String nodeType; // UPSTREAM or DOWNSTREAM
    private String protocol; // FTP, SFTP
    private String ipAddress;
    private int port;
    private String username;
    private String password;
    private boolean isActive;

    public Node() {
    }

    public Node(int nodeId, String nodeName, String nodeType, String protocol,
            String ipAddress, int port, String username, String password,
            boolean isActive) {
        this.nodeId = nodeId;
        this.nodeName = nodeName;
        this.nodeType = nodeType;
        this.protocol = protocol;
        this.ipAddress = ipAddress;
        this.port = port;
        this.username = username;
        this.password = password;
        this.isActive = isActive;
    }

    public int getNodeId() {
        return nodeId;
    }

    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    @Override
    public String toString() {
        return "Node{"
                + "nodeId=" + nodeId
                + ", nodeName='" + nodeName + '\''
                + ", nodeType='" + nodeType + '\''
                + ", protocol='" + protocol + '\''
                + ", isActive=" + isActive
                + '}';
    }
}
