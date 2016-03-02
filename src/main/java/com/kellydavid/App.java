package com.kellydavid;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class App
{
    public static final String STUDENT_ID = "e19b9a3807b5cfaa4db33fd30468ef249bb86ccc16c08efbed08f3fb6959e346";
    public static final int CONNECTION_POOL_SIZE = 10;

    private static ServerSocket ss;
    private static ThreadPoolExecutor requestThreads;

    public static void main( String[] args )
    {
        // Get hostname and port number
        if(args.length != 2){
            System.out.println("CS: Must use arguments <hostname> <port-number>\n");
            System.exit(-1);
        }
        String hostname = args[0];
        int portNumber = Integer.parseInt(args[1]);

        initialiseServerSocket(hostname, portNumber);
        requestThreads = new ThreadPoolExecutor(CONNECTION_POOL_SIZE, CONNECTION_POOL_SIZE, 1000,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

        // listen for incoming requests
        listen();
    }

    private static void initialiseServerSocket(String hostname, int portNumber)
    {
        try {
            // Create server socket
            ss = new ServerSocket();
            ss.setReuseAddress(true);
            ss.bind(new InetSocketAddress(hostname, portNumber));
        } catch (Exception e) {
            System.err.println("CS: Error creating socket.\n");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static void listen()
    {
        try {
            while(true){
                System.out.print("CS: Listening for connections...\n");
                // Setup a new Connection thread when a new client connects.
                Socket so = ss.accept();
                requestThreads.execute(new RequestProcessor(so));
            }
        } catch (Exception e) {
            System.err.println("CS: Error while listening for connections.\n");
            e.printStackTrace();
            System.exit(-1);
        }
    }
}

