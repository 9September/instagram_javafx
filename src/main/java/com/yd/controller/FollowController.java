package com.yd.controller;

import com.yd.dao.FollowDAO;
import com.yd.dao.UserDAO;
import com.yd.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.io.ByteArrayInputStream;
import java.util.List;

public class FollowController {

    @FXML
    private ListView<User> followListView;
    @FXML
    private Label headerLabel;
    @FXML
    private TextField searchField;
    @FXML
    private Button searchButton;

    private User currentUser;
    private FollowDAO followDAO = new FollowDAO();
    private UserDAO userDAO = new UserDAO();
    private String mode; // "followers" 또는 "following"

    public void setUser(User user, String mode) {
        this.currentUser = user;
        this.mode = mode;

        if ("followers".equals(mode)) {
            headerLabel.setText("팔로워");
        } else if ("following".equals(mode)) {
            headerLabel.setText("팔로잉");
        }
        loadFollowList();
    }

    private void loadFollowList() {
        List<User> users;
        if ("followers".equals(mode)) {
            users = followDAO.getFollowers(currentUser.getId());
        } else {
            users = followDAO.getFollowingUsers(currentUser.getId());
        }
        ObservableList<User> userList = FXCollections.observableArrayList(users);
        followListView.setItems(userList);

        setupFollowListView();
    }

    private void setupFollowListView() {
        followListView.setCellFactory(param -> new ListCell<>() {
            private HBox content;
            private ImageView profileImageView;
            private Label nameLabel;

            {
                content = new HBox(10);
                profileImageView = new ImageView();
                profileImageView.setFitWidth(40);
                profileImageView.setFitHeight(40);
                profileImageView.setPreserveRatio(true);

                nameLabel = new Label();
                nameLabel.getStyleClass().add("user-name-label");

                content.getChildren().addAll(profileImageView, nameLabel);
            }

            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Image profileImage = getImageFromBytes(user.getProfileImage());
                    profileImageView.setImage(profileImage != null ? profileImage : getDefaultProfileImage());
                    nameLabel.setText(user.getId());

                    setGraphic(content);

                    // Hover 시 스타일 적용
                    content.setOnMouseEntered(e -> content.setStyle("-fx-background-color: #f0f0f0;"));
                    content.setOnMouseExited(e -> content.setStyle("-fx-background-color: transparent;"));
                }
            }
        });
    }

    private Image getImageFromBytes(byte[] imageBytes) {
        if (imageBytes != null && imageBytes.length > 0) {
            return new Image(new ByteArrayInputStream(imageBytes));
        } else {
            // 기본 이미지 로드
            return new Image(getClass().getResourceAsStream("/images/default_profile.jpg"));
        }
    }

    private Image getDefaultProfileImage() {
        return new Image(getClass().getResourceAsStream("/images/default_profile.jpg"));
    }

    @FXML
    private void handleSearch() {
        String query = searchField.getText().trim().toLowerCase();
        if (query.isEmpty()) {
            loadFollowList();
            return;
        }

        List<User> allFollows;
        if ("followers".equals(mode)) {
            allFollows = userDAO.getFollowers(currentUser.getId());
        } else {
            allFollows = userDAO.getFollowing(currentUser.getId());
        }

        // 검색어에 맞는 사용자 필터링
        List<User> filteredList = allFollows.stream()
                .filter(user -> user.getId().toLowerCase().contains(query))
                .toList();

        ObservableList<User> observableList = FXCollections.observableArrayList(filteredList);
        followListView.setItems(observableList);
    }
}