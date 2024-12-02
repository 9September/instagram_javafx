package com.yd.network;

import com.yd.controller.MessageController;
import com.yd.controller.MainController;

import java.io.*;
import java.net.Socket;
import java.util.List;

import com.yd.model.Group;
import javafx.application.Platform;

public class ChatClient extends Thread {
    private final String serverAddress = "localhost"; // 서버 주소
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
                        if (finalMessage.startsWith("GROUP_CREATED:")) {
                            int groupId = Integer.parseInt(finalMessage.substring("GROUP_CREATED:".length()));
                            messageController.notifyGroupCreated(groupId);
                        } else if (finalMessage.startsWith("GROUP:")) {
                            // 그룹 메시지 형식: GROUP:groupId:senderId:messageText
                            String[] parts = finalMessage.split(":", 4);
                            if (parts.length == 4) {
                                int groupId = Integer.parseInt(parts[1]);
                                String senderId = parts[2];
                                String messageText = parts[3];
                                String displayMessage = senderId + " [그룹 " + groupId + "]: " + messageText;
                                messageController.addMessageToView(displayMessage);
                            }
                        } else if (finalMessage.contains("is now online") || finalMessage.contains("is now offline")) {
                            mainController.updateUserStatus(finalMessage);
                        } else {
                            // 개인 메시지 형식: messageId:senderId:messageText
                            String[] messageParts = finalMessage.split(":", 3);
                            if (messageParts.length == 3) {
                                int messageId = Integer.parseInt(messageParts[0].trim());
                                String senderId = messageParts[1].trim();
                                String messageContent = messageParts[2].trim();

                                // 자신이 보낸 메시지가 아닌 경우만 메시지 뷰에 추가
                                if (!senderId.equals(currentUserId)) {
                                    if (messageController != null) {
                                        messageController.addMessageToView(senderId + ": " + messageContent);
                                    }

                                    // 메시지 읽음 처리
                                    this.markMessageAsRead(messageId);
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

    // 리소스 정리 메서드
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

    // 메시지 전송 메서드
    public void sendMessage(String message) {
        if (out != null && !message.trim().isEmpty()) {
            // 메시지 전송 로그 추가
            System.out.println("Sending message from " + currentUserId + ": " + message);
            // 개인 메시지 형식: receiverId:messageText
            // 그룹 메시지: GROUP_MESSAGE:groupId:messageText
            out.println(message);
        }
    }

    // 그룹 메시지 전송 메서드 추가
    public void sendGroupMessage(int groupId, String messageText) {
        if (out != null && !messageText.trim().isEmpty()) {
            String formattedMessage = "GROUP_MESSAGE:" + groupId + ":" + messageText;
            sendMessage(formattedMessage);
        }
    }

    public void sendGroupCreation(Group group) {
        // 서버에 그룹 생성 요청 전송
        String message = "CREATE_GROUP:" + group.getGroupName() + "," +
                String.join(",", group.getMemberIds());
        sendMessage(message);
    }

    // 그룹 생성 메서드 추가
    public void createGroup(String groupName, List<String> memberIds) {
        if (out != null && groupName != null && !groupName.trim().isEmpty()) {
            // 그룹 생성 데이터 형식: groupName,user1,user2,user3,...
            StringBuilder groupData = new StringBuilder();
            groupData.append(groupName);
            for (String userId : memberIds) {
                groupData.append(",").append(userId);
            }
            String formattedMessage = "CREATE_GROUP:" + groupData.toString();
            sendMessage(formattedMessage);
        }
    }

    // 팔로잉 상태 요청 메서드 (필요시 사용)
    public void requestFollowingStatus() {
        if (out != null) {
            out.println("REQUEST_STATUS");
        }
    }

    // 클라이언트 중지 메서드
    public void stopClient() {
        running = false; // 수신 루프 종료를 위한 플래그 설정

        // 서버와의 연결을 끊고 리소스를 해제
        if (out != null) {
            out.println(currentUserId + " disconnected");
        }
        cleanup(); // cleanup 메서드를 사용하여 자원 정리
        System.out.println("Client " + currentUserId + " has been disconnected from the server.");
    }

    // 메시지 읽음 상태로 업데이트 메서드
    public void markMessageAsRead(int messageId) {
        // 메시지를 읽음 처리하기 위해 서버에 요청을 보냄
        if (out != null) {
            out.println("READ_MESSAGE:" + messageId);
        }
    }
}
