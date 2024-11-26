package com.yd.controller;

import com.yd.dao.FollowDAO;
import com.yd.dao.MessageDAO;
import com.yd.model.Message;
import com.yd.network.ChatClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;

import java.util.List;

public class MessageController {
    @FXML
    private ListView<String> followingListView; // 팔로잉 목록
    @FXML
    private ListView<String> messageListView; // 메시지 목록
    @FXML
    private TextField messageInputField;

    private ChatClient chatClient;
    private FollowDAO followDAO = new FollowDAO();
    private MessageDAO messageDAO = new MessageDAO();

    private String currentUserId; // 현재 사용자 ID
    private String selectedUserId; // 선택된 사용자 ID
    private MainController mainController; 
    
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
    
    public void initializeChatClient(String currentUserId) {
        this.currentUserId = currentUserId;

        // ChatClient 초기화
        chatClient = new ChatClient(currentUserId, this);
        chatClient.start();

        // 팔로잉 목록 로드
        loadFollowingList();
        
        // 팔로잉 목록의 클릭 이벤트 설정
        followingListView.setOnMouseClicked(this::handleUserSelection);
    }

    private void loadFollowingList() {
        List<String> followingList = followDAO.getFollowingUserIds(currentUserId);
        Platform.runLater(() -> {
            followingListView.getItems().clear();
            followingListView.getItems().addAll(followingList);
        });
    }
    
    @FXML
    private void handleUserSelection(MouseEvent event) {
        selectedUserId = followingListView.getSelectionModel().getSelectedItem();

        if (selectedUserId != null) {
            // 메시지 기록 로드
            loadMessageHistory(selectedUserId);
        } else {
            System.out.println("사용자가 선택되지 않았습니다.");
        }
    }

    private void loadMessageHistory(String userId) {
        List<Message> messages = messageDAO.getMessages(currentUserId, userId);
        Platform.runLater(() -> {
            messageListView.getItems().clear();
            for (Message message : messages) {
                messageListView.getItems().add(message.getSenderId() + ": " + message.getMessageText() + " (" + message.getTimestamp() + ")");
            }
            // 메시지를 읽음으로 표시
            messageDAO.markMessagesAsRead(userId, currentUserId);

            // 메시지 알림 업데이트
            if (mainController != null) {
                mainController.updateMessageNotification();
            } else {
                System.out.println("MainController가 초기화되지 않았습니다.");
            }
        });
    }



    @FXML
    private void handleSendMessage() {
        String messageText = messageInputField.getText();

        if (messageText != null && !messageText.isEmpty() && selectedUserId != null) {
            // 메시지 저장
            Message message = new Message(currentUserId, selectedUserId, messageText);
            messageDAO.saveMessage(message);

            // 메시지 목록 업데이트
            Platform.runLater(() -> messageListView.getItems().add(currentUserId + ": " + messageText));

            messageInputField.clear();
        } else {
            System.out.println("메시지 전송 실패: 대상 사용자를 선택하지 않았습니다.");
        }
    }

    public void addMessageToView(String message) {
        Platform.runLater(() -> {
            if (messageListView != null) {
                messageListView.getItems().add(message);
            } else {
                System.out.println("messageListView가 초기화되지 않았습니다.");
            }
        });
    }
    public void setChatClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }
}
