import MyExceptions.*;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RMIWORTHServer extends Remote {
    // Metodo per registrarsi al server
    boolean register(String name, String password) throws IOException, UserAlreadyPresentException;
    // Metodi per callback, register per rendersi disponibili alla callback,
    // unregister per definire la non più disponibilità
    void registerForCallback(ClientInterface stub) throws RemoteException;
    void unregisterForCallback(ClientInterface stub) throws RemoteException;
}
