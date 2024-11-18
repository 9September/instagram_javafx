package com.yd.controller;

import com.yd.dao.UserDAO;
import com.yd.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;

public class EditProfileController {

    @FXML
    private TextField emailField;

    @FXML
    private DatePicker birthdayPicker;

    @FXML
    private TextField phoneField;

    private User currentUser;
    private UserDAO userDAO = new UserDAO();

    public void setUser(User user) {
        this.currentUser = user;
        emailField.setText(user.getEmail());
        birthdayPicker.setValue(user.getBirthday());
        phoneField.setText(user.getPhoneNumber());
    }

    @FXML
    private void handleSave() {
        String newEmail = emailField.getText().trim();
        LocalDate newBirthday = birthdayPicker.getValue();
        String newPhone = phoneField.getText().trim();

        boolean success = userDAO.updateUserProfile(currentUser.getId(), newEmail, newPhone, newBirthday, null);
        if (success) {
            currentUser.setEmail(newEmail);
            currentUser.setBirthday(newBirthday);
            currentUser.setPhoneNumber(newPhone);

            Alert alert = new Alert(Alert.AlertType.INFORMATION, "프로필이 성공적으로 수정되었습니다.", ButtonType.OK);
            alert.showAndWait();

            // 창 닫기
            Stage stage = (Stage) emailField.getScene().getWindow();
            stage.close();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR, "프로필 수정에 실패했습니다.", ButtonType.OK);
            alert.showAndWait();
        }
    }
}
