package common;

public class Protocol {
    
    public static final String LOGIN = "LOGIN";
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String MESSAGE = "MSG";
    public static final String BROADCAST = "BCAST";
    public static final String PRIVATE = "PRIV";
    public static final String FILE_REQUEST = "FILE_REQ";
    public static final String FILE_START = "FILE_START";
    public static final String FILE_DATA = "FILE_DATA";
    public static final String FILE_COMPLETE = "FILE_COMPLETE";
    public static final String USER_LIST = "USER_LIST";
    public static final String USER_JOINED = "USER_JOINED";
    public static final String USER_LEFT = "USER_LEFT";
    public static final String DISCONNECT = "QUIT";
    
   
    public static final String DELIMITER = "|";
    
    public static final int BUFFER_SIZE = 4096;
    
    
    public static String buildMessage(String... parts) {
        return String.join(DELIMITER, parts);
    }
}