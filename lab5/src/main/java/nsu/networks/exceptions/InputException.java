package nsu.networks.exceptions;

public class InputException extends  Exception{
    private InputException(String message) {
        super(message);
    }
    static InputException notValidPort() {
        return new InputException("Port is not valid");
    }
}