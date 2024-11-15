package com.yd.model;

import java.time.LocalDateTime;

public class Reel {
    private int reelId;
    private String videoUrl;
    private String description;
    private String uploaderId;
    private LocalDateTime createdAt;
    private int numOfLikes;
    private int numOfComments;

    // 기본 생성자
    public Reel() {
    }

    // 전체 생성자
    public Reel(int reelId, String videoUrl, String description, String uploaderId,
                LocalDateTime createdAt, int numOfLikes, int numOfComments) {
        this.reelId = reelId;
        this.videoUrl = videoUrl;
        this.description = description;
        this.uploaderId = uploaderId;
        this.createdAt = createdAt;
        this.numOfLikes = numOfLikes;
        this.numOfComments = numOfComments;
    }

    // Getter 및 Setter 메서드

    public int getReelId() {
        return reelId;
    }

    public void setReelId(int reelId) {
        this.reelId = reelId;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(String uploaderId) {
        this.uploaderId = uploaderId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getNumOfLikes() {
        return numOfLikes;
    }

    public void setNumOfLikes(int numOfLikes) {
        this.numOfLikes = numOfLikes;
    }

    public int getNumOfComments() {
        return numOfComments;
    }

    public void setNumOfComments(int numOfComments) {
        this.numOfComments = numOfComments;
    }

    @Override
    public String toString() {
        return "Reel{" +
                "reelId=" + reelId +
                ", videoUrl='" + videoUrl + '\'' +
                ", description='" + description + '\'' +
                ", uploaderId='" + uploaderId + '\'' +
                ", createdAt=" + createdAt +
                ", numOfLikes=" + numOfLikes +
                ", numOfComments=" + numOfComments +
                '}';
    }
}
