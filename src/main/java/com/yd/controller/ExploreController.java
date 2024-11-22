package com.yd.controller;

import com.yd.dao.PostDAO;
import com.yd.model.Post;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

public class ExploreController {

    @FXML
    private TilePane exploreTilePane;

    private PostDAO postDAO = new PostDAO();

    @FXML
    public void initialize() {
        loadExploreContent();
    }

    private void loadExploreContent() {
        List<Post> posts = postDAO.getAllPosts();

        for (Post post : posts) {
            if (post.getImage() != null && post.getImage().length > 0) {
                ImageView imageView = new ImageView();
                Image image = new Image(new ByteArrayInputStream(post.getImage()));
                imageView.setImage(image);
                imageView.setFitWidth(200);
                imageView.setFitHeight(200);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);
                imageView.setCache(true);

                // 이미지 클릭 시 상세보기 창 열기
                imageView.setOnMouseClicked(event -> {
                    openPostDetailWindow(post);
                });

                exploreTilePane.getChildren().add(imageView);
            }
        }
    }

    private void openPostDetailWindow(Post post) {
        // 새 창(Stage) 생성
        Stage detailStage = new Stage();
        detailStage.initModality(Modality.APPLICATION_MODAL);
        detailStage.setTitle("포스트 상세보기");

        // 포스트 이미지
        ImageView postImageView = new ImageView();
        if (post.getImage() != null && post.getImage().length > 0) {
            Image image = new Image(new ByteArrayInputStream(post.getImage()));
            postImageView.setImage(image);
        }
        postImageView.setFitWidth(400);
        postImageView.setFitHeight(400);
        postImageView.setPreserveRatio(true);
        postImageView.setSmooth(true);
        postImageView.setCache(true);

        // 포스트 텍스트
        Label postTextLabel = new Label(post.getText());
        postTextLabel.setWrapText(true);
        postTextLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #333333;");

        // 작성자 정보
        Label authorLabel = new Label("작성자: " + post.getWriterId());
        authorLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");

        // 좋아요 수
        Label likesLabel = new Label("좋아요: " + post.getNumOfLikes());
        likesLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666666;");


        // 레이아웃 구성
        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20));
        layout.getChildren().addAll(postImageView, postTextLabel, authorLabel, likesLabel);

        Scene scene = new Scene(layout, 500, 600);
        detailStage.setScene(scene);
        detailStage.showAndWait();
    }
}