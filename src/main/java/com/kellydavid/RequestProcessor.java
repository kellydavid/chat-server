package com.kellydavid;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class RequestProcessor implements Runnable{

    private Socket so;

    /**
     * Constructor for RequestProcessor accepts socket of client.
     * @param so
     */
    public RequestProcessor(Socket so){
        this.so = so;
    }

    public void run()
    {
        try{
            // receive data
            String recvd = new BufferedReader(new InputStreamReader(so.getInputStream())).readLine();
            // process request
            process(recvd);
            // close socket
            so.close();
        }catch(Exception e){
            System.err.println("CS: Error processing request\n");
            e.printStackTrace();
        }
    }

    private void process(String request)
    {
        if(request.startsWith("HELO")) {
            System.out.print("CS: Received HELO request\n");
            heloHandler(request);
        }
        else if(request.startsWith("KILL_SERVICE")){
            System.out.print("CS: Received KILL_SERVICE request\n");
            System.exit(0);
        }else{
            System.out.print("CS: Received unknown request\n");
        }
    }

    private synchronized void heloHandler(String request)
    {
        sendResponse(request +
                "\nIP:" + so.getLocalAddress().getHostAddress() +
                "\nPort:" + so.getLocalPort() +
                "\nStudentID:" + App.STUDENT_ID);
    }

    private void sendResponse(String response)
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