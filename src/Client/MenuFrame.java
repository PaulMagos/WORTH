package Client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class MenuFrame extends JFrame {
    // Parte superiore e inferiore del frame
    JPanel north = new JPanel();
    JPanel south = new JPanel();
    // Label e Field per inserire il nome del progetto
    JLabel project = new JLabel("Project Name");
    JTextField projectName = new JTextField();
    // Buttons per la creazione e l accesso a un progetto
    JButton create = new JButton("Create Project");
    JButton access = new JButton("Access Project");
    // Menu Bar in cui vengono indicati i progetti a cui si appartiene
    // (non aggiornati al momento, ma al momento del login)
    // E gli utenti online ( lista aggiornata, attraverso la stub di callBack )
    JMenuBar bar;
    JMenu Projects;
    JMenu Users;
    // Istanza del client chiamante
    Client client;

    // Costruttore
    public MenuFrame(Client client, Point location){
        super("Menu");
        this.client = client;
        // Lista dei progetti relativi all utente corrente
        List<String> liste = client.getInfosforUser();
        // Lego gli action listener ai bottoni
        create.addActionListener(new CreateListener());
        access.addActionListener(new AccessListener());
        // Premendo invio l opzione di default è accedere al progetto
        rootPane.setDefaultButton(access);
        // Creo la bar superiore
        bar = new JMenuBar();
        // vi aggiungo i progetti a cui l'utente appartiene
        Projects = new JMenu("Projects");
        if (liste!=null) setProjects(liste);
        // Creo anche la lista di utenti, anche se inizialmente è vuota
        Users = new JMenu("Users");
        bar.add(Projects);
        bar.add(Users);

        // Definisco le varie componenti
        north.setLayout(new GridLayout(3,1));
        north.add(bar);
        north.add(project);
        north.add(projectName);
        south.setLayout(new GridLayout(2,1));
        south.add(create);
        south.add(access);

        getContentPane().add(north, BorderLayout.NORTH);
        getContentPane().add(south, BorderLayout.SOUTH);
        // Imposto la dimensione e la posizione relativa alla posizione del precedente frame
        setSize(300,180);
        setLocation(location);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
    }
    // Dialog per gli eventuali errori
    public void error(String login_error) {
        JOptionPane.showMessageDialog(this, login_error, "Dialog",
                JOptionPane.ERROR_MESSAGE);
    }
    // Listener per il button create
    private class CreateListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String name = projectName.getText();
            if ( name.equals("") ){
                error("Name is empty");
                return;
            }
            try {
                client.createProject(name);
            } catch (IOException remoteException) {
                error(remoteException.toString());
            }
        }
    }
    // Listener per il button access
    private class AccessListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String name = projectName.getText();
            if ( name.equals("") ){
                error("Name is empty");
                return;
            }
            client.accessProject(name);
        }
    }

    // Metodo per aggiunger i nomi del progetti, in una lista alla lista dei progetti nel frame
    public void setProjects(List<String> Projects){
        JMenuItem info;
        for (String i : Projects) {
            info = new JMenuItem(i);
            this.Projects.add(info);
        }
    }
    // Metodo per gestire gli utenti online, se sono online viene aggiornato il loro nome con un badge di "success"
    public void setUsers(Map<String, String> usrs) {
        Users.removeAll();
        StringTokenizer str;
        String name;
        String stat;
        for (String i : usrs.keySet()){
            if (i.equals(client.getUserName())) continue;
            str = new StringTokenizer(i + " " + usrs.get(i));
            name = str.nextToken();
            stat = str.nextToken();
            ImageIcon imageIcon = null;
            try {
                File file = new File("src/online.png");
                if (file.exists()) {
                    imageIcon = new ImageIcon(ImageIO.read(getClass().getResourceAsStream("Client/online.png")));
                }
            } catch (IOException ignored) {}
            // Se l utente è online lo scrivo accanto al suo nome
            JMenuItem us = new JMenuItem((!(imageIcon==null))? name: name + (stat.equals("Online")? " Online": ""));
            us.setIcon((stat.equals("Online"))? imageIcon:null);
            us.setIconTextGap(2);
            Users.add(us);
        }
    }
}
