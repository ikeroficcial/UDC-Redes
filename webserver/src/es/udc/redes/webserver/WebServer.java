package es.udc.redes.webserver;

import java.io.IOException;
import java.net.*;

/**
 * This class represents the server.
 * It manages all the connections and creates the needed threads (because it is a multithread server).
 * @author 386
 */

public class WebServer {

    /**
     * Main method. It sets the sockets and creates a ServerThread when it is needed.
     * @param argc Is not used, it is a default parameter.
     * @throws IOException It can throw this exception when it tries to open or close a socket.
     */

    public static void main(String[] argc) throws IOException {
        GetPropertiesValues properties = new GetPropertiesValues();
        properties.getPropertiesValues();

        ServerSocket servsocket = null;
        Socket socket = null;
        es.udc.redes.webserver.ServerThread sthread;
        try {
            servsocket = new ServerSocket(Integer.parseInt(properties.propArray[0]));
            servsocket.setSoTimeout(300000);

            while (true) {
                socket = servsocket.accept();
                sthread = new es.udc.redes.webserver.ServerThread(socket, properties.propArray[1],
                        properties.propArray[2], Boolean.parseBoolean(properties.propArray[3]));
                sthread.start();
            }

        } catch (SocketTimeoutException e) {
            System.err.println("Nothing received in 300 secs");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                socket.close();
                servsocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
}
