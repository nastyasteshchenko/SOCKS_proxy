package nsu.networks.exceptions;

public class CheckingInputUtils {
    private static final int MIN_USER_PORT = 1024;
    private static final int MAX_USER_PORT = 49151;

    public static void checkValidPort(int port) throws InputException {
        if (port < MIN_USER_PORT || port > MAX_USER_PORT) {
            throw InputException.notValidPort();
        }
    }

}