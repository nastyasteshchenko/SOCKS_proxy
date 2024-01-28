package nsu.networks.exceptions;

public class ProxyWorkingException extends Exception {
    private ProxyWorkingException(String message) {
        super(message);
    }

    public static ProxyWorkingException acceptingClientError() {
        return new ProxyWorkingException("Error while accepting client");
    }

    public static ProxyWorkingException nextIdentificationFailure() {
        return new ProxyWorkingException("Next identification failure");
    }

    public static ProxyWorkingException readNextIdentificationFailure() {
        return new ProxyWorkingException("Read next identification failure");
    }

    public static ProxyWorkingException greetingFailure() {
        return new ProxyWorkingException("Greeting failure");
    }

    public static ProxyWorkingException readGreetingFailure() {
        return new ProxyWorkingException("Read greeting failure");
    }

    public static ProxyWorkingException wrongClientRequest() {
        return new ProxyWorkingException("Client request is not supported");
    }

    public static ProxyWorkingException clientQueryFailure() {
        return new ProxyWorkingException("Client query failure");
    }

    public static ProxyWorkingException createHostSocketChannelError() {
        return new ProxyWorkingException("Create host socket channel error");
    }

    public static ProxyWorkingException hostUnreachable() {
        return new ProxyWorkingException("Host unreachable");
    }

    public static ProxyWorkingException errorWhileSendingAnswer() {
        return new ProxyWorkingException("Error while writing client");
    }

    public static ProxyWorkingException connectionFailure() {
        return new ProxyWorkingException("Connection failure");
    }

    public static ProxyWorkingException errorWhileReadingClient() {
        return new ProxyWorkingException("Error while reading client");
    }

    public static ProxyWorkingException errorWhileReadingFromHost() {
        return new ProxyWorkingException("Error while reading from host");
    }

}
