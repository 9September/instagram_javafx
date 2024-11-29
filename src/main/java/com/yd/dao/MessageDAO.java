package com.yd.dao;

import com.yd.model.Message;
import com.yd.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {
    private static final String INSERT_MESSAGE_SQL =
            "INSERT INTO MESSAGES (SENDER_ID, RECEIVER_ID, MESSAGE_TEXT) VALUES (?, ?, ?)";
    private static final String SELECT_MESSAGES_SQL =
            "SELECT * FROM MESSAGES WHERE SENDER_ID = ? AND RECEIVER_ID = ? ORDER BY TIMESTAMP ASC";

    public void saveMessage(Message message) {
        String query = "INSERT INTO messages (sender_id, receiver_id, message_text, timestamp) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, message.getSenderId());
            stmt.setString(2, message.getReceiverId());
            stmt.setString(3, message.getMessageText());
            stmt.setTimestamp(4, java.sql.Timestamp.valueOf(message.getTimestamp()));

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Message> getMessages(String senderId, String receiverId) {
        List<Message> messages = new ArrayList<>();
        String query = "SELECT * FROM messages WHERE (sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) ORDER BY timestamp";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, senderId);
            stmt.setString(2, receiverId);
            stmt.setString(3, receiverId);
            stmt.setString(4, senderId);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Message message = new Message(
                        rs.getString("SENDER_ID"),
                        rs.getString("RECEIVER_ID"),
                        rs.getString("MESSAGE_TEXT")
                );
                message.setMessageId(rs.getInt("id")); // 수정된 부분
                message.setTimestamp(rs.getTimestamp("timestamp").toLocalDateTime()); // 메시지 전송 시간 설정
                messages.add(message);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return messages;
    }
    public int getUnreadMessagesCount(String receiverId) {
        int unreadCount = 0;
        String query = "SELECT COUNT(*) FROM messages WHERE receiver_id = ? AND is_read = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, receiverId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                unreadCount = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return unreadCount;
    }

    public void markMessagesAsRead(String senderId, String receiverId) {
        String query = "UPDATE messages SET is_read = 1 WHERE sender_id = ? AND receiver_id = ? AND is_read = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, senderId);
            stmt.setString(2, receiverId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void updateMessageStatusToRead(int messageId) {
        String query = "UPDATE MESSAGES SET is_read = 1 WHERE message_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, messageId);
            int rowsUpdated = stmt.executeUpdate();
            System.out.println("Rows updated (message read): " + rowsUpdated);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
