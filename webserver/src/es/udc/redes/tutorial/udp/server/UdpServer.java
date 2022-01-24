package es.udc.redes.tutorial.udp.server;

import java.net.*;

/**
 * Implements a UDP echo sqerver.
 */
public class UdpServer {

    public static void main(String argv[]) {
        if (argv.length != 1) {
            System.err.println("Format: es.udc.redes.tutorial.udp.server.UdpServer <port_number>");
            System.exit(-1);
        }
        DatagramSocket sDatagram = null;
        try {
            // Create a server socket
            sDatagram = new DatagramSocket(Integer.parseInt(argv[0]));
            // Set timeout to 300 secs
            sDatagram.setSoTimeout(300000);

            while (true) {
                // Prepare datagram for reception
                byte array[] = new byte[1024];
                DatagramPacket dgramRec = new DatagramPacket(array, array.length);

                // Receive the message
                sDatagram.receive(dgramRec);
                System.out.println("SERVER: Received "
                        + new String(dgramRec.getData(), 0, dgramRec.getLength())
                        + " from " + dgramRec.getAddress().toString() + ":"
                        + dgramRec.getPort());
                String message = new String(dgramRec.getData(), 0, dgramRec.getLength());

                // Prepare datagram to send response
                DatagramPacket dgramSent = new DatagramPacket(message.getBytes(),
                        message.getBytes().length, dgramRec.getAddress(), dgramRec.getPort());

                // Send response
                sDatagram.send(dgramSent);
                System.out.println("SERVER: Sending "
                        + new String(dgramSent.getData()) + " to "
                        + dgramSent.getAddress().toString() + ":"
                        + dgramSent.getPort());

            }

            // Uncomment next catch clause after implementing the logic
        } catch (SocketTimeoutException e) {
            System.err.println("No requests received in 300 secs ");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close the socket
            sDatagram.close();
        }

    }
}
