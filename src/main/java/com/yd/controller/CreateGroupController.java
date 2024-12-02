package com.yd.controller;

import com.yd.dao.UserDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;
import java.util.ArrayList;

public class CreateGroupController {
    @FXML
    private TextField groupNameField;
    @FXML
    private ListView<String> usersListView;
    @FXML
    private Button createButton;
    @FXML
    private Button cancelButton;

    private MessageController messageController;
    private String currentUserId;
    private UserDAO userDAO = new UserDAO();

    public void setMessageController(MessageController messageController) {
        this.messageController = messageController;
    }

    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
        loadUsers();
    }

    private void loadUsers() {
        List<String> allUsers = userDAO.getAllUserIds();
        allUsers.remove(currentUserId); // 자신은 제외
        ObservableList<String> items = FXCollections.observableArrayList(allUsers);
        usersListView.setItems(items);
        usersListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    @FXML
    private void handleCreateGroup() {
        String groupName = groupNameField.getText().trim();
        List<String> selectedUsers = usersListView.getSelectionModel().getSelectedItems();

        if (groupName.isEmpty()) {
            showAlert("경고", "그룹 이름을 입력해주세요.");
            return;
        }

        if (selectedUsers.isEmpty()) {
            showAlert("경고", "그룹에 추가할 사용자를 선택해주세요.");
            return;
        }

        // 그룹 생성 요청
        List<String> members = new ArrayList<>(selectedUsers);
        members.add(currentUserId);
        messageController.createGroup(groupName, members);

        // 창 닫기
        Stage stage = (Stage) createButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleCancel() {
        // 창 닫기
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    // 알림 메시지 표시 메서드
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
