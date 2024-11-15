package com.yd.controller;

import com.yd.dao.PostDAO;
import com.yd.model.Post;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;

import java.io.ByteArrayInputStream;
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

                // 이미지 클릭 시 상세보기 또는 다른 동작을 원한다면 이벤트 핸들러를 추가할 수 있습니다.
                imageView.setOnMouseClicked(event -> {
                    // 상세보기 로직 구현
                });

                exploreTilePane.getChildren().add(imageView);
            }
        }
    }
}
