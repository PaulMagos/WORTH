import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

class MessageReceiver implements Runnable {
    // Socket sulla quale riceve messaggi
    MulticastSocket sock;
    List<String> messages;
    // Nome dell'utente
    String name;
    // Frame della chat dell'utente name
    ChatFrame frame;
    byte[] buf;
    boolean mode;
    // Costruttore per chat grafica
    MessageReceiver(MulticastSocket sock, String name, ChatFrame frame) {
        this.name = name;
        this.frame = frame;
        this.sock = sock;
        buf = new byte[1024];
        mode = true;
    }
    // Costruttore per chat da terminale
    MessageReceiver(MulticastSocket sock, String name){
        this.name = name;
        this.sock = sock;
        buf = new byte[1024];
        messages = new ArrayList<>();
        mode = false;
    }

    public void run() {
        // Creo il paccketto con il buffer
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        while (Thread.currentThread().isAlive()) {
            try {
                // Resto in attesa di ricezione
                sock.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());
                // Scrivo sul frame della chat il messaggio ricevuto,
                // se è da parte dell'utente stesso lo modifico con you al posto del suo nome
                if(mode) frame.updateChat((received.startsWith(name) ?
                        "you" + received.substring(name.length()) : received));
                else messages.add((received.startsWith(name) ?
                        "you" + received.substring(name.length()) : received));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}


public class ChatClient{
    // Socket per la chat UDP Multicast
    MulticastSocket socket;
    // Message receiver ( Runnable che cicla all'infinito e legge nuovi messaggi)
    MessageReceiver receiver;

    // Porta e address della caht di progetto
    int port;
    InetAddress address;

    // Username del client che invia e riceve
    String userName;

    // Costruttore
    public ChatClient(int chatPort, String name, ChatFrame frame, String host) throws IOException {
        // Creo un InetAddress
        address = InetAddress.getByName(host);
        socket = new MulticastSocket(chatPort);

        // Se il sistema operativo è mac os devo effettuare delle operazioni in più
        // dato che alloca alcuni indirizzi multicast per AirDrop
        if ((System.getProperty("os.name").toLowerCase()).startsWith("mac os"))
            socket.setNetworkInterface(NetworkInterface.getByName("en0"));

        // Join del multicast group
        socket.joinGroup(address);

        this.port = chatPort;
        this.userName = name;

        // Thread del receiver
        receiver = new MessageReceiver(socket, name, frame);
        Thread rec = new Thread(receiver);
        rec.start();
    }

    // Costruttore per chat da terminale (su richiesta)
    public ChatClient(int chatPort, String name, String host) throws IOException {
        // Creo un InetAddress
        address = InetAddress.getByName(host);
        socket = new MulticastSocket(chatPort);

        // Se il sistema operativo è mac os devo effettuare delle operazioni in più
        // dato che alloca alcuni indirizzi multicast per AirDrop
        if ((System.getProperty("os.name").toLowerCase()).startsWith("mac os"))
            socket.setNetworkInterface(NetworkInterface.getByName("en0"));

        // Join del multicast group
        socket.joinGroup(address);

        this.port = chatPort;
        this.userName = name;

        // Thread del receiver
        receiver = new MessageReceiver(socket, name);
        Thread rec = new Thread(receiver);
        rec.start();
    }

    // Methodo per inviare messaggi sul canale udp Multicast
    public void Send(String text) throws IOException {
        byte[] buf = (this.userName + ": " + text).getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, this.address, this.port);
        socket.send(packet);
    }
    // Richiesta di lettura della chat
    public void read() {
        receiver.messages.forEach(System.out::println);
    }
}