import WorthServer.DataBase;

import java.io.IOException;
import java.rmi.AlreadyBoundException;

public class MainServer {

    public static void main(String[] args) throws IOException, AlreadyBoundException {
        // check argomenti
        String filesLocation = "../server";
        try {
            filesLocation = args[0];
        }catch (ArrayIndexOutOfBoundsException e){
            System.out.println("Usage: java -cp [jars] MainServer [optional fileLocation of server]");
            System.out.println("Nessun path immesso uso \"../server\"");
        }

        DataBase db = new DataBase(filesLocation);
    }
}
