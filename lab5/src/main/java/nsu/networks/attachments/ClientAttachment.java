package nsu.networks.attachments;

import java.nio.channels.SocketChannel;

public class ClientAttachment {
    private Status status = Status.GREETING;
    private byte[] clientRequest;
    private int hostPort;
    private SocketChannel hostSocket;

    public boolean isGreeting() {
        return status == Status.GREETING;
    }

    public boolean isNextIdentification() {
        return status == Status.NEXT_IDENTIFICATION;
    }

    public void switchStatusToNextIdentification() {
        status = Status.NEXT_IDENTIFICATION;
    }

    public void switchStatusToHostConnecting() {
        status = Status.HOST_CONNECTING;
    }

    public void switchStatusToForwarding() {
        status = Status.FORWARDING;
    }

    public boolean isForwarding() {
        return status == Status.FORWARDING;
    }

    public byte[] getClientRequest() {
        return clientRequest;
    }

    public void setClientRequest(byte[] msg) {
        clientRequest = msg;
    }

    public int getHostPort() {
        return hostPort;
    }

    public void setHostPort(int port) {
        hostPort = port;
    }

    public void setHostSocket(SocketChannel hostSocket) {
        this.hostSocket = hostSocket;
    }

    public SocketChannel getHostSocket() {
        return hostSocket;
    }


}
