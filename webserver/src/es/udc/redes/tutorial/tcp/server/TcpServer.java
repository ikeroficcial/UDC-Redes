package es.udc.redes.tutorial.tcp.server;
import java.io.IOException;
import java.net.*;

/** Multithread TCP echo server. */

public class TcpServer {

  public static void main(String argv[]) {
    if (argv.length != 1) {
      System.err.println("Format: es.udc.redes.tutorial.tcp.server.TcpServer <port>");
      System.exit(-1);
    }
    ServerSocket servsocket = null;
    Socket socket = null;
    ServerThread sthread = null;
    try {
      // Create a server socket
      servsocket = new ServerSocket(Integer.parseInt(argv[0]));

      // Set a timeout of 300 secs
      servsocket.setSoTimeout(300000);

      while (true) {
        // Wait for connections
        socket = servsocket.accept();

        // Create a ServerThread object, with the new connection as parameter
        sthread = new ServerThread(socket);

        // Initiate thread using the start() method
        sthread.start();

      }
    } catch (SocketTimeoutException e) {
      System.err.println("Nothing received in 300 secs");
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
