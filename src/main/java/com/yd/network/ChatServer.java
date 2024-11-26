package com.yd.network;

import com.yd.dao.MessageDAO;
import com.yd.model.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ChatServer {
    private static Map<String, PrintWriter> clientWriters = new HashMap<>();
    private static MessageDAO messageDAO = new MessageDAO();

    public static void main(String[] args) {
        System.out.println("Chat server started...");
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private String clientId;
        private PrintWriter out;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                 
                this.out = out;
                clientId = in.readLine();
                synchronized (clientWriters) {
                    clientWriters.put(clientId, out);
                }
                System.out.println(clientId + " connected.");

                String message;
                while ((message = in.readLine()) != null) {
                    String[] parts = message.split(":", 2);
                    if (parts.length == 2) {
                        String targetClient = parts[0];
                        String messageText = parts[1];
                        handleMessage(clientId, targetClient, messageText);
                    }
                }
            } catch (IOException e) {
                System.out.println(clientId + " disconnected.");
            } finally {
                synchronized (clientWriters) {
                    clientWriters.remove(clientId);
                }
            }
        }

        private void handleMessage(String senderId, String receiverId, String messageText) {
            Message message = new Message(senderId, receiverId, messageText);
            messageDAO.saveMessage(message);

            PrintWriter receiverOut = clientWriters.get(receiverId);
            if (receiverOut != null) {
                receiverOut.println(senderId + ": " + messageText);
            }
        }
    }
}
