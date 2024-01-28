package nsu.networks.handlers;

import nsu.networks.exceptions.ProxyWorkingException;
import nsu.networks.attachments.ClientAttachment;

import nsu.networks.bytesvals.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class ClientHandler {
    private static final int BUF_SIZE = 1460;
    private static final int IPV4_SIZE = 4;
    private static final int IPV6_SIZE = 16;
    private static final int PORT_SIZE = 2;
    private static final int GREETING_ANSWER_SIZE = 2;
    private static final List<Byte> ACCEPTABLE_AUTH_METHODS = List.of(AuthMethods.NO_AUTH.getVal());
    private final DomainNamesResolver domainNamesResolver;
    private final ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);

    public ClientHandler(DatagramChannel DNSChannel) {
        domainNamesResolver = new DomainNamesResolver(DNSChannel);
    }

    public void readClient(SelectionKey key) throws ProxyWorkingException {
        ClientAttachment clientAttachment = (ClientAttachment) key.attachment();
        SocketChannel clientSocket = (SocketChannel) key.channel();
        if (clientAttachment.isGreeting()) {
            startGreeting(clientSocket, clientAttachment);
        } else if (clientAttachment.isNextIdentification()) {
            startNextIdentification(key.selector(), clientSocket, clientAttachment);
        } else if (clientAttachment.isForwarding()) {
            handleClientQuery(clientSocket, clientAttachment);
        }
    }

    public void readDNS(SelectionKey key) throws IOException, ProxyWorkingException {
        DNSResponse dnsResponse = domainNamesResolver.getDNSResponse();
        ClientInfo info = dnsResponse.info();
        if (info == null) {
            return;
        }
        if (dnsResponse.ip() == null) {
            sendNextIdentificationAnswer(info.clientSocket(), info.clientAttachment().getClientRequest(),
                    Answer.HOST_UNAVAILABLE.getVal());
            info.clientSocket().close();
            info.clientAttachment().getHostSocket().close();
            throw ProxyWorkingException.hostUnreachable();
        } else {
            try {
                createHostSocket(key.selector(), dnsResponse.ip(), info);
            } catch (IOException e) {
                info.clientSocket().close();
                info.clientAttachment().getHostSocket().close();
                throw ProxyWorkingException.createHostSocketChannelError();
            }
        }
    }

    public void sendRequestGranted(ClientInfo info) throws IOException {
        sendNextIdentificationAnswer(info.clientSocket(), info.clientAttachment().getClientRequest(),
                Answer.REQUEST_GRANTED.getVal());
    }

    public void sendConnectionFailure(ClientInfo info) throws IOException {
        sendNextIdentificationAnswer(info.clientSocket(), info.clientAttachment().getClientRequest(),
                Answer.CONNECTION_FAILURE.getVal());
    }

    private void startGreeting(SocketChannel clientSocket, ClientAttachment clientAttachment)
            throws ProxyWorkingException {
        try {
            readGreetingMsg(clientSocket);
            if (buffer.get(0) == SocksVersion.SOCKS5.getVal()) {
                boolean isMethodFound = false;
                for (int i = 2; i < buffer.get(1) + 2; i++) {
                    if (ACCEPTABLE_AUTH_METHODS.contains(buffer.get(i))) {
                        isMethodFound = true;
                        sendGreetingAnswer(clientSocket, buffer.get(i));
                        clientAttachment.switchStatusToNextIdentification();
                    }
                }
                if (!isMethodFound) {
                    sendGreetingAnswer(clientSocket, AuthMethods.NO_ACCEPTABLE_METHOD.getVal());
                }
            } else {
                sendGreetingAnswer(clientSocket, AuthMethods.NO_ACCEPTABLE_METHOD.getVal());
            }
        } catch (IOException | ProxyWorkingException e) {
            throw ProxyWorkingException.greetingFailure();
        }
    }

    private void readGreetingMsg(SocketChannel clientSocket) throws IOException, ProxyWorkingException {
        buffer.clear();
        int msgSize = 2;
        int totalRead = 0;
        while (totalRead != msgSize) {
            int count = clientSocket.read(buffer);
            if (count == -1) {
                throw ProxyWorkingException.readGreetingFailure();
            }
            totalRead += count;
            if (msgSize == 2 && totalRead > 1) {
                msgSize += buffer.get(1);
            }
        }
        buffer.flip();
    }

    private void sendGreetingAnswer(SocketChannel clientChannel, byte authMethod) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(GREETING_ANSWER_SIZE)
                .put(SocksVersion.SOCKS5.getVal()).put(authMethod).flip();
        int totalWrite = 0;
        while (totalWrite != GREETING_ANSWER_SIZE) {
            totalWrite += clientChannel.write(buffer);
        }
    }

    private void startNextIdentification(Selector selector, SocketChannel clientSocket,
                                         ClientAttachment clientAttachment) throws ProxyWorkingException {

        try {
            int msgSize = readNextIdentification(clientSocket);
            clientAttachment.setHostPort(buffer.getShort(msgSize - PORT_SIZE));

            setClientRequest(clientAttachment, msgSize);

            if (checkClientRequestMsg(clientSocket, clientAttachment.getClientRequest())) {
                handleClientRequest(selector, clientSocket, clientAttachment);
            } else {
                throw ProxyWorkingException.wrongClientRequest();
            }

        } catch (IOException | ProxyWorkingException e) {
            throw ProxyWorkingException.nextIdentificationFailure();
        }

    }

    private int readNextIdentification(SocketChannel clientSocket) throws IOException, ProxyWorkingException {
        buffer.clear();
        int msgSize = 5;
        int totalRead = 0;
        while (totalRead != msgSize) {
            int count = clientSocket.read(buffer);
            if (count == -1) {
                throw ProxyWorkingException.readNextIdentificationFailure();
            }
            totalRead += count;
            if (msgSize == 5 && totalRead > 4) {
                if (buffer.get(3) == AddressType.DOMAIN_NAME.getVal()) {
                    msgSize += buffer.get(4) + PORT_SIZE;
                } else if (buffer.get(3) == AddressType.IPV4.getVal()) {
                    msgSize += IPV4_SIZE - 1 + PORT_SIZE;
                } else if (buffer.get(3) == AddressType.IPV6.getVal()) {
                    msgSize += IPV6_SIZE - 1 + PORT_SIZE;
                }
            }
        }
        buffer.flip();
        return msgSize;
    }

    private void setClientRequest(ClientAttachment clientAttachment, int msgSize) {
        byte[] msg = new byte[msgSize];
        buffer.get(msg, 0, msgSize);
        clientAttachment.setClientRequest(msg);
        buffer.clear();
    }

    private boolean checkClientRequestMsg(SocketChannel clientSocket, byte[] clientRequest) throws IOException {
        if (clientRequest[0] != SocksVersion.SOCKS5.getVal()) {
            sendNextIdentificationAnswer(clientSocket, clientRequest, Answer.SOCKS_SERVER_ERROR.getVal());
            return false;
        }
        if (clientRequest[1] != Commands.ESTABLISH_TCP_IP_CONNECTION.getVal()) {
            sendNextIdentificationAnswer(clientSocket, clientRequest, Answer.COMMAND_UNSUPPORTED.getVal());
            return false;
        }
        if (clientRequest[2] == AddressType.IPV6.getVal()) {
            sendNextIdentificationAnswer(clientSocket, clientRequest, Answer.ADDRESS_TYPE_UNSUPPORTED.getVal());
            return false;
        }
        return true;
    }

    private void handleClientRequest(Selector selector, SocketChannel clientSocket, ClientAttachment clientAttachment)
            throws IOException, ProxyWorkingException {
        clientAttachment.setHostSocket(SocketChannel.open());
        byte[] clientRequest = clientAttachment.getClientRequest();

        if (clientRequest[3] == AddressType.DOMAIN_NAME.getVal()) {

            byte nameSize = clientRequest[4];
            byte[] domainName = Arrays.copyOfRange(clientRequest, 5, 5 + nameSize);

            String domain = new String(domainName, StandardCharsets.UTF_8);
            InetAddress address = domainNamesResolver.checkCash(domain);
            if (address != null) {
                createHostSocket(selector, address, new ClientInfo(clientSocket, clientAttachment));
            } else {
                domainNamesResolver.sendQuery(new ClientInfo(clientSocket, clientAttachment), domain);
            }

        } else if (clientRequest[3] == AddressType.IPV4.getVal()) {

            byte[] addressInBytes = Arrays.copyOfRange(clientRequest, 5, 5 + IPV4_SIZE);
            try {
                ClientInfo clientInfo = new ClientInfo(clientSocket, clientAttachment);
                createHostSocket(selector, InetAddress.getByAddress(addressInBytes), clientInfo);
            } catch (IOException e) {
                throw ProxyWorkingException.createHostSocketChannelError();
            }
        }
    }

    private void sendNextIdentificationAnswer(SocketChannel clientSocket, byte[] clientRequest, byte answerCode)
            throws IOException {
        clientRequest[1] = answerCode;
        int answerSize = clientRequest.length;
        ByteBuffer buffer = ByteBuffer.allocate(clientRequest.length).put(clientRequest).flip();
        int totalWrite = 0;
        while (totalWrite != answerSize) {
            totalWrite += clientSocket.write(buffer);
        }
    }

    private void createHostSocket(Selector selector, InetAddress address, ClientInfo info) throws IOException {
        SocketChannel hostSocket = info.clientAttachment().getHostSocket();
        hostSocket.configureBlocking(false);
        hostSocket.connect(new InetSocketAddress(address.getHostAddress(), info.clientAttachment().getHostPort()));
        hostSocket.register(selector, SelectionKey.OP_CONNECT, info);
        info.clientAttachment().switchStatusToHostConnecting();
    }

    private void handleClientQuery(SocketChannel clientSocket, ClientAttachment clientAttachment)
            throws ProxyWorkingException {
        try {
            buffer.clear();
            int count = clientSocket.read(buffer);
            buffer.flip();
            if (count == -1) {
                return;
            }
            int wrote;
            int totallyWrote = 0;
            while (totallyWrote != count) {
                wrote = clientAttachment.getHostSocket().write(buffer);
                totallyWrote += wrote;
            }
        } catch (IOException e) {
            throw ProxyWorkingException.clientQueryFailure();
        }
    }
}
