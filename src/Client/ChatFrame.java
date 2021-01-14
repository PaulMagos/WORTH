package Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static java.lang.System.exit;

public class ChatFrame extends JFrame {

    // Tutti i vari pannelli, label, il textfield per inviare,
    // la textarea per leggere ed il bottone per inviare e per aggiungere membri
    private JPanel north = new JPanel();
    private JPanel center = new JPanel();
    private JPanel south = new JPanel();
    JTextField n = new JTextField();
    JButton add = new JButton("Add");
    JTextArea chat;
    JTextField sendMsg;
    JButton send = new JButton("send");
    JScrollPane scrollPane;
    Client client;

    // Costruttore
    public ChatFrame(Client client, Point location){
        super("Chat");
        this.client = client;
        north.setLayout(new GridLayout(1,2));
        JPanel addMember = new JPanel(new GridLayout(3,1));
        JLabel name = new JLabel("New member name: ");
        add.addActionListener(new addMember());
        addMember.add(name);
        addMember.add(n);
        addMember.add(add);
        north.add(new JLabel("Chat: "+ client.getProjectName()));
        north.add(addMember);

        center.setLayout(new GridLayout(1,1));
        chat = new JTextArea();
        scrollPane = new JScrollPane(chat);

        chat.setEditable(false);
        center.add(scrollPane);

        south.setLayout(new GridLayout(2,1));
        sendMsg = new JTextField();
        send.addActionListener(new SendListener());
        south.add(sendMsg);
        south.add(send);

        // Setto il tipo di layout e aggiungo i miei pannelli al frame
        getContentPane().setLayout(new GridLayout(3,1));
        getContentPane().add(north, BorderLayout.NORTH);
        getContentPane().add(center, BorderLayout.CENTER);
        getContentPane().add(south, BorderLayout.SOUTH);

        // Imposto la posizione sullo schermo e rendo visibile il frame
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(300,300);
        setLocation((int) location.getX()-300, (int) location.getY());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
    }

    // Action listener per verificare azioni su un bottone
    public class addMember implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String name = n.getText();
            if ( name.equals("") ){
                error("Name is empty");
                return;
            }
            client.addMember(name);
        }
    }

    // Error Dialog per informare di eventuali errori
    public void error(String s) {
        JOptionPane.showMessageDialog(new JFrame(), s, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // Metodo per aggiungere i messaggi alla text area uno sotto all'altro
    // Per simulare il funzionamento di una chat
    public void updateChat(String s){
        chat.append(s);
        chat.append("\n");
        chat.setCaretPosition(chat.getDocument().getLength());
        if (s.startsWith("SERVER: PROJECT DELETED") && !s.endsWith(client.getUserName())) {
            error(s);
            exit(0);
        }
    }


    // Action listener per verificare azioni su un bottone
    private class SendListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String  txt = sendMsg.getText();
            sendMsg.setText(null);
            if (txt.equals("")) return;
            try {
                client.sendMessage(txt);
            } catch (Exception exception) {
                System.out.println("ERRORE QUA");
            }
        }
    }
}
