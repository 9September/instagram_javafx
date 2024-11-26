package com.yd.model;

import java.time.LocalDateTime;

public class Message {
    private int messageId;
    private String senderId;
    private String receiverId;
    private String messageText;
    private LocalDateTime timestamp;

    // Constructors
    public Message(String senderId, String receiverId, String messageText) {
        this.senderId = senderId;
        this.receiverId = receiverId;
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

    @Override
    public String toString() {
        return senderId + ": " + messageText + " (" + timestamp + ")";
    }
}
