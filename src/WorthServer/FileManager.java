package WorthServer;

import WorthServer.Project.Card;
import WorthServer.Project.Project;
import WorthServer.Project.ProjectImpl;
import WorthServer.User.Account;
import WorthServer.User.User;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.System.exit;

public class FileManager {
    private static final String Users = "/Users";
    private static final String Projects = "/Projects";

    private File serverPath;
    private File usersPath;
    private File projectsPath;
    private ObjectMapper objectMapper;

    public FileManager(String serverPath){
        this.serverPath = new File(serverPath);
        this.usersPath = new File(serverPath + Users);
        this.projectsPath = new File(serverPath + Projects);
        this.objectMapper = new ObjectMapper();
        if (!existsServerPath()) {
            if(!createServerPath()){
                System.out.println("Server Path Create Error");
                exit(0);
            }
        }
    }

    // Metodo per verificare l'esistenza della cartella server
    private boolean existsServerPath(){ return this.serverPath.exists(); }

    // Metodo per creare la cartella server
    private boolean createServerPath(){
        return this.serverPath.mkdir() && createProjectsPath() && createUsersPath();
    }

    // Metodo per verificare l'esistenza della cartella Users (this.serverPath + "/Users")
    private boolean existsUsersPath(){ return this.usersPath.exists(); }
    // Metodo per creare la cartella Users (this.serverPath + "/Users")
    private boolean createUsersPath(){ return this.usersPath.mkdir(); }
    // Metodo per verificare l'esistenza della cartella Projects (this.serverPath + "/Projects")
    private boolean existsProjectsPath(){ return this.projectsPath.exists(); }
    // Metodo per creare la cartella Projects (this.serverPath + "/Projects")
    private boolean createProjectsPath(){ return this.projectsPath.mkdir(); }

    // Metodo per creare la cartella delle card del progetto
    private void createProjectCardsPath(File cards){
        if (!cards.exists()) {
            final boolean mkdir = cards.mkdir();
        }
    }

    // Getter for Projects (and relative cards) and Users
    public List<User> getUsers() throws IOException {
        List<User> usersList = new ArrayList<>();
        if (this.usersPath.isDirectory()){
            String[] paths = this.usersPath.list();
            if(paths != null){
                Arrays.sort(paths);
                File UserFile;
                for(String userName: paths){
                    UserFile = new File(usersPath + "/" +  userName);
                    usersList.add(objectMapper.readValue(UserFile, Account.class));
                }
            }
        }
        return usersList;
    }
    public List<Project> getProjects() throws IOException {
        List<Project> projectsList = new ArrayList<>();
        if (this.projectsPath.isDirectory()){
            String[] paths = this.projectsPath.list();
            if(paths != null){
                File ProjectFile;
                File cardsFolderPath;
                Project currentProject;
                List<Card> cardsForCurrentProject = new ArrayList<>();
                for(String projectName: paths){
                    Arrays.sort(paths);
                    ProjectFile = new File(projectsPath + "/" +  projectName);
                    if (ProjectFile.isDirectory()) continue;
                    currentProject = objectMapper.readValue(ProjectFile, ProjectImpl.class);
                    cardsFolderPath = new File( projectsPath + "/" + currentProject.getProjectName());
                    if (cardsFolderPath.isDirectory()){
                        String[] cardsPaths = cardsFolderPath.list();
                        if (cardsPaths != null){
                            Arrays.sort(cardsPaths);
                            File currentCard;
                            for (String cardName: cardsPaths){
                                currentCard = new File(cardsFolderPath + "/" + cardName);
                                cardsForCurrentProject.add(objectMapper.readValue(currentCard, Card.class));
                            }
                        }
                    }
                    currentProject.addCards(cardsForCurrentProject);
                    cardsForCurrentProject.clear();
                    projectsList.add(currentProject);
                }
            }
        }
        return projectsList;
    }

    // Creo il file e ci aggiungo le informazioni dell'utente dato come parametro
    public boolean updateUsers(User User) throws IOException {
        File userFile = new File(this.usersPath + "/" + User.getName() + ".json");
        if (userFile.createNewFile()) {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(userFile, User);
            return true;
        }
        return false;
    }
    // Aggiorno le informazioni relative al progetto
    public void updateProjects(Project project) throws IOException {
        File projectFile = new File(projectsPath + "/" +  project.getProjectName() + ".json");
        File cards = new File(this.projectsPath + "/" + project.getProjectName());
        if (!projectFile.exists()) if (projectFile.createNewFile()) createProjectCardsPath(cards);
        File currentCardFile;
        for (Card currentCard: project.getCards()){
            currentCardFile = new File(cards + "/" + currentCard.getName() + ".json");
            if(!currentCardFile.exists()) { boolean newFile = currentCardFile.createNewFile(); }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(currentCardFile, currentCard);
        }
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(projectFile, project);
    }
    // Elimino il progetto, e la relativa cartella di card
    public void updateProjects(Project project, String delete) {
        File projectFile = new File(projectsPath + "/" +  project.getProjectName() + ".json");
        File cards = new File(this.projectsPath + "/" + project.getProjectName());
        if (projectFile.exists() && delete.equals("delete")){
            if (cards.exists()) deleteRec(cards);
            if (!projectFile.delete()) System.out.println("Can't delete "+projectFile);
        }
    }

    // Semplice metodo per eliminare ricorsivamente gli elementi in una cartella e la cartella stessa
    private void deleteRec(File path) {
        String[] entries = path.list();
        if (entries!=null) {
            for (String subEntry : entries) {
                File currentFile = new File(path.getPath(), subEntry);
                if (currentFile.isDirectory()) {
                    deleteRec(currentFile);
                }
                if (!currentFile.delete()) System.out.println("Can't delete "+ currentFile);
            }
            if (!path.delete()) System.out.println("Can't delete " + path);
        }
    }
}
