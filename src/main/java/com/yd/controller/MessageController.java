package com.yd.controller;

import com.yd.dao.MessageDAO;
import com.yd.dao.UserDAO;
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
    private ListView<String> userListView; // 모든 사용자 목록
    @FXML
    private ListView<String> messageListView; // 메시지 목록
    @FXML
    private TextField messageInputField;

    private ChatClient chatClient;
    private UserDAO userDAO = new UserDAO();
    private MessageDAO messageDAO = new MessageDAO();

    private String currentUserId; // 현재 사용자 ID
    private String selectedUserId; // 선택된 사용자 ID
    private MainController mainController;

    // MainController 설정 메서드
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    // ChatClient 초기화 메서드
    public void initializeChatClient(String currentUserId, MainController mainController) {
        this.currentUserId = currentUserId;
        this.mainController = mainController;

        // ChatClient 초기화 및 시작
        chatClient = new ChatClient(currentUserId, this, mainController);
        chatClient.start();

        // 모든 사용자 목록 로드
        loadUserList();

        // 사용자 목록 클릭 이벤트 설정
        userListView.setOnMouseClicked(this::handleUserSelection);
    }

    // 모든 사용자 목록 로드 메서드
    private void loadUserList() {
        List<String> allUsers = userDAO.getAllUserIds(); // 모든 사용자 ID 가져오기
        Platform.runLater(() -> {
            userListView.getItems().clear();
            userListView.getItems().addAll(allUsers);
        });
    }

    // 사용자 선택 이벤트 처리 메서드
    @FXML
    private void handleUserSelection(MouseEvent event) {
        selectedUserId = userListView.getSelectionModel().getSelectedItem();

        if (selectedUserId != null) {
            // 메시지 기록 로드
            loadMessageHistory(selectedUserId);
        } else {
            System.out.println("사용자가 선택되지 않았습니다.");
        }
    }

    // 메시지 기록 로드 메서드
    private void loadMessageHistory(String userId) {
        List<Message> messages = messageDAO.getMessages(currentUserId, userId);
        Platform.runLater(() -> {
            messageListView.getItems().clear();
            for (Message message : messages) {
                String displayMessage = message.getSenderId() + ": " + message.getMessageText() + " (" + message.getTimestamp() + ")";
                messageListView.getItems().add(displayMessage);
            }
            // 메시지를 읽음으로 표시
            messageDAO.markMessagesAsRead(currentUserId, userId);

            // 메시지 알림 업데이트
            if (mainController != null) {
                mainController.updateMessageNotification();
            } else {
                System.out.println("MainController가 초기화되지 않았습니다.");
            }
        });
    }

    // 메시지 전송 이벤트 처리 메서드
    @FXML
    private void handleSendMessage() {
        String messageText = messageInputField.getText();

        if (messageText != null && !messageText.isEmpty() && selectedUserId != null) {
            // 메시지 저장
            Message message = new Message(currentUserId, selectedUserId, messageText);
            messageDAO.saveMessage(message);

            // 메시지 목록 업데이트
            Platform.runLater(() -> {
                String displayMessage = currentUserId + ": " + messageText;
                messageListView.getItems().add(displayMessage);
                // 자동 스크롤
                messageListView.scrollTo(messageListView.getItems().size() - 1);
            });

            messageInputField.clear();

            // 서버에 메시지 전송 (형식: receiverId:messageText)
            chatClient.sendMessage(selectedUserId + ":" + messageText);
        } else {
            System.out.println("메시지 전송 실패: 대상 사용자를 선택하지 않았거나 메시지가 비어있습니다.");
        }
    }

    // 서버로부터 메시지를 수신했을 때 UI에 추가하는 메서드
    public void addMessageToView(String message) {
        Platform.runLater(() -> {
            if (messageListView != null) {
                messageListView.getItems().add(message);
                // 자동 스크롤
                messageListView.scrollTo(messageListView.getItems().size() - 1);
            } else {
                System.out.println("messageListView가 초기화되지 않았습니다.");
            }
        });
    }

    // ChatClient 설정 메서드
    public void setChatClient(ChatClient chatClient) {
        this.chatClient = chatClient;
    }
}
