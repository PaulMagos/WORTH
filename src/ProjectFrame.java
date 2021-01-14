import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class ProjectFrame extends JFrame {

    // Tutti i panel vari, le text field, i bottoni e cosi via

    // Pannello della parte di sinistra e destra del frame
    JPanel Left = new JPanel();
    JPanel Right = new JPanel();
    // Pannello della parte di sinistra superiore e inferiore
    JPanel Leftnorth = new JPanel();
    JPanel Leftsouth = new JPanel();

    // Field per il nome della card per le 4 possibili selezioni
    JTextField cardField1;
    JTextField cardField2;
    JTextField cardField3;
    JTextField cardField4;

    // Field per la descrizione della card per il pannello di creazione
    JTextField cardDescription;

    // Lista dei movimenti per le card
    JComboBox<String> old_list = new JComboBox<>(new String[]{"TODO", "INPROGRESS", "TOBEREVISITED", "DONE"});
    JComboBox<String> new_list = new JComboBox<>(new String[]{"INPROGRESS", "TOBEREVISITED", "DONE"});

    // Bottoni per la member list e la card list
    JButton MemberList = new JButton("Member list");
    JButton CardList = new JButton("Card List");

    // Client che ha instanziato il frame e il suo corrispondente chatframe
    Client client;
    ChatFrame chatFrame;

    // Costruttore
    public ProjectFrame(Client client, Point location) {
        super("ProjectMenu");
        this.client = client;
        chatFrame = new ChatFrame(this.client, location);

        // Left Panel
        JLabel project = new JLabel( "Project: " + this.client.getProjectName());
        project.setHorizontalAlignment(JLabel.CENTER);
        JButton cancelPrj = new JButton("Delete");
        cancelPrj.addActionListener(new DeleteProj());
        JPanel pad = new JPanel(new GridLayout(2,1));
        pad.add(project);
        pad.add(cancelPrj);
        Leftnorth.setLayout(new BorderLayout());
        Leftnorth.add(pad, BorderLayout.NORTH);
        JLabel n = new JLabel("User: " + this.client.getUserName());
        n.setHorizontalAlignment(JLabel.CENTER);
        JButton log_out = new JButton("Log Out");
        log_out.addActionListener(new logOut());
        JPanel r = new JPanel(new GridLayout(2,1));
        r.add(n);
        r.add(log_out);
        Leftsouth.setLayout(new BorderLayout());
        Leftsouth.add(r, BorderLayout.SOUTH);
        Left.setBorder(new MatteBorder(0,0,0,1,Color.BLACK));
        Left.setLayout(new GridLayout(2,1));
        Left.add(Leftnorth, BorderLayout.NORTH);
        Left.add(Leftsouth, BorderLayout.SOUTH);
        Left.setSize(new Dimension(150, 400));

        // Right Panel con il menu per le 4 selezioni possibili per le card
        JTabbedPane tab = new JTabbedPane(JTabbedPane.TOP);
        JComponent create = makeTextPanel("create");
        JComponent show = makeTextPanel("show");
        JComponent move = makeTextPanel("move");
        JComponent history = makeTextPanel("history");
        tab.addTab("Add Card",null,create, "does nothing");
        tab.addTab("Show Card",null,show, "does nothing");
        tab.addTab("Move Card",null,move, "does nothing");
        tab.addTab("Show Card History",null,history, "does nothing");

        Right.add(tab, BorderLayout.NORTH);
        MemberList.addActionListener(new MemberListView());
        CardList.addActionListener(new CardListView());
        Right.add(MemberList);
        Right.add(CardList);
        this.getContentPane().add(Left, BorderLayout.WEST);
        this.getContentPane().add(Right);


        // Imposto la dimensione e il posizionamento della finestra
        setSize(600,300);
        setLocation(location);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        setVisible(true);

    }

    // Metodo per creare un panel ( perché alcuni hanno componenti comuni )
    protected JComponent makeTextPanel(String text) {
        // Panel declaration and partition in three parts (the left and the right part are not necessary)
        JComponent panel = new JPanel(new GridLayout(1,3));
        JPanel left = new JPanel();
        JPanel center = new JPanel(new GridLayout());
        JPanel right =  new JPanel();

        JPanel northSide = new JPanel(new GridLayout(4,1));
        JPanel southSide = new JPanel();
        southSide.setSize(10,30);

        // Label for card field for every panel
        JPanel CardNameSide = new JPanel(new GridLayout(1,2));
        JLabel cardName = new JLabel("Card Name");
        CardNameSide.add(cardName);


        switch (text){
            case "create":
                // Description label and Description TextField for create panel
                JPanel DescriptionSide = new JPanel(new GridLayout(1,2));
                JLabel desc = new JLabel("Description");
                cardDescription = new JTextField();
                DescriptionSide.add(desc);
                DescriptionSide.add(cardDescription);

                // Create Button
                JButton createCardButton = new JButton("Create");
                createCardButton.addActionListener(new CreateCard());
                // Field per il nome
                cardField1 = new JTextField();
                CardNameSide.add(cardField1);

                northSide.add(CardNameSide, BorderLayout.NORTH);
                northSide.add(DescriptionSide);
                northSide.add(createCardButton);
                break;
            case "show":
                // Field per il nome della card
                cardField2 = new JTextField();
                CardNameSide.add(cardField2);
                // Button per vedere le informazioni
                JButton showCard = new JButton("Show");
                // Lego l action listener al button
                showCard.addActionListener(new ShowCard());
                northSide.add(CardNameSide, BorderLayout.NORTH);
                northSide.add(showCard);
                break;
            case "move":
                // Field per il nome della card
                cardField3 = new JTextField();
                // Button per inviare la richiesta di move
                JButton move = new JButton("Move");
                // Lego l action listener al button
                move.addActionListener(new MoveCard());
                CardNameSide.add(cardField3);
                northSide.add(CardNameSide);
                // Aggiungo le liste da e a ( per decidere dove spostare la card )
                northSide.add(old_list);
                northSide.add(new_list);
                northSide.add(move);
                break;
            case "history":
                // Field per il nome della card
                cardField4 = new JTextField();
                // Button per richiedere lo storico
                JButton history = new JButton("Show History");
                // Lego l action listener al button
                history.addActionListener(new ShowCardHistory());
                CardNameSide.add(cardField4);
                northSide.add(CardNameSide, BorderLayout.NORTH);
                northSide.add(history);
                break;
        }

        center.add(northSide);
        //center.add(southSide);

        panel.add(left);
        panel.add(center);
        panel.add(right);
        return panel;
    }
    // Error dialog per eventuali errori
    public void error(String s) {
        JOptionPane.showMessageDialog(this, s, "Error", JOptionPane.ERROR_MESSAGE);
    }
    // Dialog per confermare con successo una operazione
    public void ok(String s) {
        JOptionPane.showMessageDialog(this, s, "Ok",
                JOptionPane.PLAIN_MESSAGE);
    }
    // Metodo per ritornare il chat frame relativo a questo projectFrame
    public ChatFrame getChatFrame() {
        return this.chatFrame;
    }
    // Action listener per la create card
    public class CreateCard implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            String name = cardField1.getText();
            String description = cardDescription.getText();
            if ( name.equals("") ){
                error("Name is empty");
                return;
            }
            if ( description.equals("") ) {
                error("description is empty");
                return;
            }
            client.createCard(name , description);
        }
    }
    // Action listener per la show card
    public class ShowCard implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e) {
            String name = cardField2.getText();
            if ( name.equals("") ){
                error("Name is empty");
                return;
            }
            client.showCard(name);
        }
    }
    // Action listener per la Move Card
    private class MoveCard implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String name = cardField3.getText();
            if ( name.equals("") ){
                error("Name is empty");
                return;
            }
            String oldl = (String) old_list.getSelectedItem();
            String newl = (String) new_list.getSelectedItem();
            assert oldl != null;
            assert newl != null;
            if(!((oldl.equals("TODO") || oldl.equals("INPROGRESS") || oldl.equals("TOBEREVISITED") || oldl.equals("DONE"))
                    &&  (newl.equals("INPROGRESS") || newl.equals("TOBEREVISITED") || newl.equals("DONE")))){
                error("LIST ERROR");
                return;
            }
            client.MoveCard(name, oldl, newl);
        }
    }
    // Action listener per la card history
    private class ShowCardHistory implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String name = cardField4.getText();
            if ( name.equals("") ){
                error("Name is empty");
                return;
            }
            client.showCardHistory(name);
        }
    }
    // Dialog per verificare che l'utente sia sicuro di una certa azione
    private boolean sureDialog(String e){
        return JOptionPane.showConfirmDialog(this, "Sei sicuro?", e,
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }
    // Dialog Panel per vedere la lista delle card
    public void sPanel(List<String> info){
        StringBuilder tm = new StringBuilder();
        for (String i: info) tm.append(i).append("\n");
        JTextArea tmp = new JTextArea( tm.toString());
        JOptionPane.showConfirmDialog(this, new JScrollPane(tmp), "Storico", JOptionPane.DEFAULT_OPTION);
    }
    // Dialog Panel per vedere la lista degli utenti membri
    public void sPanel(List<String> info, String name){
        StringBuilder tm = new StringBuilder();
        for (String i: info) tm.append(i).append("\n");
        JTextArea tmp = new JTextArea( tm.toString());
        JOptionPane.showConfirmDialog(this, new JScrollPane(tmp), name, JOptionPane.DEFAULT_OPTION);
    }
    // Action listener per la delete del progetto
    private class DeleteProj implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e){
            if(sureDialog("Delete Project")) {
                client.deleteProject();
            }
        }
    }
    // Action listener per la logout dell utente
    private class logOut implements ActionListener{
        @Override
        public void actionPerformed(ActionEvent e){
            if(sureDialog("Log Out")) {
                client.logOut();
            }
        }
    }
    // Action listener per vedere i membri del progetto
    private class MemberListView implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            client.MembersList();
        }
    }
    // Action listener per vedere le card relative al progetto
    private class CardListView implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            client.CardsList();
        }
    }
}
