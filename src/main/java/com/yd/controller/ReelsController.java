package com.yd.controller;

import com.yd.dao.ReelsDAO;
import com.yd.model.Reel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.util.List;

public class ReelsController {

    @FXML
    private ListView<Reel> reelsListView;

    private ReelsDAO reelsDAO = new ReelsDAO();

    @FXML
    public void initialize() {
        loadReels();

        // 커스텀 셀 팩토리 설정
        reelsListView.setCellFactory(new Callback<ListView<Reel>, ListCell<Reel>>() {
            @Override
            public ListCell<Reel> call(ListView<Reel> param) {
                return new ListCell<Reel>() {
                    private ImageView videoImageView = new ImageView();
                    private Label descriptionLabel = new Label();
                    private VBox vBox = new VBox(descriptionLabel);
                    private HBox hBox = new HBox(videoImageView, vBox);

                    {
                        videoImageView.setFitWidth(200);
                        videoImageView.setFitHeight(200);
                        videoImageView.setPreserveRatio(true);
                        descriptionLabel.setWrapText(true);
                        hBox.setSpacing(10);
                    }

                    @Override
                    protected void updateItem(Reel reel, boolean empty) {
                        super.updateItem(reel, empty);
                        if (empty || reel == null) {
                            setText(null);
                            setGraphic(null);
                        } else {
                            // 영상의 썸네일을 이미지로 표시 (실제 영상 재생은 추가 구현 필요)
                            // 여기서는 VIDEO_URL이 영상의 썸네일 URL이라고 가정
                            Image thumbnail = new Image(reel.getVideoUrl(), 200, 200, true, true, true);
                            videoImageView.setImage(thumbnail);
                            descriptionLabel.setText(reel.getDescription());
                            setGraphic(hBox);
                        }
                    }
                };
            }
        });
    }

    private void loadReels() {
        // 예시로 첫 100개의 Reel을 가져옵니다.
        List<Reel> reels = reelsDAO.getAllReels(0, 100);
        ObservableList<Reel> observableReels = FXCollections.observableArrayList(reels);
        reelsListView.setItems(observableReels);
    }
}
