package com.yd.controller;

import com.yd.dao.FollowDAO;
import com.yd.dao.PostDAO;
import com.yd.dao.UserDAO;
import com.yd.dao.MessageDAO;
import com.yd.network.ChatClient;
import com.yd.network.ChatServer;
import com.yd.model.Post;
import com.yd.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javafx.application.Platform;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class MainController {
    private Map<String, Boolean> userStatusMap = new HashMap<>();
    private ChatClient chatClient;
    private static MainController instance;
    private Stage stage;

    public MainController() {
        instance = this;
    }

    public static MainController getInstance() {
        return instance;
    }

    @FXML
    private MessageController messageController;

    @FXML
    private Button messagesButton;

    @FXML
    private TextArea postTextArea;

    @FXML
    private ListView<Post> postListView;

    @FXML
    private Button attachImageButton;

    @FXML
    private ListView<User> followingListView;

    @FXML
    private ListView<User> recommendListView;

    @FXML
    private ImageView profileImageView;

    @FXML
    private ImageView twitterImage;

    @FXML
    private ImageView attachedImageView;

    private MessageDAO messageDAO;

    private int postOffset = 0;
    private final int postLimit = 20;
    private ObservableList<Post> postItems = FXCollections.observableArrayList();
    private User currentUser;
    private boolean isLoading = false;
    private byte[] attachedImageBytes = null;
    private PostDAO postDAO = new PostDAO();
    private FollowDAO followDAO = new FollowDAO();
    private UserDAO userDAO = new UserDAO(); // 클래스 레벨에서 생성

    @FXML
    private BorderPane mainBorderPane;

    @FXML
    void goToHome(ActionEvent event) {
        rebuildCenterContent();
        loadFollowingList();
    }

    @FXML
    void goToSearch(ActionEvent event) {
        loadUI("search.fxml");
    }

    @FXML
    void goToExplore(ActionEvent event) {
        loadUI("explore.fxml");
    }

    @FXML
    void goToReels(ActionEvent event) {
        loadUI("reels.fxml");
    }

    @FXML
    void goToMessages(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/messages.fxml"));
            Node node = loader.load();

            // MessageController와 연결
            MessageController messageController = loader.getController();
            messageController.initializeChatClient(currentUser.getId(), this);
            messageController.setMainController(this); // MainController 설정

            // mainBorderPane의 중앙에 메시지 화면 로드
            mainBorderPane.setCenter(node);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("오류", "메시지 화면을 로드하는 중 오류가 발생했습니다.");
        }
    }



    @FXML
    void goToNotifications(ActionEvent event) {
        loadUI("notifications.fxml");
    }

    @FXML
    void goToPosts(ActionEvent event) {
        loadUI("postcreation.fxml");
    }

    @FXML
    void goToProfile(ActionEvent event) {
        loadUI("mypage.fxml");
    }

    private void loadUI(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxml));
            Node node = loader.load();

            // 메시지 화면인 경우에만 MessageController 초기화
            if ("messages.fxml".equals(fxml)) {
                // MessageController와 연결
                MessageController controller = loader.getController();
                controller.initializeChatClient(currentUser.getId(), this); // 사용자 ID 전달
            }

            // 화면 중앙에 새로운 노드 설정
            mainBorderPane.setCenter(node);
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, fxml + "을(를) 로드하는 중 오류가 발생했습니다.", ButtonType.OK);
            alert.showAndWait();
        }
    }

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            // 현재 로그인된 사용자 정보 가져오기
            currentUser = LoginController.getCurrentUser();
            if (currentUser == null) {
                goToLogin();  // 로그인 화면으로 전환
                return;
            }

            // 데이터베이스 연결 객체 초기화
            messageDAO = new MessageDAO();

            // 프로필 이미지 초기화
            Image profileImage = getImageFromBytes(currentUser.getProfileImage());
            profileImageView.setImage(profileImage);


            // 다른 초기화 작업 수행
            loadFollowingList();
            setupFollowingListView();
            setupRecommendListView();
            loadRecommendList();
            rebuildCenterContent();
            updateMessageNotification();

            // 창 닫기 이벤트 설정 (앱 종료 시 사용자 오프라인 상태로 변경)
            stage = (Stage) mainBorderPane.getScene().getWindow(); // mainBorderPane을 통해 Stage를 가져옴
            stage.setOnCloseRequest(event -> handleWindowClose());
        });
    }


    private void handleWindowClose() {
        if (currentUser != null) {
            // 사용자를 오프라인 상태로 설정
            userDAO.setUserOnlineStatus(currentUser.getId(), false);
            System.out.println("User " + currentUser.getId() + " is now set to offline.");

            // ChatClient 연결 해제 및 리소스 정리
            if (chatClient != null) {
                chatClient.stopClient(); // 클라이언트 종료
            }
        }
        Platform.exit(); // JavaFX 응용 프로그램 종료
    }


    // 바이트 배열을 Image로 변환하는 유틸리티 메서드
    private Image getImageFromBytes(byte[] imageBytes) {
        try {
            if (imageBytes != null && imageBytes.length > 0) {
                return new Image(new ByteArrayInputStream(imageBytes));
            } else {
                // 기본 이미지 로드
                return getDefaultProfileImage();
            }
        } catch (Exception e) {
            // 예외 발생 시 기본 이미지 로드
            return getDefaultProfileImage();
        }
    }


    private void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Stage stage = (Stage) profileImageView.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));
            stage.setTitle("Twitter - Login");
            stage.setWidth(400);
            stage.setHeight(600);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 포스트 목록 로드
    private void loadPosts() {
        postOffset = 0; // 오프셋 초기화
        postItems.clear(); // 기존 아이템 초기화
        loadMorePosts(); // 초기 포스트 로드
    }

    private void loadMorePosts() {
        if (isLoading) return;
        isLoading = true;

        // 새로운 스레드에서 데이터 로드 (UI 스레드 블로킹 방지)
        Task<List<Post>> loadTask = new Task<>() {
            @Override
            protected List<Post> call() throws Exception {
                return postDAO.getAllPosts(postOffset, postLimit);
            }
        };

        loadTask.setOnSucceeded(event -> {
            List<Post> posts = loadTask.getValue();
            if (!posts.isEmpty()) {
                postItems.addAll(posts);
                postOffset += posts.size();
            }
            isLoading = false;
        });

        loadTask.setOnFailed(event -> {
            isLoading = false;
            Throwable error = loadTask.getException();
            showAlert("오류", "포스트를 로드하는 중 오류가 발생했습니다: " + error.getMessage());
        });

        new Thread(loadTask).start();
    }

    // 포스트 ListView 설정
    private void setupPostListView() {
        postListView.setCellFactory(param -> new ListCell<>() {
            private VBox content = new VBox();
            private HBox header = new HBox();
            private Label userIdLabel = new Label();
            private Label textLabel = new Label();
            private ImageView postImageView = new ImageView();
            private ImageView postProfileImageView = new ImageView();
            private HBox footer = new HBox(10);
            private Button likeButton = new Button();
            private Label likeCountLabel = new Label();
            private Button commentButton = new Button("댓글");
            private Button retweetButton = new Button();
            private Label retweetCountLabel = new Label();

            {
                // 프로필 이미지 설정
                postProfileImageView.setFitWidth(40);
                postProfileImageView.setFitHeight(40);
                postProfileImageView.setPreserveRatio(true);
                postProfileImageView.setStyle("-fx-background-radius: 20; -fx-border-radius: 20;");

                // 사용자 아이디 레이블 설정
                userIdLabel.setStyle("-fx-font-weight: bold;");

                // 텍스트 레이블 설정
                textLabel.setWrapText(true);

                // 포스트 이미지 뷰 설정
                postImageView.setFitWidth(400);
                postImageView.setPreserveRatio(true);
                postImageView.setSmooth(true);
                postImageView.setCache(true);

                // 좋아요 버튼 및 레이블 설정
                footer.getChildren().addAll(likeButton, likeCountLabel, commentButton);
                footer.setSpacing(10);

                // 헤더에 프로필 이미지와 사용자 아이디 추가
                header.getChildren().addAll(postProfileImageView, userIdLabel);
                header.setSpacing(10);

                // 콘텐츠에 헤더, 텍스트, 이미지 추가
                content.getChildren().addAll(header, textLabel, postImageView, footer);
                content.setSpacing(5);
                content.setStyle("-fx-background-color: rgba(44,44,44,0.95); -fx-padding: 15; -fx-spacing: 10; -fx-background-radius: 3;");
            }

            @Override
            protected void updateItem(Post post, boolean empty) {
                super.updateItem(post, empty);
                if (empty || post == null) {
                    setText(null);
                    setGraphic(null);
                } else {

                    byte[] writerImageBytes = userDAO.getUserProfileImage(post.getWriterId());

                    Image profileImage = getImageFromBytes(writerImageBytes);
                    postProfileImageView.setImage(profileImage != null ? profileImage : getDefaultProfileImage());

                    userIdLabel.setText("@" + post.getWriterId());
                    textLabel.setText(post.getText());

                    if (post.getImage() != null && post.getImage().length > 0) {
                        Image postImage = getImageFromBytes(post.getImage());
                        postImageView.setImage(postImage);
                        postImageView.setVisible(true);
                    } else {
                        postImageView.setImage(null);
                        postImageView.setVisible(false);
                    }

                    // 좋아요 상태 및 카운트 업데이트
                    boolean isLiked = postDAO.isPostLiked(post.getPostId(), currentUser.getId());
                    likeButton.setText(isLiked ? "♥" : "♡");
                    likeCountLabel.setText(String.valueOf(post.getNumOfLikes()));

                    // 이벤트 핸들러 설정
                    likeButton.setOnAction(e -> {
                        handleLikeAction(post, likeButton, likeCountLabel);
                    });

                    commentButton.setOnAction(e -> {
                        openCommentWindow(post);
                    });

                    setGraphic(content);

                    // 마지막 아이템 근처에 도달하면 추가 데이터 로드
                    if (getIndex() >= postItems.size() - 5 && !isLoading) {
                        loadMorePosts();
                    }
                }
            }

            private void handleLikeAction(Post post, Button likeButton, Label likeCountLabel) {
                if (postDAO.isPostLiked(post.getPostId(), currentUser.getId())) {
                    boolean success = postDAO.unlikePost(post.getPostId(), currentUser.getId());
                    if (success) {
                        post.setNumOfLikes(post.getNumOfLikes() - 1);
                    }
                } else {
                    boolean success = postDAO.likePost(post.getPostId(), currentUser.getId());
                    if (success) {
                        post.setNumOfLikes(post.getNumOfLikes() + 1);
                    }
                }
                // 좋아요 상태 및 카운트 업데이트
                boolean isLiked = postDAO.isPostLiked(post.getPostId(), currentUser.getId());
                likeButton.setText(isLiked ? "♥" : "♡");
                likeCountLabel.setText(String.valueOf(post.getNumOfLikes()));
            }

            private Image getDefaultProfileImage() {
                return new Image(getClass().getResourceAsStream("/images/default_profile.jpg"));
            }

        });
    }

    private void openCommentWindow(Post post) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/comment.fxml"));
            Parent root = loader.load();

            // PostController에 포스트 전달
            PostController postController = loader.getController();
            postController.setPost(post);

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Comments for Post ID: " + post.getPostId());
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setWidth(600);
            stage.setHeight(400);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("오류", "댓글 창을 여는 중 오류가 발생했습니다.");
        }
    }

    @FXML
    private void handleAttachImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("이미지 첨부");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("이미지 파일", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File selectedFile = fileChooser.showOpenDialog(attachImageButton.getScene().getWindow());
        if (selectedFile != null) {
            // 파일 크기 제한 (예: 5MB)
            if (selectedFile.length() > 5 * 1024 * 1024) { // 5MB
                showAlert("경고", "이미지 파일 크기가 너무 큽니다. 5MB 이하의 파일을 선택해주세요.");
                return;
            }

            try {
                attachedImageBytes = Files.readAllBytes(selectedFile.toPath());
                Image image = new Image(new ByteArrayInputStream(attachedImageBytes));
                attachedImageView.setImage(image); // 이미지 미리보기 설정
                showAlert("성공", "이미지가 첨부되었습니다.");
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("오류", "이미지 첨부 중 오류가 발생했습니다.");
            }
        }
    }

    // 팔로우 목록 로드
    private void loadFollowingList() {
        List<User> followingUsers = followDAO.getFollowingUsers(currentUser.getId());

        for (User user : followingUsers) {
            boolean isOnline = userDAO.isUserOnline(user.getId());
            user.setOnline(isOnline);
        }

        ObservableList<User> items = FXCollections.observableArrayList(followingUsers);
        followingListView.setPlaceholder(new Label("팔로잉 목록이 없습니다."));
        followingListView.setItems(items);

        // 팔로우 목록을 로드할 때마다 셀 설정 적용
        setupFollowingListView();
    }

    private void setupFollowingListView() {
        followingListView.setCellFactory(param -> new ListCell<>() {
            private HBox content;
            private ImageView profileImageView;
            private Label nameLabel;
            private Label statusLabel;

            {
                content = new HBox(10);
                profileImageView = new ImageView();
                profileImageView.setFitWidth(40);
                profileImageView.setFitHeight(40);
                profileImageView.setPreserveRatio(true);

                nameLabel = new Label();
                nameLabel.getStyleClass().add("user-name-label");

                statusLabel = new Label();
                statusLabel.getStyleClass().add("user-status-label");

                content.getChildren().addAll(profileImageView, nameLabel, statusLabel);
            }

            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // 프로필 이미지 설정
                    Image profileImage = getImageFromBytes(user.getProfileImage());
                    profileImageView.setImage(profileImage != null ? profileImage : getDefaultProfileImage());
                    nameLabel.setText(user.getId());

                    // 온라인 상태 표시 업데이트
                    statusLabel.setText(user.isOnline() ? "온라인" : "오프라인");
                    statusLabel.setStyle(user.isOnline() ? "-fx-text-fill: green;" : "-fx-text-fill: red;");

                    setGraphic(content);

                    // Hover 시 스타일 적용
                    content.setOnMouseEntered(e -> content.setStyle("-fx-background-color: #f0f0f0;"));
                    content.setOnMouseExited(e -> content.setStyle("-fx-background-color: transparent;"));
                }
            }
        });

        followingListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // 더블 클릭 시 언팔로우
                User selectedUser = followingListView.getSelectionModel().getSelectedItem();
                if (selectedUser != null) {
                    unfollowUser(selectedUser.getId()); // 사용자 아이디로 언팔로우
                }
            }
        });
    }

    private void setupRecommendListView() {
        recommendListView.setCellFactory(param -> new ListCell<>() {
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

        recommendListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) { // 더블 클릭 시 팔로우
                User selectedUser = recommendListView.getSelectionModel().getSelectedItem();
                if (selectedUser != null) {
                    followUser(selectedUser.getId()); // 사용자 아이디로 팔로우
                }
            }
        });
    }

    // 팔로우 추천 목록 로드
    private void loadRecommendList() {
        List<User> recommendUsers = followDAO.getRecommendUsers(currentUser.getId());
        ObservableList<User> items = FXCollections.observableArrayList(recommendUsers);
        recommendListView.setPlaceholder(new Label("추천할 사용자가 없습니다."));
        recommendListView.setItems(items);
    }

    private Image getDefaultProfileImage() {
        return new Image(getClass().getResourceAsStream("/images/default_profile.jpg"));
    }

    private void rebuildCenterContent() {
        // 새로운 VBox 생성하여 중앙 콘텐츠 구성
        VBox feedContainer = new VBox();
        feedContainer.setSpacing(10);
        feedContainer.getStyleClass().add("feed-container");
        feedContainer.setPadding(new Insets(10, 10, 10, 10));

        // ListView 생성 및 설정
        postListView = new ListView<>();
        postListView.setPrefHeight(720);
        postListView.getStyleClass().add("list-view");
        postListView.setItems(postItems);
        setupPostListView();

        // ListView를 VBox에 추가
        feedContainer.getChildren().add(postListView);

        // 중앙 콘텐츠 영역에 설정
        mainBorderPane.setCenter(feedContainer);

        // 포스트 로드
        loadPosts();
    }

    @FXML
    void handleFollowRecommendUser(MouseEvent event) {
        if (event.getClickCount() == 2) { // 더블 클릭 시 팔로우
            User selectedUser = recommendListView.getSelectionModel().getSelectedItem();
            if (selectedUser != null) {
                followUser(selectedUser.getId());
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // 사용자 팔로우
    private void followUser(String userId) {
        boolean success = followDAO.followUser(userId, currentUser.getId());
        if (success) {
            loadFollowingList();
            loadRecommendList();
            showAlert("성공", userId + "님을 팔로우했습니다.");
        } else {
            showAlert("오류", "팔로우에 실패했습니다.");
        }
    }

    // 특정 사용자를 언팔로우하는 기능
    private void unfollowUser(String userId) {
        boolean success = followDAO.unfollowUser(userId, currentUser.getId());
        if (success) {
            loadFollowingList();
            loadRecommendList();
            showAlert("성공", userId + "님의 팔로우를 취소했습니다.");
        } else {
            showAlert("오류", "언팔로우에 실패했습니다.");
        }
    }

    @FXML
    void handlePost(ActionEvent event) {
        String text = postTextArea.getText().trim();
        if (text.isEmpty()) {
            showAlert("경고", "포스트 내용을 입력해주세요.");
            return;
        }

        byte[] imageData = attachedImageBytes; // 올바른 이미지 데이터 사용

        boolean success = postDAO.addPost(text, imageData, currentUser.getId());
        if (success) {
            postTextArea.clear();
            attachedImageBytes = null; // 이미지 선택 초기화
            attachedImageView.setImage(null); // 이미지 미리보기 초기화
            loadPosts(); // 포스트 다시 로드
            showAlert("성공", "포스트가 작성되었습니다.");
        } else {
            showAlert("오류", "포스트 작성에 실패했습니다.");
        }
    }


    @FXML
    void goToMyPage(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/mypage.fxml"));
            Parent root = loader.load();

            MyPageController myPageController = loader.getController();
            myPageController.setUser(currentUser); // 현재 사용자 정보를 전달하는 메서드가 있어야 함


            Stage stage = (Stage) profileImageView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("My Page");
            stage.setWidth(1080);
            stage.setHeight(720);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("오류", "마이페이지 로드 중 오류가 발생했습니다.");
        }
    }

    @FXML
    private void goToMainPage(MouseEvent event) {
        try {
            // 현재 창을 닫고 메인 창을 다시 로드하는 방식
            Stage currentStage = (Stage) twitterImage.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Parent root = loader.load();

            MainController mainController = loader.getController();
            mainController.setUser(currentUser);

            currentStage.setScene(new Scene(root));
            currentStage.setTitle("Twitter - Main");
            currentStage.setWidth(1080);
            currentStage.setHeight(720);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("오류", "메인 페이지 로드 중 오류가 발생했습니다.");
        }
    }

    @FXML
    void openPostCreation(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PostCreation.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = new Stage();
            stage.setTitle("포스트 작성");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "포스트 작성 창을 여는 중 오류가 발생했습니다.", ButtonType.OK);
            alert.showAndWait();
        }
    }


    public void setUser(User user) {
        this.currentUser = user;
        loadFollowingList();

        // 프로필 이미지 설정
        Image profileImage = getImageFromBytes(user.getProfileImage());
        profileImageView.setImage(profileImage);
    }


    private int getUnreadMessagesCount() {
        int unreadCount = 0;
        try {
            // DAO 객체 생성
            MessageDAO messageDAO = new MessageDAO();

            // 현재 사용자의 미확인 메시지 수 가져오기
            unreadCount = messageDAO.getUnreadMessagesCount(currentUser.getId());
            System.out.println("미확인 메시지 수: " + unreadCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return unreadCount;
    }

    public void updateMessageNotification() {
        int unreadMessagesCount = messageDAO.getUnreadMessagesCount(currentUser.getId());
        Platform.runLater(() -> {
            Button messagesButton = (Button) mainBorderPane.lookup("#messagesButton");
            if (messagesButton != null) {
                if (unreadMessagesCount > 0) {
                    messagesButton.setText("Messages (" + unreadMessagesCount + ")");
                } else {
                    messagesButton.setText("Messages");
                }
            } else {
                System.out.println("messagesButton을 찾을 수 없습니다.");
            }
        });
    }

    public void updateUserStatus(String userId, boolean isOnline) {
        Platform.runLater(() -> {
            for (User user : followingListView.getItems()) {
                if (user.getId().equals(userId)) {
                    user.setOnline(isOnline); // User 객체에 온라인 상태 속성 추가
                    followingListView.refresh();
                    break;
                }
            }
        });
    }


    public void updateUserStatus(String statusMessage) {
        // 예: "sbma0122 is now online" 이런 메시지로부터 사용자 ID와 상태 추출
        String[] parts = statusMessage.split(" ", 3);
        if (parts.length == 3) {
            String userId = parts[0];
            boolean isOnline = parts[2].equals("online");

            // 팔로잉 목록에서 해당 사용자의 상태를 업데이트
            for (User user : followingListView.getItems()) {
                if (user.getId().equals(userId)) {
                    user.setOnline(isOnline);
                    followingListView.refresh();
                    break;
                }
            }
        }
    }

    @FXML
    public void handleExit() {
        if (chatClient != null && chatClient.isAlive()) {
            chatClient.interrupt();  // 클라이언트 스레드 종료 요청
        }
        Platform.exit();
        System.exit(0);
    }

    public void displayConnectionError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("연결 오류");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public void updateFollowingStatus(String userId, boolean isOnline) {
        Platform.runLater(() -> {
            for (User user : followingListView.getItems()) {
                if (user.getId().equals(userId)) {
                    user.setOnline(isOnline);
                    followingListView.refresh();
                    break;
                }
            }
        });
    }

    public MessageController getMessageController() {
        return this.messageController; // 메세지 컨트롤러가 초기화 되어있다면 반환
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // 현재 사용자의 온라인 상태를 오프라인으로 변경
        userDAO.setUserOnlineStatus(currentUser.getId(), false);
        System.out.println("User " + currentUser.getId() + " has logged out.");

        // 채팅 클라이언트 종료 및 연결 해제 (리소스 정리)
        if (chatClient != null) {
            chatClient.stopClient(); // chatClient 연결 해제 메서드 호출
        }

        // 현재 창을 닫고 로그인 화면으로 전환
        Stage currentStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        currentStage.close(); // 현재 스테이지(메인 화면) 닫기

        try {
            // 로그인 화면 로드 및 표시
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage loginStage = new Stage();
            loginStage.setScene(new Scene(root));
            loginStage.setTitle("Login");
            loginStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
