package com.mediation.website.entity;

import java.io.Serializable;

/**
 * Represents a network node (upstream provider or downstream consumer).
 * Holds connection and configuration details necessary for container provisioning.
 */
public class Node implements Serializable {
    private int nodeId;
    private String nodeName;
    private String nodeType;
    private String protocol;
    private String ipAddress;
    private int port;
    private String username;
    private String password;
    private boolean isActive;

    public Node() {}

    public Node(int nodeId, String nodeName, String nodeType, String protocol, String ipAddress, int port, String username, String password, boolean isActive) {
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
}
