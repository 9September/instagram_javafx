package com.yd.controller;

import com.yd.dao.CommentDAO;
import com.yd.dao.PostDAO;
import com.yd.model.Comment;
import com.yd.model.Post;
import com.yd.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class PostController {

    @FXML
    private Label postLabel;

    @FXML
    private TextArea postTextArea;

    @FXML
    private ListView<Comment> commentListView;

    @FXML
    private TextField commentField;

    @FXML
    private Button attachImageButton;

    @FXML
    private ImageView attachedImageView;

    private byte[] attachedImageBytes = null;

    @FXML
    private Button addCommentButton;

    private CommentDAO commentDAO = new CommentDAO();
    private PostDAO postDAO = new PostDAO();
    private User currentUser;
    private Post currentPost;

    public void setPost(Post post) {
        this.currentPost = post;
        if (postLabel != null) {
            postLabel.setText("[" + post.getWriterId() + "] " + post.getText());
        }
        loadComments();
    }

    @FXML
    public void initialize() {
        currentUser = LoginController.getCurrentUser();
        if (commentListView != null) {
            commentListView.setCellFactory(param -> new ListCell<>() {
                @Override
                protected void updateItem(Comment comment, boolean empty) {
                    super.updateItem(comment, empty);
                    if (empty || comment == null) {
                        setText(null);
                    } else {
                        setText("[" + comment.getWriterId() + "] " + comment.getText() + " (" + comment.getNumOfLikes() + " likes)");
                    }
                }
            });
        }
    }

    // 댓글 목록 로드
    private void loadComments() {
        if (currentPost == null || commentListView == null) return;
        List<Comment> comments = commentDAO.getCommentsByPostId(currentPost.getPostId());
        ObservableList<Comment> items = FXCollections.observableArrayList(comments);
        commentListView.setItems(items);
    }

    @FXML
    void handleAddComment() {
        if (commentField == null) return;
        String text = commentField.getText();
        if (text.isEmpty()) {
            return;
        }
        boolean success = commentDAO.addComment(text, currentUser.getId(), currentPost.getPostId());
        if (success) {
            commentField.clear();
            loadComments();
        } else {
            // 댓글 추가 실패 시 메시지 표시
            showAlert("오류", "댓글 작성에 실패했습니다.");
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
                attachedImageView.setVisible(true);
                showAlert("성공", "이미지가 첨부되었습니다.");
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("오류", "이미지 첨부 중 오류가 발생했습니다.");
            }
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
            showAlert("성공", "포스트가 작성되었습니다.");
        } else {
            showAlert("오류", "포스트 작성에 실패했습니다.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
