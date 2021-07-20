public class NoDocumentsFoundException  extends Exception{
    public NoDocumentsFoundException() {
        super();
    }

    public NoDocumentsFoundException(String message) {
        super(message);
    }

    public NoDocumentsFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
