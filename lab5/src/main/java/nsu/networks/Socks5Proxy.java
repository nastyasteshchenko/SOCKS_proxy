package nsu.networks;

import nsu.networks.attachments.ClientAttachment;
import nsu.networks.attachments.HostAttachment;
import nsu.networks.exceptions.CheckingInputUtils;
import nsu.networks.exceptions.ProxyWorkingException;
import nsu.networks.exceptions.DangerousProxyException;
import nsu.networks.exceptions.InputException;
import nsu.networks.handlers.ClientHandler;
import nsu.networks.handlers.ClientInfo;
import nsu.networks.handlers.HostHandler;
import org.xbill.DNS.ResolverConfig;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;

class Socks5Proxy extends Thread {
    //private static final String HOST = "192.168.43.39";
    private static final String HOST = "localhost";
    private final Selector selector;
    private final ClientHandler clientHandler;
    private final HostHandler hostHandler;

    private Socks5Proxy(Selector selector, ClientHandler clientHandler) {
        this.selector = selector;
        this.clientHandler = clientHandler;
        hostHandler = new HostHandler();
    }

    static Socks5Proxy create(int port) throws InputException, DangerousProxyException {
        CheckingInputUtils.checkValidPort(port);

        try {
            Selector selector = Selector.open();
            ServerSocketChannel proxySocket = ServerSocketChannel.open();
            DatagramChannel DNSChannel = DatagramChannel.open();

            proxySocket.bind(new InetSocketAddress(HOST, port));
            proxySocket.configureBlocking(false);
            proxySocket.register(selector, SelectionKey.OP_ACCEPT);

            DNSChannel.configureBlocking(false);
            InetSocketAddress dnsServer = ResolverConfig.getCurrentConfig().server();
            DNSChannel.connect(dnsServer);
            DNSChannel.register(selector, SelectionKey.OP_READ);

            return new Socks5Proxy(selector, new ClientHandler(DNSChannel));

        } catch (IOException e) {
            throw DangerousProxyException.creatingProxyError();
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    if (key.isValid()) {
                        if (key.isAcceptable()) {
                            accept(key);
                        } else if (key.isReadable()) {
                            read(key);
                        } else if (key.isConnectable()) {
                            connect(key);
                        }
                    }
                    iterator.remove();
                }
            } catch (ProxyWorkingException ignored) {
            } catch (DangerousProxyException | IOException e) {
                try {
                    closeProxy();
                } catch (DangerousProxyException ignored) {
                    break;
                }
                break;
            }
        }
    }

    private void accept(SelectionKey key) throws ProxyWorkingException {
        try {
            ServerSocketChannel serverSocket = (ServerSocketChannel) key.channel();
            SocketChannel clientSocket = serverSocket.accept();
            clientSocket.configureBlocking(false);
            clientSocket.register(key.selector(), SelectionKey.OP_READ, new ClientAttachment());
        } catch (IOException e) {
            throw ProxyWorkingException.acceptingClientError();
        }
    }

    private void read(SelectionKey key) throws ProxyWorkingException, DangerousProxyException {
        if (key.channel() instanceof SocketChannel) {
            if (key.attachment() instanceof ClientAttachment) {
                readClient(key);
            } else {
                readHost(key);
            }
        } else {
            readDNS(key);
        }
    }

    private void connect(SelectionKey key) throws ProxyWorkingException, DangerousProxyException {
        ClientInfo clientInfo = (ClientInfo) key.attachment();
        try {
            if (hostHandler.connect(key)) {
                sendRequestGrantedAnswer(key, clientInfo);
                key.attach(new HostAttachment(clientInfo.clientSocket()));
            }
        } catch (IOException e) {
            sendConnectionFailureAnswer(key, clientInfo);
            throw ProxyWorkingException.connectionFailure();
        }
    }

    private void readClient(SelectionKey key) throws ProxyWorkingException, DangerousProxyException {
        try {
            clientHandler.readClient(key);
        } catch (ProxyWorkingException e) {
            closeConnection(key);
            throw ProxyWorkingException.errorWhileReadingClient();
        }
    }

    private void readHost(SelectionKey key) throws ProxyWorkingException, DangerousProxyException {
        try {
            hostHandler.read(key);
        } catch (IOException e) {
            closeConnection(key);
            throw ProxyWorkingException.errorWhileReadingFromHost();
        }
    }

    private void readDNS(SelectionKey key) throws ProxyWorkingException, DangerousProxyException {
        try {
            clientHandler.readDNS(key);
        } catch (IOException e) {
            closeProxy();
            throw DangerousProxyException.DNSError();
        }
    }

    private void closeConnection(SelectionKey key) throws DangerousProxyException {
        try {
            Channel channel = key.channel();
            Object attachment = key.attachment();
            if (attachment instanceof ClientAttachment clientAttachment) {
                channel.close();
                if (clientAttachment.getHostSocket() != null) {
                    clientAttachment.getHostSocket().close();
                }
            } else if (attachment instanceof HostAttachment hostAttachment) {
                channel.close();
                if (hostAttachment.clientSocket() != null) {
                    hostAttachment.clientSocket().close();
                }
            }
            key.cancel();
        } catch (IOException e) {
            throw DangerousProxyException.closingChannelsError();
        }
    }

    private void closeProxy() throws DangerousProxyException {
        try {
            Iterator<SelectionKey> iterator = selector.keys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                key.channel().close();
                key.cancel();
                iterator.remove();
            }
            selector.close();
        } catch (IOException e) {
            throw DangerousProxyException.closingChannelsError();
        }
    }

    private void sendConnectionFailureAnswer(SelectionKey key, ClientInfo clientInfo) throws ProxyWorkingException, DangerousProxyException {
        try {
            clientHandler.sendConnectionFailure(clientInfo);
        } catch (IOException e1) {
            closeDueToConnectionFailure(key, clientInfo);
            throw ProxyWorkingException.errorWhileSendingAnswer();
        }
    }

    private void sendRequestGrantedAnswer(SelectionKey key, ClientInfo clientInfo) throws ProxyWorkingException, DangerousProxyException {
        try {
            clientHandler.sendRequestGranted(clientInfo);
        } catch (IOException e) {
            closeDueToConnectionFailure(key, clientInfo);
            throw ProxyWorkingException.errorWhileSendingAnswer();
        }
    }

    private void closeDueToConnectionFailure(SelectionKey key, ClientInfo clientInfo) throws DangerousProxyException {
        try {
            clientInfo.clientSocket().close();
            key.channel().close();
        } catch (IOException e) {
            throw DangerousProxyException.closingChannelsError();
        }
    }
}
