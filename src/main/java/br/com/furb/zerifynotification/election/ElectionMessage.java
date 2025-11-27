package br.com.furb.zerifynotification.election;

public class ElectionMessage {

    public enum Type { HEARTBEAT, ELECTION, OK, COORDINATOR }

    private Type type;
    private String fromId;
    private String leaderId;

    public ElectionMessage() {}

    public ElectionMessage(Type type, String fromId) {
        this.type = type;
        this.fromId = fromId;
    }

    public ElectionMessage(Type type, String fromId, String leaderId) {
        this.type = type;
        this.fromId = fromId;
        this.leaderId = leaderId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getFromId() {
        return fromId;
    }

    public void setFromId(String fromId) {
        this.fromId = fromId;
    }

    public String getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }
}

