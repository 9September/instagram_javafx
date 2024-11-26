package com.yd.network;

import com.yd.controller.MainController;
import com.yd.controller.MessageController;
import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient extends Thread {
    private final String serverAddress = "localhost";
    private final int serverPort = 12345;
    private String currentUserId;
    private MessageController messageController;
    private MainController mainController;  
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public ChatClient(String currentUserId, MessageController messageController) {
        this.currentUserId = currentUserId;
        this.messageController = messageController;
        this.mainController = mainController;
    }

    @Override
    public void run() {
        try {
            socket = new Socket(serverAddress, serverPort);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // 서버로부터 실시간 메시지 수신 대기
            String message;
            while ((message = in.readLine()) != null) {
                String finalMessage = message;
                Platform.runLater(() -> messageController.addMessageToView(finalMessage));
                
                if (mainController != null) {
                    Platform.runLater(() -> mainController.updateMessageNotification());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(currentUserId + ": " + message);
        }
    }
    public void onMessageReceived(String message) {
        Platform.runLater(() -> {
            messageController.addMessageToView(message);
            
            // 알림 업데이트를 위한 호출 추가
            if (mainController != null) {
                mainController.updateMessageNotification();
            }
        });
    }
}
