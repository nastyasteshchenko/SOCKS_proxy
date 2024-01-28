package nsu.networks.exceptions;

public class DangerousProxyException extends Exception {
    private DangerousProxyException(String message) {
        super(message);
    }

    public static DangerousProxyException creatingProxyError() {
        return new DangerousProxyException("Error while creating proxy");
    }

    public static DangerousProxyException closingChannelsError() {
        return new DangerousProxyException("Error while closing channels");
    }

    public static DangerousProxyException DNSError() {
        return new DangerousProxyException("DNS failed");
    }
}
