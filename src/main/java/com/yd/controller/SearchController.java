package com.yd.controller;

import com.yd.dao.UserDAO;
import com.yd.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.util.List;

public class SearchController {

    @FXML
    private TextField searchField;

    @FXML
    private ListView<User> searchResultListView;

    private UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            performSearch(newValue);
        });
    }

    private void performSearch(String query) {
        if (query == null || query.isEmpty()) {
            searchResultListView.setItems(FXCollections.observableArrayList());
            return;
        }

        List<User> users = userDAO.searchUsersByIdOrName(query);
        ObservableList<User> observableUsers = FXCollections.observableArrayList(users);
        searchResultListView.setItems(observableUsers);
    }
}
