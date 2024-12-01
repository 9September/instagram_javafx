package com.yd.dao;

import com.yd.model.Notification;
import com.yd.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    public List<Notification> getUserNotifications(String id) { // userid->id
        List<Notification> notifications = new ArrayList<>();
        String query = "SELECT * FROM Notifications WHERE id = ? ORDER BY createdAt DESC"; // userid->id

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, id); //// userid->id

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Notification notification = new Notification();
                notification.setNotificationId(rs.getInt("notificationId"));
                notification.setUserId(rs.getString("id")); // userId -> id
                notification.setInitiatorId(rs.getString("initiatorId"));
                notification.setPostId(rs.getObject("POST_ID", Integer.class)); // postId -> POST_ID
                notification.setMessageId(rs.getObject("messageId", Integer.class));
                notification.setType(rs.getString("type"));
                notification.setContent(rs.getString("content"));
                notification.setRead(rs.getBoolean("is_read")); //isRead->is_read
                notification.setCreatedAt(rs.getTimestamp("createdAt"));
                notifications.add(notification);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notifications;
    }

    public boolean markAsRead(int notificationId) {
        String query = "UPDATE Notifications SET isRead = TRUE WHERE notificationId = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, notificationId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean createNotification(Notification notification) {
        String query = "INSERT INTO Notifications (userId, initiatorId, postId, messageId, type, content) " +
                       "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, notification.getUserId());
            stmt.setString(2, notification.getInitiatorId());
            stmt.setObject(3, notification.getPostId());
            stmt.setObject(4, notification.getMessageId());
            stmt.setString(5, notification.getType());
            stmt.setString(6, notification.getContent());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
