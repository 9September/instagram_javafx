package com.yd.controller;

import com.yd.dao.NotificationDAO;
import com.yd.model.Notification;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ListView;

import java.util.List;

public class NotificationController {

    @FXML
    private ListView<String> notificationListView;

    private NotificationDAO notificationDAO = new NotificationDAO();
    private String id; //가독성때문에 currentUserID->id

    public void initializeNotifications(String id) { // userId -> id
        this.id = id;  // userId -> id currentUserID->id
        loadNotifications();
    }

    private void loadNotifications() {
        List<Notification> notifications = notificationDAO.getUserNotifications(id); //currentUserID->id
        ObservableList<String> notificationItems = FXCollections.observableArrayList();
        for (Notification notification : notifications) {
            String content = notification.getContent() + (notification.isRead() ? "" : " (새 알림)");
            notificationItems.add(content);
        }
        notificationListView.setItems(notificationItems);
    }

    @FXML
    private void markNotificationAsRead() {
        int selectedIndex = notificationListView.getSelectionModel().getSelectedIndex();
        if (selectedIndex >= 0) {
            // 실제 알림 데이터의 ID 가져오기
            List<Notification> notifications = notificationDAO.getUserNotifications(id); //currentUserID->id
            Notification selectedNotification = notifications.get(selectedIndex);
            boolean success = notificationDAO.markAsRead(selectedNotification.getNotificationId());
            if (success) {
                Alert alert = new Alert(AlertType.INFORMATION, "알림이 읽음 처리되었습니다.");
                alert.showAndWait();
                loadNotifications(); // 알림 목록 다시 로드
            }
        }
    }
}
