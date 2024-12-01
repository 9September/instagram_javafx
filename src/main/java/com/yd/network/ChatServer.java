package com.yd.network;

import com.yd.dao.MessageDAO;
import com.yd.dao.UserDAO;
import com.yd.model.Message;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static Map<String, ClientHandler> clientHandlers = new ConcurrentHashMap<>();
    private static MessageDAO messageDAO = new MessageDAO();
    private static UserDAO userDAO = new UserDAO();

    public static void main(String[] args) {
        System.out.println("Chat server started...");
        ExecutorService executorService = Executors.newCachedThreadPool();
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                executorService.submit(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isUserOnline(String userId) {
        return clientHandlers.containsKey(userId);
    }

    public static void broadcastUserStatus(String userId, boolean isOnline) {
        String statusMessage = userId + (isOnline ? " is now online." : " is now offline.");
        for (ClientHandler handler : clientHandlers.values()) {
            handler.sendMessage(statusMessage);
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private String clientId;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

                this.out = out;
                clientId = in.readLine();
                synchronized (clientHandlers) {
                    clientHandlers.put(clientId, this);
                }
                System.out.println(clientId + " connected.");
                userDAO.setUserOnlineStatus(clientId, true);
                broadcastUserStatus(clientId, true);

                sendPendingMessages();

                String message;
                while ((message = in.readLine()) != null) {
                    String[] parts = message.split(":", 2);
                    if (parts.length == 2) {
                        String targetClient = parts[0];
                        String messageText = parts[1];
                        handleMessage(clientId, targetClient, messageText);
                    } else if (message.startsWith("READ_MESSAGE:")) {
                        String messageIdStr = message.substring("READ_MESSAGE:".length());
                        handleReadMessage(messageIdStr);
                    }
                }
            } catch (IOException e) {
                System.out.println(clientId + " disconnected unexpectedly.");
            } finally {
                synchronized (clientHandlers) {
                    clientHandlers.remove(clientId);
                }
                userDAO.setUserOnlineStatus(clientId, false);  // 연결 종료 시 사용자 상태를 오프라인으로 업데이트
                broadcastUserStatus(clientId, false);
                System.out.println(clientId + " disconnected and set to offline.");
            }
        }

        private void handleMessage(String senderId, String receiverId, String messageText) {
            Message message = new Message(senderId, receiverId, messageText);
            messageDAO.saveMessage(message);

            // 수신자가 온라인인지 확인
            ClientHandler receiverHandler = clientHandlers.get(receiverId);
            if (receiverHandler != null) {
                // 메시지 형식: messageId:senderId:messageText
                receiverHandler.sendMessage(message.getMessageId() + ":" + senderId + ": " + messageText);
            } else {
                // 수신자가 오프라인일 경우 로그 출력
                System.out.println("Receiver not online or handler not found: " + receiverId);
            }
        }


        private void sendPendingMessages() {
            List<Message> pendingMessages = messageDAO.getUnreadMessages(clientId);
            for (Message msg : pendingMessages) {
                sendMessage(msg.getSenderId() + ": " + msg.getMessageText());
                messageDAO.markMessageAsRead(msg.getMessageId());
            }
        }

        private void handleReadMessage(String messageId) {
            try {
                int id = Integer.parseInt(messageId);
                messageDAO.updateMessageStatusToRead(id);
            } catch (NumberFormatException e) {
                System.err.println("Invalid message ID format: " + messageId);
            }
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }
    }
}
