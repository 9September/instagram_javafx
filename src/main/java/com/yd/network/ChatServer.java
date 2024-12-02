package com.yd.network;

import com.yd.dao.GroupDAO;
import com.yd.dao.MessageDAO;
import com.yd.dao.UserDAO;
import com.yd.model.Group;
import com.yd.model.Message;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static Map<String, ClientHandler> clientHandlers = new ConcurrentHashMap<>();
    private static MessageDAO messageDAO = new MessageDAO();
    private static UserDAO userDAO = new UserDAO();
    private static GroupDAO groupDAO = new GroupDAO();

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

    public static void broadcastGroupMessage(int groupId, String message) {
        Group group = groupDAO.getGroupById(groupId);
        if (group != null) {
            for (String memberId : group.getMemberIds()) {
                ClientHandler handler = clientHandlers.get(memberId);
                if (handler != null) {
                    handler.sendMessage("GROUP:" + groupId + ":" + message);
                }
            }
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
                if (clientId == null || clientId.isEmpty()) {
                    socket.close();
                    return;
                }

                synchronized (clientHandlers) {
                    clientHandlers.put(clientId, this);
                }
                System.out.println(clientId + " connected.");
                userDAO.setUserOnlineStatus(clientId, true);
                broadcastUserStatus(clientId, true);

                sendPendingMessages();

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("CREATE_GROUP:")) {
                        handleCreateGroup(message.substring("CREATE_GROUP:".length()));
                    } else if (message.startsWith("GROUP_MESSAGE:")) {
                        handleGroupMessage(message.substring("GROUP_MESSAGE:".length()));
                    } else if (message.startsWith("READ_MESSAGE:")) {
                        String messageIdStr = message.substring("READ_MESSAGE:".length());
                        handleReadMessage(messageIdStr);
                    } else {
                        // 개인 메시지 처리
                        String[] parts = message.split(":", 2);
                        if (parts.length == 2) {
                            String targetClient = parts[0];
                            String messageText = parts[1];
                            handleMessage(clientId, targetClient, messageText);
                        }
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

        private void handleCreateGroup(String groupData) {
            // 그룹 생성 데이터 형식: groupName,user1,user2,user3,...
            String[] parts = groupData.split(",");
            if (parts.length < 2) {
                sendMessage("ERROR:그룹 이름과 최소 한 명의 멤버를 선택해주세요.");
                return;
            }
            String groupName = parts[0];
            List<String> memberIds = Arrays.asList(Arrays.copyOfRange(parts, 1, parts.length));
            memberIds.add(clientId); // 그룹 생성자 포함

            Group group = new Group(groupName, clientId, memberIds);
            int groupId = groupDAO.createGroup(group);

            if (groupId != -1) {
                sendMessage("GROUP_CREATED:" + groupId);
                broadcastGroupMessage(groupId, clientId + "님이 그룹 \"" + groupName + "\"을 생성했습니다.");
                System.out.println("Group created: " + groupName + " (ID: " + groupId + ")");
            } else {
                sendMessage("ERROR:그룹 생성에 실패했습니다.");
            }
        }

        private void handleGroupMessage(String groupMessageData) {
            // 그룹 메시지 데이터 형식: groupId:messageText
            String[] parts = groupMessageData.split(":", 2);
            if (parts.length != 2) {
                sendMessage("ERROR:잘못된 그룹 메시지 형식입니다.");
                return;
            }
            try {
                int groupId = Integer.parseInt(parts[0]);
                String messageText = parts[1];

                Group group = groupDAO.getGroupById(groupId);
                if (group == null) {
                    sendMessage("ERROR:존재하지 않는 그룹입니다.");
                    return;
                }

                // 메시지 저장
                Message message = new Message(clientId, groupId, messageText);
                messageDAO.saveMessage(message);

                // 그룹에 속한 모든 사용자에게 메시지 브로드캐스트
                String formattedMessage = clientId + " [그룹 " + groupId + "]: " + messageText + " (" + message.getTimestamp() + ")";
                broadcastGroupMessage(groupId, formattedMessage);
            } catch (NumberFormatException e) {
                sendMessage("ERROR:그룹 ID는 숫자여야 합니다.");
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
            // 개인 메시지
            List<Message> pendingMessages = messageDAO.getUnreadMessages(clientId);
            for (Message msg : pendingMessages) {
                sendMessage(msg.getMessageId() + ":" + msg.getSenderId() + ": " + msg.getMessageText());
                messageDAO.markMessageAsRead(msg.getMessageId());
            }

            // 그룹 메시지
            List<Group> groups = groupDAO.getGroupsByUserId(clientId);
            for (Group group : groups) {
                List<Message> pendingGroupMessages = messageDAO.getUnreadGroupMessages(group.getGroupId());
                for (Message msg : pendingGroupMessages) {
                    sendMessage("GROUP:" + group.getGroupId() + ":" + msg.getSenderId() + ": " + msg.getMessageText());
                    messageDAO.markMessageAsRead(msg.getMessageId());
                }
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
