import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.rmi.NotBoundException;

public class LoginFrame extends JFrame{
    // Pannello superiore e inferiore del frame
    JPanel north = new JPanel();
    JPanel south = new JPanel();
    // Field per l username e la password
    JTextField user = new JTextField();
    JPasswordField password = new JPasswordField();
    // Buttons per il login e la registrazione
    JButton login = new JButton("Login");
    JButton signup = new JButton("Register");
    // Riferimento al client che ha i stanziato il frame
    Client client;

    public LoginFrame(Client client){
        this.setName("Login");
        this.client = client;
        // Lego gli action listener ai bottoni
        login.addActionListener(new LoginListener());
        signup.addActionListener(new RegisterListener());
        // Definisco il bottone di default, per poter premere invio e loggarsi direttamente
        rootPane.setDefaultButton(login);

        // Definisco le due parti del frame con i vari componenti
        north.setLayout(new GridLayout(4,1));
        north.add((new JLabel("username",SwingConstants.CENTER)));
        north.add(user);
        north.add(new JLabel("password",SwingConstants.CENTER));
        north.add(password);
        south.setLayout(new GridLayout(2, 3));
        south.add(login);
        south.add(signup);

        getContentPane().add(north, BorderLayout.NORTH);
        getContentPane().add(south, BorderLayout.SOUTH);
        // Definisco dimensione e posizione del frame
        setSize(300,200);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
    }
    // Action listener per il bottone di registrazione
    public class RegisterListener implements ActionListener{
        public void actionPerformed(ActionEvent event){
            String name = user.getText();
            String pwd = new String(password.getPassword());
            if ( name.equals("") ){
                error("Name is empty");
                return;
            }
            if ( pwd.equals("") ){
                error("password is empty");
                return;
            }
            if ( pwd.length()<4 ){
                error("password must be at least 4 characters long");
                return;
            }
            try {
                client.ServerReg(name, pwd);
            } catch (IOException | NullPointerException | NotBoundException e) {
                error(e.toString());
            }
        }
    }
    // Action listener per il bottone di login
    public class LoginListener implements ActionListener{
        public void actionPerformed(ActionEvent event){
            String name = user.getText();
            String pwd = new String(password.getPassword());
            if ( name.equals("") ){
                error("Name is empty");
                return;
            }
            if ( pwd.equals("") ){
                error("password is empty");
                return;
            }
            if ( pwd.length()<4 ){
                error("password must be at least 4 characters long");
                return;
            }
            client.ServerLog(name, pwd);
        }
    }
    // Dialog panel per gli errori
    protected void error(String error){
        JOptionPane.showMessageDialog(this, error, "Error",JOptionPane.ERROR_MESSAGE);
    }

}

