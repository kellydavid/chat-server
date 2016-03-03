package com.kellydavid;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by davidkelly on 02/03/2016.
 */
public class Chatroom {
    private String address;
    private int port;
    private HashMap<Integer, Client> clients; // client ref -> Client object
    private HashMap<Integer, ArrayList<Integer>> rooms; // room ref -> List of client ref memebers

    public Chatroom(String address, int port)
    {
        this.address = address;
        this.port = port;
        this.rooms = new HashMap<Integer, ArrayList<Integer>>();
        this.clients = new HashMap<Integer, Client>();
    }

    public synchronized void sendMessageToRoom(String room, String client, String message)
    {
        ArrayList<Integer> clientList = rooms.get(room.hashCode());
        for (Integer clientRef: clientList) {
            // get client
            Client connection = clients.get(clientRef);
            connection.getConnection().sendChatMessageToClient(room.hashCode(), client, message);
        }   
    }

    public synchronized Integer getRef(String string){
        return string.hashCode();
    }

    public synchronized boolean doesRoomExist(String room)
    {
        return rooms.get(room.hashCode()) != null ? true:false;
    }

    public synchronized boolean doesClientExist(String client)
    {
        return clients.get(client.hashCode()) != null ? true:false;
    }

    public synchronized void createRoom(String room)
    {
        rooms.put(room.hashCode(), new ArrayList<Integer>());
    }

    public synchronized void addClient(String client, boolean tcp, String address, String port, RequestProcessor connection){
        clients.put(client.hashCode(), new Client(client, tcp, address, port, connection));
    }

    public synchronized void addClientToRoom(String room, String client){
        rooms.get(room.hashCode()).add(client.hashCode());
    }

    public synchronized void removeClientFromRoom(String room, String client){
        rooms.get(room.hashCode()).remove(client.hashCode());
    }

    public synchronized boolean isClientMemberOfRoom(String room, String client)
    {
        ArrayList<Integer> clients = rooms.get(room.hashCode());
        if(clients == null) return false;
        return clients.contains(client.hashCode()) ? true:false;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    class Client
    {
        private RequestProcessor connection;
        private String name;
        private boolean tcp; // if false then use udp
        private String ip;
        private String port;

        public Client(String name, RequestProcessor connection){
            this(name, true, "0", "0", connection);
        }

        public Client(String name, boolean tcp, String ip, String port, RequestProcessor connection){
            this.name = name;
            this.tcp = tcp;
            this.ip = ip;
            this.port = port;
            this.connection = connection;
        }

        public RequestProcessor getConnection() {
            return connection;
        }

        public synchronized String getName() {
            return name;
        }

        public synchronized void setName(String name) {
            this.name = name;
        }

        public synchronized boolean isTcp() {
            return tcp;
        }

        public synchronized void setTcp(boolean tcp) {
            this.tcp = tcp;
        }

        public synchronized String getIp() {
            return ip;
        }

        public synchronized void setIp(String ip) {
            this.ip = ip;
        }

        public synchronized String getPort() {
            return port;
        }

        public synchronized void setPort(String port) {
            this.port = port;
        }
    }
}
