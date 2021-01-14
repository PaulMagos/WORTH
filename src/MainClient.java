import com.formdev.flatlaf.FlatDarculaLaf;

import javax.swing.*;
import java.io.IOException;

public class MainClient {
    public static void main (String[] args){
        // check argomenti
        String ip = "localhost";
        String interfaceC = "gui";
        try {
            ip = args[0];
        }catch (ArrayIndexOutOfBoundsException e){
            System.out.println("Usage: java -cp [jars] MainClassClient [optional ip of server] "
                    + "[optional interface type {termina, gui}]");
            System.out.println("Nessun ip immesso uso localhost (server su stesso pc)");
        }
        try {
            interfaceC = args[1];
        }catch (ArrayIndexOutOfBoundsException e){
            System.out.println("Nessun tipo di interfaccia inserito [terminale, gui] uso gui");
        }

        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (UnsupportedLookAndFeelException e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                    | UnsupportedLookAndFeelException classNotFoundException) {
                System.out.println("Using Java theme");
            }
        }

        try {
            Client client = new Client(ip, interfaceC);
            Thread t = new Thread(client);
            t.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
