package com.yd.dao;

import com.yd.model.Message;
import com.yd.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {
    private static final String INSERT_MESSAGE_SQL =
            "INSERT INTO MESSAGES (SENDER_ID, RECEIVER_ID, MESSAGE_TEXT, TIMESTAMP, IS_READ, GROUP_ID) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String SELECT_MESSAGES_SQL =
            "SELECT * FROM MESSAGES WHERE " +
                    "((SENDER_ID = ? AND RECEIVER_ID = ?) OR (SENDER_ID = ? AND RECEIVER_ID = ?)) " +
                    "AND GROUP_ID IS NULL " +
                    "ORDER BY TIMESTAMP ASC";
    private static final String SELECT_GROUP_MESSAGES_SQL =
            "SELECT * FROM MESSAGES WHERE GROUP_ID = ? ORDER BY TIMESTAMP ASC";
    private static final String SELECT_UNREAD_MESSAGES_SQL =
            "SELECT * FROM MESSAGES WHERE RECEIVER_ID = ? AND IS_READ = 0 AND GROUP_ID IS NULL ORDER BY TIMESTAMP ASC";
    private static final String SELECT_UNREAD_GROUP_MESSAGES_SQL =
            "SELECT * FROM MESSAGES WHERE GROUP_ID = ? AND IS_READ = 0 ORDER BY TIMESTAMP ASC";
    private static final String UPDATE_MESSAGE_READ_STATUS_SQL =
            "UPDATE MESSAGES SET IS_READ = 1 WHERE ID = ?";
    private static final String SELECT_UNREAD_MESSAGES_COUNT_SQL =
            "SELECT COUNT(*) AS unread_count FROM MESSAGES WHERE RECEIVER_ID = ? AND IS_READ = 0 AND GROUP_ID IS NULL";

    // 메시지 저장 메서드 (개인 메시지 및 그룹 메시지)
    public void saveMessage(Message message) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_MESSAGE_SQL, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, message.getSenderId());
            stmt.setString(2, message.getReceiverId());
            stmt.setString(3, message.getMessageText());
            stmt.setTimestamp(4, Timestamp.valueOf(message.getTimestamp()));
            stmt.setBoolean(5, false); // 메시지 초기 상태는 읽지 않음
            if (message.getGroupId() != null) {
                stmt.setInt(6, message.getGroupId());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }

            stmt.executeUpdate();

            // 생성된 메시지 ID 설정
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    message.setMessageId(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 개인 메시지 조회 메서드
    public List<Message> getMessages(String userId1, String userId2) {
        List<Message> messages = new ArrayList<>();
        String query = SELECT_MESSAGES_SQL;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, userId1);
            stmt.setString(2, userId2);
            stmt.setString(3, userId2);
            stmt.setString(4, userId1);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Message message = new Message(
                        rs.getString("SENDER_ID"),
                        rs.getString("RECEIVER_ID"),
                        rs.getString("MESSAGE_TEXT")
                );
                message.setMessageId(rs.getInt("ID"));
                message.setTimestamp(rs.getTimestamp("TIMESTAMP").toLocalDateTime());
                message.setGroupId(null); // 개인 메시지
                messages.add(message);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    // 그룹 메시지 조회 메서드
    public List<Message> getGroupMessages(int groupId) {
        List<Message> messages = new ArrayList<>();
        String query = SELECT_GROUP_MESSAGES_SQL;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Message message = new Message(
                        rs.getString("SENDER_ID"),
                        rs.getString("RECEIVER_ID"),
                        rs.getString("MESSAGE_TEXT")
                );
                message.setMessageId(rs.getInt("ID"));
                message.setTimestamp(rs.getTimestamp("TIMESTAMP").toLocalDateTime());
                message.setGroupId(rs.getInt("GROUP_ID"));
                messages.add(message);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }

    // 개인 메시지의 읽지 않은 메시지 조회 메서드
    public List<Message> getUnreadMessages(String receiverId) {
        List<Message> unreadMessages = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_UNREAD_MESSAGES_SQL)) {

            stmt.setString(1, receiverId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Message message = new Message(
                        rs.getString("SENDER_ID"),
                        rs.getString("RECEIVER_ID"),
                        rs.getString("MESSAGE_TEXT")
                );
                message.setMessageId(rs.getInt("ID"));
                message.setTimestamp(rs.getTimestamp("TIMESTAMP").toLocalDateTime());
                message.setGroupId(null); // 개인 메시지
                unreadMessages.add(message);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return unreadMessages;
    }

    // 그룹 메시지의 읽지 않은 메시지 조회 메서드
    public List<Message> getUnreadGroupMessages(int groupId) {
        List<Message> unreadMessages = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_UNREAD_GROUP_MESSAGES_SQL)) {

            stmt.setInt(1, groupId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Message message = new Message(
                        rs.getString("SENDER_ID"),
                        rs.getString("RECEIVER_ID"),
                        rs.getString("MESSAGE_TEXT")
                );
                message.setMessageId(rs.getInt("ID"));
                message.setTimestamp(rs.getTimestamp("TIMESTAMP").toLocalDateTime());
                message.setGroupId(rs.getInt("GROUP_ID"));
                unreadMessages.add(message);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return unreadMessages;
    }

    // 메시지를 읽음 상태로 업데이트하는 메서드
    public void markMessageAsRead(int messageId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_MESSAGE_READ_STATUS_SQL)) {

            stmt.setInt(1, messageId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 수신자가 특정 사용자로부터 받은 모든 메시지를 읽음 상태로 업데이트하는 메서드
    public void markMessagesAsRead(String senderId, String receiverId) {
        String query = "UPDATE MESSAGES SET IS_READ = 1 WHERE SENDER_ID = ? AND RECEIVER_ID = ? AND IS_READ = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, senderId);
            stmt.setString(2, receiverId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 메시지 읽음 상태를 업데이트하는 추가 메서드 (필요시 사용)
    public void updateMessageStatusToRead(int messageId) {
        String query = "UPDATE MESSAGES SET IS_READ = 1 WHERE ID = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, messageId);
            int rowsUpdated = stmt.executeUpdate();
            System.out.println("Rows updated (message read): " + rowsUpdated);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 수신자의 읽지 않은 개인 메시지 수를 반환하는 메서드
    public int getUnreadMessagesCount(String receiverId) {
        int unreadCount = 0;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_UNREAD_MESSAGES_COUNT_SQL)) {

            stmt.setString(1, receiverId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                unreadCount = rs.getInt("unread_count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return unreadCount;
    }
}
