package com.yd.controller;

import com.yd.dao.GroupDAO;
import com.yd.dao.MessageDAO;
import com.yd.dao.UserDAO;
import com.yd.model.Group;
import com.yd.model.Message;
import com.yd.network.ChatClient;
import com.yd.util.DatabaseConnection;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageController {
    @FXML
    private ListView<String> userListView; // 모든 사용자 목록
    @FXML
    private ListView<String> groupListView; // 그룹 목록
    @FXML
    private ListView<String> messageListView; // 메시지 목록
    @FXML
    private TextField messageInputField;

    private ChatClient chatClient;
    private UserDAO userDAO = new UserDAO();
    private MessageDAO messageDAO = new MessageDAO();
    private GroupDAO groupDAO = new GroupDAO();

    private String currentUserId; // 현재 사용자 ID
    private String selectedUserId; // 선택된 사용자 ID
    private Integer selectedGroupId; // 선택된 그룹 ID
    private MainController mainController;

    private ObservableList<String> groupItems = FXCollections.observableArrayList();


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

        // 그룹 목록 로드
        loadGroupList();

        // 사용자 목록 클릭 이벤트 설정
        userListView.setOnMouseClicked(this::handleUserSelection);
        groupListView.setOnMouseClicked(this::handleGroupSelection);
    }

    // 모든 사용자 목록 로드 메서드
    private void loadUserList() {
        List<String> allUsers = userDAO.getAllUserIds(); // 모든 사용자 ID 가져오기
        Platform.runLater(() -> {
            userListView.getItems().clear();
            userListView.getItems().addAll(allUsers);
        });
    }

    // 그룹 목록 로드 메서드
    private void loadGroupList() {
        List<Group> groups = groupDAO.getGroupsByUserId(currentUserId);
        List<String> groupNames = new ArrayList<>();
        for (Group group : groups) {
            groupNames.add("[" + group.getGroupId() + "] " + group.getGroupName());
        }
        Platform.runLater(() -> {
            groupListView.getItems().clear();
            groupListView.getItems().addAll(groupNames);
        });
    }

    // 사용자 선택 이벤트 처리 메서드
    @FXML
    private void handleUserSelection(MouseEvent event) {
        selectedUserId = userListView.getSelectionModel().getSelectedItem();

        if (selectedUserId != null) {
            // 메시지 기록 로드
            loadMessageHistoryForUser(selectedUserId);
        } else {
            System.out.println("사용자가 선택되지 않았습니다.");
        }
    }

    // 그룹 선택 이벤트 처리 메서드
    @FXML
    private void handleGroupSelection(MouseEvent event) {
        selectedUserId = null; // 개인 사용자 선택 해제
        String selectedGroup = groupListView.getSelectionModel().getSelectedItem();
        if (selectedGroup != null) {
            // 그룹 ID 파싱: [groupId] groupName
            int start = selectedGroup.indexOf('[');
            int end = selectedGroup.indexOf(']');
            if (start != -1 && end != -1 && end > start) {
                String groupIdStr = selectedGroup.substring(start + 1, end);
                try {
                    selectedGroupId = Integer.parseInt(groupIdStr);
                    loadMessageHistoryForGroup(selectedGroupId);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid group ID format: " + groupIdStr);
                }
            }
        } else {
            System.out.println("그룹이 선택되지 않았습니다.");
        }
    }

    // 개인 메시지 기록 로드 메서드
    private void loadMessageHistoryForUser(String userId) {
        List<Message> messages = messageDAO.getMessages(currentUserId, userId);
        Platform.runLater(() -> {
            messageListView.getItems().clear();
            for (Message message : messages) {
                String displayMessage = message.toString();
                messageListView.getItems().add(displayMessage);
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


    // 그룹 메시지 기록 로드 메서드
    private void loadMessageHistoryForGroup(int groupId) {
        List<Message> messages = messageDAO.getGroupMessages(groupId);
        Platform.runLater(() -> {
            messageListView.getItems().clear();
            for (Message message : messages) {
                String displayMessage = message.toString();
                messageListView.getItems().add(displayMessage);
            }

            // 메시지 알림 업데이트
            if (mainController != null) {
                mainController.updateMessageNotification();
            } else {
                System.out.println("MainController가 초기화되지 않았습니다.");
            }
        });
    }



    // 메시지 전송 이벤트 처리 메서드 수정 (개인 메시지 및 그룹 메시지 지원)
    @FXML
    private void handleSendMessage() {
        String messageText = messageInputField.getText();

        if (messageText != null && !messageText.isEmpty()) {
            if (selectedUserId != null) {
                // 개인 메시지 전송
                sendPrivateMessage(messageText);
            } else if (selectedGroupId != null) {
                // 그룹 메시지 전송
                sendGroupMessage(selectedGroupId, messageText);
            } else {
                showAlert("경고", "메시지를 보낼 대상을 선택해주세요.");
            }
        } else {
            System.out.println("메시지 전송 실패: 메시지가 비어있습니다.");
        }
    }

    // 개인 메시지 전송 메서드
    private void sendPrivateMessage(String messageText) {
        // 메시지 저장
        Message message = new Message(currentUserId, selectedUserId, messageText);
        messageDAO.saveMessage(message);

        // 서버에 메시지 전송 (형식: receiverId:messageText)
        chatClient.sendMessage(selectedUserId + ":" + messageText);

        messageInputField.clear();
    }

    // 그룹 메시지 전송 메서드
    private void sendGroupMessage(int groupId, String messageText) {
        // 메시지 저장
        Message message = new Message(currentUserId, groupId, messageText);
        messageDAO.saveMessage(message);

        // 서버에 메시지 전송 (형식: GROUP_MESSAGE:groupId:messageText)
        chatClient.sendGroupMessage(groupId, messageText);

        messageInputField.clear();
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

    // 그룹 생성 성공 시 알림을 받는 메서드
    public void notifyGroupCreated(int groupId) {
        Platform.runLater(() -> {
            loadGroupList();
            showAlert("성공", "그룹이 생성되었습니다. 그룹 ID: " + groupId);
        });
    }

    // 그룹 생성 이벤트 처리 메서드
    @FXML
    private void handleCreateGroup() {
        try {
            // FXML 파일의 정확한 경로를 설정합니다.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create_group.fxml"));
            Parent root = loader.load();

            CreateGroupController controller = loader.getController();
            controller.setMessageController(this);
            controller.setCurrentUserId(currentUserId);

            Stage stage = new Stage();
            stage.setTitle("그룹 만들기");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("오류", "그룹 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    // 알림 메시지 표시 메서드
    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.showAndWait();
        });
    }

    // 그룹 생성 창에서 그룹을 생성하고 서버에 전송하는 메서드
    public void createGroup(String groupName, List<String> memberIds) {
        // 데이터베이스에 그룹 생성
        Group group = new Group();
        group.setGroupName(groupName);
        group.setCreatedBy(currentUserId);
        group.setCreatedAt(LocalDateTime.now());
        group.setMemberIds(memberIds);

        int groupId = groupDAO.createGroup(group);
        if (groupId != -1) {
            // 그룹 생성 성공, 서버에 전송
            chatClient.sendGroupCreation(group);
            Platform.runLater(() -> {
                loadGroupList();
                showAlert("성공", "그룹이 성공적으로 생성되었습니다.");
            });
        } else {
            Platform.runLater(() -> {
                showAlert("오류", "그룹 생성에 실패했습니다.");
            });
        }
    }

    // 핸들러 메서드 추가: 개인 대화 보기
    @FXML
    private void handlePrivateChat(ActionEvent event) {
        userListView.setVisible(true);
        groupListView.setVisible(false);
    }

    // 핸들러 메서드 추가: 그룹 대화 보기
    @FXML
    private void handleGroupChat(ActionEvent event) {
        userListView.setVisible(false);
        groupListView.setVisible(true);
    }
}
