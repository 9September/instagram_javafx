package com.yd.model;

import java.time.LocalDateTime;
import java.util.List;

public class Group {
    private int groupId;
    private String groupName;
    private String createdBy;
    private LocalDateTime createdAt;
    private List<String> memberIds; // 그룹에 속한 사용자 ID 목록

    // Constructors
    public Group() {}

    public Group(String groupName, String createdBy, List<String> memberIds) {
        this.groupName = groupName;
        this.createdBy = createdBy;
        this.memberIds = memberIds;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }
}
