package es.udc.redes.webserver;

import java.net.*;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class represents each thread created by WebServer.
 * It sends to the client the requested information.
 * @author 386
 */

public class ServerThread extends Thread {

    private Socket socket;
    private String default_file;
    private String base_directory;
    private boolean allow;

    private boolean get = false;
    private boolean head = false;
    private boolean use_default_file = false;
    private BufferedReader sInput;
    private BufferedWriter sOutput;
    private StatusCode statusCode;
    private static final String server = "WebServer_386";
    private static final SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
    private static final SimpleDateFormat formatter2 = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss z");

    /**
     * Builds a ServerThread specifying its socket and, the parameters provided by the file "server.properties"
     * @param s TCP socket from where the requests come.
     * @param default_file String that indicates the file to use for default (provided by "server.properties").
     * @param base_dir String that indicates the base directory to use (provided by "server.properties")
     *                 (Not implemented).
     * @param allow Boolean to implement another "server.properties" function (Not implemented).
     */
    public ServerThread(Socket s, String default_file, String base_dir, boolean allow){
        this.socket = s;
        this.default_file = default_file;
        this.base_directory = base_dir;
        this.allow = allow;
    }

    /**
     * Method that allows the thread to receive requests and process them.
     */
    public void run() {
        this.statusCode = StatusCode.OK;
        try {

            sInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            sOutput = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            String current_line;
            String recieved = "";
            String filename;

            do {
                current_line = sInput.readLine();
                recieved=recieved.concat(current_line + "\n");
            } while (!"".equals(current_line));

            String[] request_array = recieved.split("\n");

            String resp = response(request_array);
            if(use_default_file) filename = default_file;
            else filename = request_array[0].substring(request_array[0].indexOf("/")+1, request_array[0].lastIndexOf(" "));

            sOutput.write(resp);
            if (get) GET(filename, this.statusCode);
            else if (head) HEAD(this.statusCode);
            System.out.println(resp);

            sOutput.close();
            sInput.close();

        } catch (SocketTimeoutException e) {
            System.err.println("Nothing received in 300 secs");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method called by run(), it generates a response from a request and (if necessary) calls GET or HEAD methods to
     * also return the file requested.
     * @param request Array of Strings which contains the full request.
     * @return A String composed by the full response (excepting the body).
     * @throws ParseException Can be thrown when parsing the "if-modified-since" String into a Date.
     * @throws IOException Can be thrown when calling copy, GET, HEAD, write_log or write_x_on_file methods.
     */
    private String response(String[] request) throws ParseException, IOException {

        statusCode = StatusCode.OK;

        String status;
        String header;
        String blank = "\n";
        String clientIP;
        String type;
        String http_method;
        File file;
        int size;
        Date last_modified;

        Date date = new Date();
        String strDate = formatter.format(date);
        String logDate = formatter2.format(date);

        clientIP = getStringIP(request);
        status = statusGenerator(request[0]);

        boolean if_modified_bool = false;
        Date since = new Date();
        for (String s : request) {
            if (s.substring(0, s.indexOf(" ")).equals("If-Modified-Since:")) {
                String strsince = s.substring(s.indexOf(" ") + 1);
                if_modified_bool = true;
                since = formatter.parse(strsince);
                break;
            }
        }

        String resource = request[0].substring(request[0].indexOf("/") + 1, request[0].lastIndexOf(" "));
        if (use_default_file) file = new File(default_file);
        else file = new File(resource);

        if (statusCode == StatusCode.OK) {
            if (file.exists()) {
                last_modified = getlastModification(resource);
                if (if_modified_bool){
                    if (last_modified.before(since)){
                        size = (int) file.length();
                        if (use_default_file) type = getFileType(default_file);
                        else type = getFileType(resource);
                        header = headerGenerator(strDate, last_modified, size, type);
                    } else {
                        statusCode = StatusCode.NOT_MODIFIED;
                        size = (int) file.length();
                        header = headerGenerator(strDate);
                        write_log(request[0], clientIP, logDate, statusCode.getStatus(), size);
                        return status + this.statusCode.getStatus() + header + blank;
                    }

                } else {
                    size = (int) file.length();
                    if (use_default_file) type = getFileType(default_file);
                    else type = getFileType(resource);
                    header = headerGenerator(strDate, last_modified, size, type);
                }

            } else {
                statusCode = StatusCode.NOT_FOUND;
                file = new File("error/error404.html");
                last_modified = getlastModification("error/error404.html");
                header = headerGenerator(strDate, last_modified, (int) file.length(), "text/html");
                write_log(request[0], clientIP, logDate, statusCode.getStatus());
            }

        } else {
            file = new File("error/error400.html");
            last_modified = getlastModification("error/error400.html");
            header = headerGenerator(strDate, last_modified, (int) file.length(), "text/html");
            write_log(request[0], clientIP, logDate, statusCode.getStatus());
        }


        http_method = request[0].substring(0, request[0].indexOf(" "));
        size = (int) file.length();
        switch (http_method) {
            case "GET":
                get = true;
                write_log(request[0], clientIP, logDate, statusCode.getStatus(), size);
                break;

            case "HEAD":
                head = true;
                write_log(request[0], clientIP, logDate, statusCode.getStatus(), size);
                break;

            default:
                statusCode = StatusCode.BAD_REQUEST;
                last_modified = getlastModification("error/error400.html");
                header = headerGenerator(strDate, last_modified, 0, "text/html");
                write_log(request[0], clientIP, logDate, statusCode.getStatus());
        }

        return status + statusCode.getStatus() + header + blank;
    }

    /**
     * It indicates the filetype of the String "file" provided.
     * NOTE: EXTRA CASES ".ico" and ".pdf" added.
     * @param file String with the name of the file.
     * @return String which indicates the filetype of "file".
     */
    private String getFileType(String file) {
        String extension = file.substring(file.lastIndexOf("."));
        String out;
        switch (extension) {
            case ".html":
                out = "text/html";
                break;

            case ".txt":
                out = "text/plain";
                break;

            case ".gif":
                out = "image/gif";
                break;

            case ".png":
                out = "image/png";
                break;
            case ".ico":
                out = "image/ico";
                break;

            case ".pdf":
                out = "document/pdf";
                break;

            default:
                out = "application/octet-stream";

        }
        return out;
    }

    /**
     * Builds a String with the first part of the response's status line (without the statuscode).
     * @param line First line of the request.
     * @return String with the first part of the status line.
     */
    private String statusGenerator(String line) {
        String httpv;
        String[] pieces;

        if ((pieces=line.split(" ")).length == 3) {
            if (pieces[1].equals("/")) use_default_file = true;
            httpv = pieces[2];

        } else {
            statusCode = StatusCode.BAD_REQUEST;
                httpv = "HTTP/1.1";
        }
        return (httpv + " ");
    }

    /**
     * Builds a String with the response's header (IN THE CASE OF ALL STATUSCODES EXCEPTING "304 NOT MODIFIED").
     * @param date String with the response's generation date.
     * @param dateLastMod Date of the last modification of the file that will be sent on response's body.
     * @param contLength Int that indicates the length (in Bytes) of the file.
     * @param contType String that indicates the filetype of the file.
     * @return String with the full response's header.
     */
    private String headerGenerator (String date, Date dateLastMod, int contLength, String contType) {
        String header = ("Date: " + date + "\nServer: " + server + "\n");
        header = header.concat("Last-Modified: " + formatter.format(dateLastMod) + "\n");
        header = header.concat("Content-Length: " + contLength + "\n");
        header = header.concat("Content-Type: " + contType + "\n");
        return header;
    }

    /**
     * Builds a String with the response's header (IN THE CASE OF THE STATUSCODE "304 NOT MODIFIED").
     * @param date String with the response's generation date.
     * @return String with the full response's header.
     */
    private String headerGenerator (String date){
        return ("Date: " + date + "\nServer: " + server + "\n");
    }

    /**
     * Gets the Date of the last modification of the file.
     * @param strfile String of the file's name.
     * @return Date with the last modification date.
     */
    private Date getlastModification(String strfile){
        File file = new File(strfile);
        return new Date(file.lastModified());
    }

    /**
     * Copies a file into a channel.
     * @param strfile String of the file's name.
     * @throws IOException Can be thrown when opening or closing the channels.
     */
    private void copy (String strfile) throws IOException {
        BufferedInputStream input = null;
        BufferedOutputStream output = null;
        File file = new File(strfile);
        sOutput.flush();

        try {
            input = new BufferedInputStream(new FileInputStream(file));
            output = new BufferedOutputStream(socket.getOutputStream());
            int c;
            while ((c = input.read()) != (-1)) {
                output.write(c);
            }

        } finally {
            if (input != null) input.close();
            if (output != null) output.flush();
        }

    }

    /**
     * Selects which file to copy into the response's body depending on the StatusCode.
     * @param strfile String of the name of the file to be copied in the default case.
     * @param statusCode StatusCode of the request.
     * @throws IOException Can be thrown when calling copy method.
     */
    private void GET(String strfile, StatusCode statusCode) throws IOException{
        switch (statusCode){
            case BAD_REQUEST:
                copy("error/error400.html");
                break;

            case FORBIDDEN:
                copy("error/error403.html");
                break;

            case NOT_FOUND:
                copy("error/error404.html");
                break;

            default:
                copy(strfile);
        }
    }

    /**
     * Selects which file to copy into the response's body if StatusCode is different to "200 OK".
     * @param statusCode StatusCode of the request.
     * @throws IOException Can be thrown when calling copy method.
     */
    private void HEAD(StatusCode statusCode) throws IOException {

        switch (statusCode) {

            case BAD_REQUEST:
                copy("error/error400.html");
                break;

            case FORBIDDEN:
                copy("error/error403.html");
                break;

            case NOT_FOUND:
                copy("error/error404.html");
                break;

            default:
        }
    }

    /**
     * Builds the String to copy on access.log file.
     * @param petitionLine String of the first request line.
     * @param ip String of the requester's ip.
     * @param date String of the date of response.
     * @param statusCode String of the StatusCode.
     * @param bytes Int that indicates the size (in Bytes) of the file provided on response's body.
     * @throws IOException Can be thrown when calling write_x_on_file method.
     */
    private void write_log (String petitionLine, String ip, String date, String statusCode, int bytes) throws IOException {
        String string = petitionLine;
        string=string.concat("\n" + ip + "\n" + date + "\n" + statusCode + bytes + "\n\n");
        write_x_on_file(string, "log/access.log");
    }

    /**
     * Builds the String to copy on error.log file.
     * @param petitionLine String of the first request line.
     * @param ip String of the requester's ip.
     * @param date String of the date of response.
     * @param statusCode String of the StatusCode.
     * @throws IOException Can be thrown when calling write_x_on_file method.
     */
    private void write_log (String petitionLine, String ip, String date, String statusCode) throws IOException {
        String string = petitionLine;
        string=string.concat("\n" + ip + "\n" + date + "\n" + statusCode + "\n");
        write_x_on_file(string, "log/error.log");
    }

    /**
     * This method concat the String x on the indicated file's content.
     * @param x String to be written.
     * @param fileName String with the name of the file.
     * @throws IOException Can be thrown when opening or closing the writing channel.
     */
    private void write_x_on_file(String x, String fileName) throws IOException{
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));
        writer.append(x);
        writer.close();
    }

    /**
     * Indicates on a String the ip of the requester.
     * @param request Array of Strings with the full request.
     * @return String of the ip.
     */
    private String getStringIP(String[] request) {
        String clientHostname = "";

        for (String s : request) {
            if (s.substring(0, s.indexOf(" ")).equals("Host:")) {
                clientHostname = s.substring(s.indexOf(" ") + 1);
                break;
            }
        }

        try{
            InetAddress ip = InetAddress.getByName(clientHostname);
            return (ip.getHostAddress());
        }catch (UnknownHostException e){
            return "localhost: 0.0.0.0";
        }
    }
}