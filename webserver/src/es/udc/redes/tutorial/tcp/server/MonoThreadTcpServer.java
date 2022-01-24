package es.udc.redes.tutorial.tcp.server;

import java.net.*;
import java.io.*;

/**
 * MonoThread TCP echo server.
 */
public class MonoThreadTcpServer {

    public static void main(String argv[]) {
        if (argv.length != 1) {
            System.err.println("Format: es.udc.redes.tutorial.tcp.server.MonoThreadTcpServer <port>");
            System.exit(-1);
        }
        ServerSocket servsocket = null;
        Socket socket = null;
        try {
            // Create a server socket
            servsocket = new ServerSocket(Integer.parseInt(argv[0]));
            // Set a timeout of 300 secs
            servsocket.setSoTimeout(300000);

            while (true) {
                // Wait for connections
                socket = servsocket.accept();

                // Set the input channel
                BufferedReader sInput = new BufferedReader(new InputStreamReader(
                        socket.getInputStream()));

                // Set the output channel
                PrintWriter sOutput = new PrintWriter(socket.getOutputStream(), true);

                // Receive the client message
                String received = sInput.readLine();
                System.out.println("SERVER: Received " + received);

                // Send response to the client
                sOutput.println(received);
                System.out.println("SERVER: Sending " + received);

                // Close the streams
                sOutput.close();
                sInput.close();

            }
        // Uncomment next catch clause after implementing the logic            
        } catch (SocketTimeoutException e) {
            System.err.println("Nothing received in 300 secs ");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
	        //Close the socket
            try {
                socket.close();
                servsocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
