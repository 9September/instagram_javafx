package com.yd.model;

import java.sql.Timestamp;

public class Notification {
    private int notificationId;
    private String id;        // 알림을 받을 사용자 ID userID->id
    private String initiatorId;   // 알림을 발생시킨 사용자 ID
    private Integer POST_ID;       // 관련 게시물 ID (null 가능) postId->POST_ID
    private Integer messageId;    // 관련 메시지 ID (null 가능)
    private String type;          // 알림 유형: MESSAGE, POST_LIKE, COMMENT, FOLLOW
    private String content;       // 알림 내용
    private boolean is_read;       // 읽음 여부 isRead->is_read
    private Timestamp createdAt;  // 생성 시간

    // Getters and Setters
    public int getNotificationId() { return notificationId; }
    public void setNotificationId(int notificationId) { this.notificationId = notificationId; }

    public String getUserId() { return id; } //user_id ->id
    public void setUserId(String id) { this.id = id; } //userID->id

    public String getInitiatorId() { return initiatorId; }
    public void setInitiatorId(String initiatorId) { this.initiatorId = initiatorId; }

    public Integer getPostId() { return POST_ID; } //postID->POST_ID
    public void setPostId(Integer POST_ID) { this.POST_ID = POST_ID; } //postID->POST_ID

    public Integer getMessageId() { return messageId; }
    public void setMessageId(Integer messageId) { this.messageId = messageId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public boolean isRead() { return is_read; } // isREad->is_read
    public void setRead(boolean is_read) { this.is_read = is_read; } // isREad->is_read

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
