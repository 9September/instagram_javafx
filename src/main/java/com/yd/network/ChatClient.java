package com.yd.network;

import com.yd.controller.MessageController;
import com.yd.controller.MainController;

import java.io.*;
import java.net.Socket;
import javafx.application.Platform;

public class ChatClient extends Thread {
    private final String serverAddress = "localhost";
    private final int serverPort = 12345;
    private String currentUserId;
    private MessageController messageController;
    private MainController mainController;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean running;

    public ChatClient(String currentUserId, MessageController messageController, MainController mainController) {
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

            // 서버에 사용자 ID 전송
            out.println(currentUserId);
            System.out.println("Connected to server as " + currentUserId);
            running = true;

            // 서버로부터 실시간 메시지 수신 대기
            String message;
            while (running && (message = in.readLine()) != null) {
                String finalMessage = message;

                Platform.runLater(() -> {
                    try {
                        // 상태 변경 메시지 처리
                        if (finalMessage.contains("is now online") || finalMessage.contains("is now offline")) {
                            mainController.updateUserStatus(finalMessage);
                        } else {
                            // 메시지의 발신자를 확인하여 중복 알림 방지
                            String[] messageParts = finalMessage.split(": ", 2);
                            if (messageParts.length == 2) {
                                String senderId = messageParts[0].trim();
                                String messageContent = messageParts[1].trim();

                                // 자신이 보낸 메시지가 아닌 경우만 알림 카운트 업데이트 및 메시지 뷰에 추가
                                if (!senderId.equals(currentUserId)) {
                                    if (mainController != null) {
                                        mainController.updateMessageNotification(); // 메시지 알림 개수 업데이트
                                    }
                                    if (messageController != null) {
                                        messageController.addMessageToView(senderId + ": " + messageContent);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error updating UI components: " + e.getMessage());
                    }
                });
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Error while running ChatClient: " + e.getMessage());
                e.printStackTrace();
            }
        } finally {
            cleanup();
        }
    }


    // 리소스 정리 메서드 추가
    private void cleanup() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("ChatClient resources cleaned up for user: " + currentUserId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        if (out != null && !message.trim().isEmpty()) {
            // 메시지 전송 로그 추가
            System.out.println("Sending message from " + currentUserId + ": " + message);
            out.println(currentUserId + ": " + message);
        }
    }

    public void requestFollowingStatus() {
        if (out != null) {
            out.println("REQUEST_STATUS");
        }
    }

    // stopClient 메서드 추가
    public void stopClient() {
        running = false; // 수신 루프 종료를 위한 플래그 설정

        // 서버와의 연결을 끊고 리소스를 해제
        if (out != null) {
            out.println(currentUserId + " disconnected");
        }
        cleanup(); // cleanup 메서드를 사용하여 자원 정리
        System.out.println("Client " + currentUserId + " has been disconnected from the server.");
    }


    public void markMessageAsRead(int messageId) {
        // 메시지를 읽음 처리하기 위해 서버에 요청을 보냄
        if (out != null) {
            out.println("READ_MESSAGE:" + messageId);
        }
    }
}
