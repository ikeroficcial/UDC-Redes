package es.udc.redes.webserver;

import java.io.*;
import java.util.Properties;

public class GetPropertiesValues {

    String[] propArray = new String[4];
    InputStream inputStream;

    public void getPropertiesValues() throws IOException {

        try {
            Properties prop = new Properties();

            File initialFile = new File("server.properties");
            inputStream = new FileInputStream(initialFile);

            prop.load(inputStream);

            // get the property value and print it out
            propArray[0] = prop.getProperty("PORT");
            propArray[1] = prop.getProperty("DEFAULT_FILE");
            propArray[2] = prop.getProperty("BASE_DIRECTORY");
            propArray[3] = prop.getProperty("ALLOW");

        } catch (Exception e) {
            System.out.println("Exception: " + e);
        } finally {
            if (inputStream != null) inputStream.close();
        }
    }
}
