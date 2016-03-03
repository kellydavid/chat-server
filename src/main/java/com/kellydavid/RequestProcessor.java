package com.kellydavid;

import java.io.InputStream;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class RequestProcessor implements Runnable{

    private Socket so;
    private Chatroom chatroom;
    public static final int RECEIVE_BUFFER_SIZE = 2048;
    private boolean connectionAlive = true;

    /**
     * Constructor for RequestProcessor accepts socket of client.
     * @param so
     */
    public RequestProcessor(Socket so, Chatroom chatroom){
        this.so = so;
        this.chatroom = chatroom;
    }

    public void run()
    {
        try{
            while(connectionAlive) {
                // receive data
                String recvd = readRequest(so.getInputStream());
                // process request
                if(recvd != null)
                    process(recvd);
            }
            // close socket
            so.close();
        }catch(Exception e){
            System.err.println("CS: Error processing request\n");
            e.printStackTrace();
        }
    }

    private synchronized String readRequest(InputStream is){
        InputStreamReader isr = new InputStreamReader(is);
        char[] buffer = new char[RECEIVE_BUFFER_SIZE];
        char[] result = null;
        int read = 0;
            try {
                read = isr.read(buffer, 0, buffer.length);
                if(read>0){
                    result = new char[read];
                    System.arraycopy(buffer, 0, result, 0, read);
                    return new String(result);
                }else{
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        return null;
    }

//    private synchronized String readRequest(InputStream is){
//        InputStreamReader isr = new InputStreamReader(is);
//        char[] buffer = new char[RECEIVE_BUFFER_SIZE];
//        char[] result = null;
//        int read = 0;
//        boolean keepReading = true;
//        while(keepReading) {
//            try {
//                read = isr.read(buffer, 0, buffer.length);
//                if(read>0){
//                    result = new char[read];
//                    System.arraycopy(buffer, 0, result, 0, read);
//                    keepReading= false;
//                }
//            } catch (Exception e) {
//                keepReading = false;
//                e.printStackTrace();
//            }
//        }
//        return new String(result);
//    }

    private void process(String request)
    {
        if(request.startsWith("HELO")) {
            System.out.print("CS: Received HELO request\n");
            heloHandler(request);
        }
        else if(request.startsWith("JOIN_CHATROOM")){
            System.out.print("CS: Received JOIN_CHATROOM request\n");
            joinChatroomHandler(request);
        }
        else if(request.startsWith("LEAVE_CHATROOM")){
            System.out.print("CS: Received JOIN_CHATROOM request\n");
            leaveChatroomHandler(request);
        }
        else if(request.startsWith("KILL_SERVICE")){
            System.out.print("CS: Received KILL_SERVICE request\n");
            System.exit(0);
        }
        else{
            System.out.print("CS: Received unknown request\n");
        }
    }

    private synchronized void heloHandler(String request)
    {
        sendResponse(request +
                "\nIP:" + so.getLocalAddress().getHostAddress() +
                "\nPort:" + so.getLocalPort() +
                "\nStudentID:" + App.STUDENT_ID);
        connectionAlive = false;
    }

    private synchronized void joinChatroomHandler(String request)
    {
        System.out.println("Request:\n" + request);
        String data[] = request.split("\n");
        HashMap<String, String> joinRequest = new HashMap<String, String>();
        for (int i = 0; i < data.length; i++) {
            String str[] = data[i].split(":");
            joinRequest.put(str[0], str[1]);
        }
        if(chatroom.isClientMemberOfRoom(chatroom.getRef(joinRequest.get("JOIN_CHATROOM")), chatroom.getRef(joinRequest.get("CLIENT_NAME")))){
            sendErrorResponse(1, "Client already exists");
            connectionAlive = false;
            return;
        }
        if(!chatroom.doesRoomExist(joinRequest.get("JOIN_CHATROOM"))){
            // create room
            chatroom.createRoom(joinRequest.get("JOIN_CHATROOM"));
        }
        if(!chatroom.doesClientExist(joinRequest.get("CLIENT_NAME"))){
            // add client
            if(joinRequest.get("CLIENT_IP").equals("0") && joinRequest.get("PORT").equals("0"))
            {
                chatroom.addClient(joinRequest.get("CLIENT_NAME"), true, joinRequest.get("CLIENT_IP"), joinRequest.get("PORT"), this);
            }
            else
            {
                chatroom.addClient(joinRequest.get("CLIENT_NAME"), false, joinRequest.get("CLIENT_IP"), joinRequest.get("PORT"), null);
            }
        }
        // add client to room
        chatroom.addClientToRoom(joinRequest.get("JOIN_CHATROOM"), joinRequest.get("CLIENT_NAME"));
        String response = "JOINED_CHATROOM:" + joinRequest.get("JOIN_CHATROOM") + "\n" +
                            "SERVER_IP:" + chatroom.getAddress() + "\n" +
                            "PORT:" + chatroom.getPort() + "\n" +
                            "ROOM_REF:" + chatroom.getRef(joinRequest.get("JOIN_CHATROOM")) + "\n" +
                            "JOIN_ID: " + chatroom.getRef(joinRequest.get("CLIENT_NAME")) + "\n";
        System.out.println(response);
        sendResponse(response);
        chatroom.sendMessageToRoom(chatroom.getRef(joinRequest.get("JOIN_CHATROOM")), joinRequest.get("CLIENT_NAME"), joinRequest.get("CLIENT_NAME") + " has joined this chatroom.");
    }

    private synchronized void leaveChatroomHandler(String request)
    {
        System.out.println("Request balh:\n" + request);
        String data[] = request.split("\n");
        HashMap<String, String> leaveRequest = new HashMap<String, String>();
        for (int i = 0; i < data.length; i++) {
            String str[] = data[i].split(":");
            leaveRequest.put(str[0], str[1]);
        }
        if(!chatroom.isClientMemberOfRoom(Integer.parseInt(leaveRequest.get("LEAVE_CHATROOM")), Integer.parseInt(leaveRequest.get("JOIN_ID")))){
            sendErrorResponse(1, "Client already not a member of chatroom");
            return;
        }
        // disconnect client
        chatroom.removeClientFromRoom(Integer.parseInt(leaveRequest.get("LEAVE_CHATROOM")), Integer.parseInt(leaveRequest.get("JOIN_ID")));
        String response = "LEFT_CHATROOM:" + leaveRequest.get("LEAVE_CHATROOM") + "\n" +
                            "JOIN_ID: " + leaveRequest.get("JOIN_ID") + "\n";
        System.out.println(response);
        sendResponse(response);
        chatroom.sendMessageToRoom(Integer.parseInt(leaveRequest.get("LEAVE_CHATROOM")), leaveRequest.get("CLIENT_NAME"), leaveRequest.get("CLIENT_NAME") + " has left this chatroom.");
    }

    public synchronized void sendChatMessageToClient(Integer room_ref, String client, String message)
    {
        String response = "CHAT:" + room_ref + "\nCLIENT_NAME:" + client + "\nMESSAGE: " + message + "\n\n";
        System.out.println(response);
        sendResponse(response);
    }

    private synchronized void sendErrorResponse(int errorCode, String message){
        sendResponse("ERROR_CODE:" + errorCode + "\n" + "ERROR_DESCRIPTION:" + message);
    }

    private synchronized void sendResponse(String response)
    {
        try {
            so.getOutputStream().write(response.getBytes());
            so.getOutputStream().flush();
        }catch(Exception e){
            System.err.println("CS: Error sending response.\n");
            e.printStackTrace();
        }
    }
}