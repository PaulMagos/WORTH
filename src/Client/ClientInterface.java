package Client;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface ClientInterface extends Remote {
    // Metodo per aggiornare il client su eventuali eventi di login o logout di altri utenti
    void notifyMe(Map<String, String> usrs) throws RemoteException;
    // metodo per ottenere l userName dell'utente sul client
    String getUserName() throws RemoteException;
    // metodo per ottenere il nome del progetto a cui l'utente ha fatto accesso sul client
    String getProjectName() throws RemoteException;
}
