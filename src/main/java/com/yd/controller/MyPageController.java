package com.yd.controller;

import com.yd.dao.CommentDAO;
import com.yd.dao.LikeDAO;
import com.yd.dao.PostDAO;
import com.yd.dao.UserDAO;
import com.yd.model.Comment;
import com.yd.model.Post;
import com.yd.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.TilePane;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class MyPageController {

    @FXML
    private Label birthdayLabel;
    @FXML
    private Label messageLabel;
    @FXML
    private DatePicker birthdayPicker;
    @FXML
    private Button backButton;
    @FXML
    private ImageView logoImageView;
    @FXML
    private Label headerLabel;
    @FXML
    private ImageView profileImageView;
    @FXML
    private ImageView twitterImage;
    @FXML
    private Label usernameLabel;
    @FXML
    private Label emailLabel;

    @FXML
    private Button uploadProfileImageButton;
    @FXML
    private TabPane myTabPane;
    @FXML
    private ListView<Post> myPostsListView;
    @FXML
    private ListView<Comment> myCommentsListView;
    @FXML
    private ListView<Post> likedPostsListView;
    @FXML
    private BorderPane rootPane;
    @FXML
    private Label postCountLabel;
    @FXML
    private Label followerCountLabel;
    @FXML
    private Label followingCountLabel;
    @FXML
    private TabPane infoTabPane;
    @FXML
    private TilePane postTilePane;
    @FXML
    private TilePane reelsTilePane;
    @FXML
    private TilePane likesTilePane;

    private UserDAO userDAO = new UserDAO();
    private PostDAO postDAO = new PostDAO();
    private CommentDAO commentDAO = new CommentDAO();
    private LikeDAO likeDAO = new LikeDAO();
    private User currentUser;
    private byte[] newProfileImageBytes = null;

    @FXML
    private BorderPane mypageBorderPane;
    @FXML
    void goToHome(ActionEvent event) {
        loadUI("main.fxml");
    }

    @FXML
    void goToSearch(ActionEvent event) {
        loadUI("Search.fxml");
    }

    @FXML
    void goToExplore(ActionEvent event) {
        loadUI("Explore.fxml");
    }

    @FXML
    void goToReels(ActionEvent event) {
        loadUI("Reels.fxml");
    }

    @FXML
    void goToMessages(ActionEvent event) {
        loadUI("Messages.fxml");
    }

    @FXML
    void goToNotifications(ActionEvent event) {
        loadUI("Notifications.fxml");
    }

    @FXML
    void goToPosts(ActionEvent event) {
        loadUI("Posts.fxml");
    }

    @FXML
    void goToDashboard(ActionEvent event) {
        loadUI("Dashboard.fxml");
    }

    @FXML
    void goToProfile(ActionEvent event) {
        loadUI("Profile.fxml");
    }

    private void loadUI(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxml));
            Node node = loader.load();
            mypageBorderPane.setCenter(node);
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, fxml + "을(를) 로드하는 중 오류가 발생했습니다.", ButtonType.OK);
            alert.showAndWait();
        }
    }


    @FXML
    public void initialize() {
        currentUser = LoginController.getCurrentUser();
        if (currentUser != null) {
            usernameLabel.setText(currentUser.getId());
            emailLabel.setText(currentUser.getEmail());
            birthdayLabel.setText(currentUser.getBirthday() != null ? currentUser.getBirthday().toString() : "");

            Image profileImage = getImageFromBytes(currentUser.getProfileImage());
            profileImageView.setImage(profileImage);

            setCircularProfileImage();
            loadUserStatistics();

        } else {
            System.out.println("currentUser is null");
        }


        loadUserInfo();

        infoTabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.getText().equals("게시물")) {
                loadMyPosts();
            } else if (newValue.getText().equals("릴스")) {
                loadMyReels();
            } else if (newValue.getText().equals("좋아요")) {
                loadLikedPosts();
            }
        });
    }

    private void setCircularProfileImage() {
        double radius = profileImageView.getFitWidth() / 2;
        Circle clip = new Circle(radius, radius, radius);
        profileImageView.setClip(clip);
    }

    public void setUser(User user) {
        this.currentUser = user;
        usernameLabel.setText("@" + user.getId());

        // 프로필 이미지를 데이터베이스에서 불러와 설정
        Image profileImage = getImageFromBytes(user.getProfileImage());
        profileImageView.setImage(profileImage);

        loadUserInfo();
        loadMyPosts();
        loadMyComments();
        loadLikedPosts();
        loadUserStatistics();
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

    // 사용자 정보 로드
    private void loadUserInfo() {
        emailLabel.setText(currentUser.getEmail());
        birthdayLabel.setText(currentUser.getBirthday() != null ? currentUser.getBirthday().toString() : "");
    }

    private void loadUserStatistics() {
        // 게시글 수
        int postCount = postDAO.getPostCountByUserId(currentUser.getId());
        postCountLabel.setText(String.valueOf(postCount));

        // 팔로워 수
        int followerCount = userDAO.getFollowerCount(currentUser.getId());
        followerCountLabel.setText(String.valueOf(followerCount));

        // 팔로잉 수
        int followingCount = userDAO.getFollowingCount(currentUser.getId());
        followingCountLabel.setText(String.valueOf(followingCount));
    }

    @FXML
    private void handleFollowerCountClick(MouseEvent event) {
        // 팔로워 목록 표시 (배경 블러 처리 및 팝업 형태)
        showFollowerListPopup();
    }

    @FXML
    private void handleFollowingCountClick(MouseEvent event) {
        // 팔로우 목록 표시 (배경 블러 처리 및 팝업 형태)
        showFollowingListPopup();
    }

    @FXML
    private void handleProfileImageUpload(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("프로필 사진 업로드");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("이미지 파일", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(profileImageView.getScene().getWindow());

        if (selectedFile != null) {
            // 파일 크기 제한 (예: 20MB)
            if (selectedFile.length() > 20 * 1024 * 1024) { // 2MB
                Alert alert = new Alert(Alert.AlertType.WARNING, "이미지 파일 크기가 너무 큽니다. 2MB 이하의 파일을 선택해주세요.", ButtonType.OK);
                alert.showAndWait();
                return;
            }

            try {
                byte[] imageBytes = Files.readAllBytes(selectedFile.toPath());

                // 데이터베이스에 프로필 이미지 업데이트
                boolean success = userDAO.updateUserProfileImage(currentUser.getId(), imageBytes);
                if (success) {
                    // 현재 사용자 객체 업데이트
                    currentUser.setProfileImage(imageBytes);

                    // ImageView 업데이트
                    Image profileImage = getImageFromBytes(imageBytes);
                    profileImageView.setImage(profileImage);

                    // 메인 페이지의 프로필 이미지도 업데이트하려면, 메인 페이지 컨트롤러에 접근하거나 창을 다시 로드해야 합니다.

                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "프로필 사진이 업데이트되었습니다.", ButtonType.OK);
                    alert.showAndWait();
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "프로필 사진을 업데이트하지 못했습니다.", ButtonType.OK);
                    alert.showAndWait();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "이미지 파일을 읽는 중 오류가 발생했습니다.", ButtonType.OK);
                alert.showAndWait();
            }
        }
    }

    @FXML
    void handleEditProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/editprofile.fxml"));
            Parent root = loader.load();

            root.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());

            // EditProfileController에 현재 사용자 정보 전달
            EditProfileController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("프로필 편집");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setWidth(300);
            stage.setHeight(400);
            stage.showAndWait();

            // 편집 후 업데이트된 정보 로드
            loadUserInfo();
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "프로필 편집 창을 여는 중 오류가 발생했습니다.", ButtonType.OK);
            alert.showAndWait();
        }
    }

    @FXML
    private void handleProfileImageClick(MouseEvent event) {
        // 프로필 이미지 변경 기능 구현
        handleProfileImageUpload(event);
    }

    // 내가 올린 포스트 로드
    private void loadMyPosts() {
        List<Post> myPosts = postDAO.getPostsByUserId(currentUser.getId());
        postTilePane.getChildren().clear();
        for (Post post : myPosts) {
            ImageView postImageView = new ImageView();
            Image postImage = getImageFromBytes(post.getImage());
            postImageView.setImage(postImage);
            postImageView.setFitWidth(200);
            postImageView.setFitHeight(200);
            postImageView.setPreserveRatio(true);
            // 클릭 이벤트 등 추가 가능
            postTilePane.getChildren().add(postImageView);
        }
    }

    private void loadMyReels() {
        // 릴스 로드 로직 구현
    }

    // 내가 단 댓글 로드
    private void loadMyComments() {
        List<Comment> myComments = commentDAO.getCommentsByUserId(currentUser.getId());
        ObservableList<Comment> myCommentsList = FXCollections.observableArrayList(myComments);
        myCommentsListView.setItems(myCommentsList);
    }

    // 내가 좋아요 한 포스트 로드
    private void loadLikedPosts() {
        List<Post> likedPosts = likeDAO.getLikedPostsByUserId(currentUser.getId());
        likesTilePane.getChildren().clear();
        for (Post post : likedPosts) {
            ImageView postImageView = new ImageView();
            Image postImage = getImageFromBytes(post.getImage());
            postImageView.setImage(postImage);
            postImageView.setFitWidth(200);
            postImageView.setFitHeight(200);
            postImageView.setPreserveRatio(true);
            // 클릭 이벤트 등 추가 가능
            likesTilePane.getChildren().add(postImageView);
        }
    }

    private void showFollowerListPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/followlist.fxml"));
            Parent root = loader.load();

            // FollowController에 현재 사용자 정보와 모드 전달
            FollowController controller = loader.getController();
            controller.setUser(currentUser, "followers");

            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("팔로워 목록");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            loadUserStatistics();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showFollowingListPopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/followlist.fxml"));
            Parent root = loader.load();

            // FollowController에 현재 사용자 정보와 모드 전달
            FollowController controller = loader.getController();
            controller.setUser(currentUser, "following");

            Scene scene = new Scene(root);
            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("팔로우 목록");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            loadUserStatistics();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}