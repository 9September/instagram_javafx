package com.yd.model;

import java.time.LocalDateTime;

public class Message {
    private int messageId;
    private String senderId;
    private String receiverId;
    private String messageText;
    private LocalDateTime timestamp;
    private Integer groupId;

    // Constructors
    public Message(String senderId, String receiverId, String messageText) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.messageText = messageText;
        this.timestamp = LocalDateTime.now();
        this.groupId = null;
    }

    public Message(String senderId, int groupId, String messageText) {
        this.senderId = senderId;
        this.groupId = groupId;
        this.messageText = messageText;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getMessageText() {
        return messageText;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    @Override
    public String toString() {
        if (groupId != null) {
            return senderId + " [그룹 " + groupId + "]: " + messageText + " (" + timestamp + ")";
        } else {
            return senderId + ": " + messageText + " (" + timestamp + ")";
        }
    }
}
