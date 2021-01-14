package WorthServer;

import MyExceptions.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TCPHandler extends Thread {
    // Port for connection TODO
    private static final int TCP_LOGIN_PORT = 9000;

    // ServerSocketChannel e Selector per poter selezionare tra i vari client che si collegano
    private final ServerSocketChannel serverSocket;
    private final Selector selector;
    // Istanza del "database" per poter effettuare le operazioni
    private final DataBase serverToWork;
    // Buffer per i messaggi
    private final ByteBuffer buf = ByteBuffer.allocate(256);

    public TCPHandler(DataBase server) throws IOException {
        this.serverToWork = server;
        // Apro il ServerSocketChannel, la porta a cui offrire il servizio definisco il selector, con la preimpostazione
        // su accept (accept connections)
        this.serverSocket =  ServerSocketChannel.open();
        this.serverSocket.bind(new InetSocketAddress(TCP_LOGIN_PORT));
        serverSocket.configureBlocking(false); // server socket non bloccante
        this.selector = Selector.open();
        this.serverSocket.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server TCP avviato su localhost alla porta " + TCP_LOGIN_PORT);
    }

    @Override
    public void run(){
        try {
            while (this.serverSocket.isOpen()) {
                selector.select();
                Iterator<SelectionKey> itr = this.selector.selectedKeys().iterator();

                while (itr.hasNext()) {  // Itero tra le key che ho per verificare se sono accettabili,
                                            // scrivibili o leggibili
                    SelectionKey key = itr.next();
                    itr.remove();
                    try {
                        if (key.isAcceptable()) this.handleAccept(key);
                        if (key.isReadable()) this.handleRead(key);
                        if (key.isWritable()) this.handleWrite(key);
                    }catch (IOException e){
                        // Se un utente ha chiuso il frame senza fare il log out, verifico se era loggato e lo faccio io
                        String err = (new String(((ByteBuffer) key.attachment()).array()));
                        if(err.startsWith("session:")) {
                            StringTokenizer j = new StringTokenizer(err);
                            String po = j.nextToken();
                            serverToWork.logout(po.substring(8));
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
}

    public void handleAccept(SelectionKey key) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();  // Apro la connessione
        channel.configureBlocking(false);
        // Registro la key su readable
        channel.register(selector, SelectionKey.OP_READ, ByteBuffer.wrap("".getBytes(StandardCharsets.UTF_8)));
        // Non necessario, informo che ho accettato una nuova connessione
        System.out.println("Server: Accettata nuova connessione dal client: " +
                channel.getRemoteAddress().toString().substring(1,
                        channel.getRemoteAddress().toString().indexOf(":")) +
                " porta " + channel.getRemoteAddress().toString().
                substring(channel.getRemoteAddress().toString().indexOf(":")+1));
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();  // Casto la key
        StringBuilder sb = new StringBuilder();
        buf.clear();    // Svuoto il buffer ( dichiarato in precedenza )
        int read;  // controllo che ci sia ancora qualcosa inviato dalla key, e lo leggo
        while( (read = channel.read(buf)) > 0 ) {
            buf.flip();
            byte[] bytes = new byte[buf.limit()];
            buf.get(bytes);
            sb.append(new String(bytes));
            buf.clear();
        }

        String msg;
        if(read<0){
            msg = key.attachment() + "left\n";
            channel.close();
        }else {
            // Il messaggio ricevuto lo parso e lo suddivido
            msg = sb.toString();
            String attach = (new String(((ByteBuffer) key.attachment()).array()));

            // Divido gli attachments in una lista di stringhe
            List<String> listofattach = new ArrayList<>(Arrays.asList(attach.split(" ")));
            // Divido i messaggi in entrata in una lista di stringhe
            List<String> list = new ArrayList<>(Arrays.asList(sb.toString().split(" ")));

            // Se l attachment della key inizia con "session:" significa che l'utente ha già fatto il login
            if(!attach.startsWith("session:")){
                try {
                    // Se non è loggato, effettuo il login e informo l'utente della riuscita o meno
                    if(this.serverToWork.login(list.get(1), list.get(0))) { msg = "session:" + list.get(1); }
                    else msg = "err";
                } catch (WrongPasswordException e) {
                    msg = "WrongPassword";
                } catch (UserOnlineException e) {
                    msg = "UserAlreadyOnline";
                }
            }else{

                // Pulisco gli attachment da eventuali residui dovuti a errori sulle richieste
                // ( devo informare il client )
                if(listofattach.size()!=1){
                    if(!listofattach.get(1).startsWith("project")) {
                        attach = listofattach.get(0);
                        msg = listofattach.get(0);
                    }else {
                        if (listofattach.size()!=2){
                                attach = listofattach.get(0) + " " + listofattach.get(1);
                                msg = listofattach.get(0) + " " + listofattach.get(1);
                        }
                        else if (!msg.startsWith("session")){
                            msg = listofattach.get(0) + " " + listofattach.get(1);
                        }
                    }
                }
                // Verifico il tipo di richiesta del client
                switch(list.get(0)) {
                    // L'utente ha fatto già l accesso, deve o creare un progetto o accedervi
                    case "ACCESSPROJECT":
                        try {
                            // Tento l accesso a un determinato progetto da lui richiesto e lo informo della riuscita
                            if (this.serverToWork.accessProject(listofattach.get(0).substring(8), list.get(1))) {
                                msg = attach + " project:" + list.get(1);
                            } else msg = msg + " err";
                        } catch (NotExistingProjectException e) {
                            msg = attach + " NotExistsProject";
                        } catch (UserNotMemberException e) {
                            msg = attach + " UserNotMemberOfThisProject";
                        }
                        break;
                    case "CREATEPROJECT":
                        try {
                            // Tento la creazione di un determinato progetto e lo informo della riuscita dell operazione
                            if (this.serverToWork.createProject(list.get(1), listofattach.get(0).substring(8))) {
                                msg = attach;
                            } else msg = attach + " err";
                        } catch (ProjectAlreadyExistsException e) {
                            msg = attach + " ProjectAlreadyExists";
                        }
                        break;
                    // Il client sta richiedendo informazioni sui progetti di cui fa parte l'utente ( Menu frame )
                    case "GETINFOS":
                        String outinfo = this.serverToWork.listProjects(listofattach.get(0).substring(8));
                        if (outinfo!= null && !outinfo.equals(""))
                            msg = attach + " " + outinfo;
                        else msg = attach;
                        break;

                    // L'utente ha già effettuato l accesso e ha gia scelto o creato un progetto,
                    // verifico se vuole fare qualcosa inerente al progetto, o uscire
                    case "project:":
                        if(listofattach.get(1).equals("project:"+list.get(1))){
                            switch (list.get(2)){
                                // Se vuole fare operazioni sulle card
                                case "CREATECARD":
                                    try {
                                        // Provo a creare la card e informo il client della riuscita dell operazione
                                        if(this.serverToWork.addCard(list.get(1), list.get(3), list.get(4))) msg = attach;
                                    } catch (CardExistsException e) {
                                        // La card non esiste
                                        msg = attach + " CardExists";
                                    } catch (NotExistingProjectException e) {
                                        // Il progetto non esiste (evento non possibile secondo un utilizzo consono)
                                        msg = attach + " ProjectNotExists";
                                    }
                                    break;
                                case "MOVECARD":
                                    try {
                                        // Provo a spostare la card e informo il client della riuscita dell operazione
                                        if (this.serverToWork.moveCard(list.get(1),list.get(3),list.get(4),list.get(5))){
                                            msg = attach;
                                        }
                                    } catch (InvalidStatusException | CardMoveException e) {
                                        // Non posso muovere la card alla lista specificata, vincoli
                                        msg = attach + " CantMoveCardFrom"+list.get(4)+"To"+list.get(5);
                                    } catch (NoSuchCardException e) {
                                        // La Card richiesta non esiste
                                        msg = attach + " CardNotExists";
                                    } catch (CardNotInListException e) {
                                        // La card non è nella lista definita dall utente (client)
                                        msg = attach + " CardNotIn"+list.get(4);
                                    }
                                    break;
                                case "SHOWCARD":
                                    try {
                                        // Richiedo le info e informo il client dell esito dell operazione e dei dati
                                        msg = attach + " " + this.serverToWork.showCard(list.get(1) , list.get(3));
                                    } catch (NoSuchCardException e) {
                                        // La card richiesta non esiste
                                        msg = attach + " CardNotExists";
                                    }
                                    break;
                                case "HISTORY":
                                    try {
                                        // Provo ad ottenere lo storico e informo il client dell esito dell operazione
                                        // e dei dati
                                        msg = attach + " " + this.serverToWork.getCardHistory(list.get(1), list.get(3));
                                    } catch (NoSuchCardException e) {
                                        // La card richiesta non esiste
                                        msg = attach + " CardNotExists";
                                    }
                                    break;
                                case "DELETEPROJ":
                                    try {
                                        // Richiedo l'eliminazione del progetto secondo richiesta del client
                                        if(this.serverToWork.cancelProject(list.get(1),
                                                listofattach.get(0).substring(8))) msg = attach;
                                    } catch (ProjectNotDoneException e) {
                                        // Il progetto ha ancora card non finite
                                        msg = attach + " ProjectNotFinished";
                                    }
                                    break;
                                case "LOGOUT":
                                    // Effettuo il logout
                                    if(this.serverToWork.logout(list.get(3))) msg = "close";
                                    break;
                                case "ADDMEMBER":
                                    try {
                                        // Aggiungo il membro indicato dal client al progetto
                                        if (this.serverToWork.addMember(list.get(1), list.get(3))){
                                            msg = attach;
                                        }
                                    } catch (AlreadyMemberException e) {
                                        // L'utente desiderato fa già parte del progetto
                                        msg = attach + " AlreadyMember";
                                    } catch (UserNotExistsException e) {
                                        // L'utente desiderato non esiste
                                        msg = attach + " UserNotExists";
                                    }
                                    break;
                                case "GETCHAT":
                                    // Il client richiede l indirizzo della chat multicast relativa al progetto
                                    String ip = this.serverToWork.getChat(list.get(3));
                                    if(ip!=null){
                                        msg = attach + " " + ip;
                                    }else msg = attach;
                                    break;
                                case "MLIST":
                                    // Il client richiede la lista dei membri del progetto corrente
                                    String outM = this.serverToWork.showMembers(list.get(1));
                                    if (outM!= null && !outM.equals(""))
                                        msg = attach + " " + outM;
                                    else msg = attach;
                                    break;
                                case "CLIST":
                                    // Il client richiede la lista delle card del progetto corrente
                                    String outC = this.serverToWork.showCards(list.get(1));
                                    if (outC!= null && !outC.equals(""))
                                        msg = attach + " " + outC;
                                    else msg = attach;
                                    break;
                            }
                        }else {
                            this.serverSocket.close();
                        }
                        break;
                }
            }
        }
        // Registro il messaggio in base alla richiesta del client da gestire e la rendo scrivibile
        channel.register(selector, SelectionKey.OP_WRITE, ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8)));
    }

    private void handleWrite(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();  // Casto la key
        ByteBuffer myBuffer = (ByteBuffer) key.attachment(); // Prendo il testo dagli attachments
        channel.write(myBuffer); // Lo scrivo e controllo che sia stato preso tutto
        if (myBuffer.hasRemaining()) return;
        channel.register(selector, SelectionKey.OP_READ, key.attachment()); // rendo di nuovo disponibile alla lettura
    }
}
