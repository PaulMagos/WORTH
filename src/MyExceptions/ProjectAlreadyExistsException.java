package MyExceptions;

public class ProjectAlreadyExistsException extends Exception {
    public ProjectAlreadyExistsException(String project_already_exists) {
        super(project_already_exists);
    }
}
