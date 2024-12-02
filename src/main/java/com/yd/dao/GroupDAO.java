package com.yd.dao;

import com.yd.model.Group;
import com.yd.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GroupDAO {

    private static final String INSERT_GROUP_SQL =
            "INSERT INTO `GROUPS` (GROUP_NAME, CREATED_BY, CREATED_AT) VALUES (?, ?, ?)";
    private static final String INSERT_GROUP_MEMBER_SQL =
            "INSERT INTO GROUP_MEMBERS (GROUP_ID, USER_ID) VALUES (?, ?)";
    private static final String SELECT_GROUP_BY_ID_SQL =
            "SELECT * FROM `GROUPS` WHERE GROUP_ID = ?";
    private static final String SELECT_MEMBERS_BY_GROUP_ID_SQL =
            "SELECT USER_ID FROM GROUP_MEMBERS WHERE GROUP_ID = ?";
    private static final String SELECT_GROUPS_BY_USER_ID_SQL =
            "SELECT g.GROUP_ID, g.GROUP_NAME, g.CREATED_BY, g.CREATED_AT " +
                    "FROM `GROUPS` g " +
                    "JOIN `GROUP_MEMBERS` gm ON g.GROUP_ID = gm.GROUP_ID " +
                    "WHERE gm.USER_ID = ?";
    // 그룹 생성 메서드
    public int createGroup(Group group) {
        int groupId = -1;
        Connection conn = null;
        PreparedStatement stmt = null;
        PreparedStatement memberStmt = null;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // 트랜잭션 시작

            stmt = conn.prepareStatement(INSERT_GROUP_SQL, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, group.getGroupName());
            stmt.setString(2, group.getCreatedBy());
            stmt.setTimestamp(3, Timestamp.valueOf(group.getCreatedAt()));

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        groupId = rs.getInt(1);
                    }
                }
            }

            // 그룹 멤버 추가
            if (groupId != -1 && group.getMemberIds() != null && !group.getMemberIds().isEmpty()) {
                memberStmt = conn.prepareStatement(INSERT_GROUP_MEMBER_SQL);
                for (String userId : group.getMemberIds()) {
                    memberStmt.setInt(1, groupId);
                    memberStmt.setString(2, userId);
                    memberStmt.addBatch();
                }
                memberStmt.executeBatch();
            }

            conn.commit(); // 트랜잭션 커밋

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback(); // 트랜잭션 롤백
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }
        } finally {
            // 리소스 정리
            try {
                if (memberStmt != null) memberStmt.close();
                if (stmt != null) stmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true); // 자동 커밋 복구
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return groupId;
    }

    // 그룹 조회 메서드
    public Group getGroupById(int groupId) {
        Group group = null;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_GROUP_BY_ID_SQL)) {

            stmt.setInt(1, groupId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    group = new Group();
                    group.setGroupId(rs.getInt("GROUP_ID"));
                    group.setGroupName(rs.getString("GROUP_NAME"));
                    group.setCreatedBy(rs.getString("CREATED_BY"));
                    group.setCreatedAt(rs.getTimestamp("CREATED_AT").toLocalDateTime());

                    // 멤버 조회
                    List<String> members = new ArrayList<>();
                    try (PreparedStatement memberStmt = conn.prepareStatement(SELECT_MEMBERS_BY_GROUP_ID_SQL)) {
                        memberStmt.setInt(1, groupId);
                        try (ResultSet memberRs = memberStmt.executeQuery()) {
                            while (memberRs.next()) {
                                members.add(memberRs.getString("USER_ID"));
                            }
                        }
                    }
                    group.setMemberIds(members);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return group;
    }

    // 사용자가 속한 그룹 목록 조회 메서드
    public List<Group> getGroupsByUserId(String userId) {
        List<Group> groups = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_GROUPS_BY_USER_ID_SQL)) {

            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Group group = new Group();
                    group.setGroupId(rs.getInt("GROUP_ID"));
                    group.setGroupName(rs.getString("GROUP_NAME"));
                    group.setCreatedBy(rs.getString("CREATED_BY"));
                    group.setCreatedAt(rs.getTimestamp("CREATED_AT").toLocalDateTime());

                    // 멤버 조회
                    List<String> members = new ArrayList<>();
                    try (PreparedStatement memberStmt = conn.prepareStatement(SELECT_MEMBERS_BY_GROUP_ID_SQL)) {
                        memberStmt.setInt(1, group.getGroupId());
                        try (ResultSet memberRs = memberStmt.executeQuery()) {
                            while (memberRs.next()) {
                                members.add(memberRs.getString("USER_ID"));
                            }
                        }
                    }
                    group.setMemberIds(members);

                    groups.add(group);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return groups;
    }


}
