package nsu.networks.handlers;


import nsu.networks.attachments.HostAttachment;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class HostHandler {

    private static final int BUF_SIZE = 1460;
    private final ByteBuffer buffer = ByteBuffer.allocate(BUF_SIZE);

    public boolean connect(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        if (socketChannel.finishConnect()) {
            ClientInfo clientInfo = (ClientInfo) key.attachment();
            clientInfo.clientAttachment().switchStatusToForwarding();
            key.interestOps(SelectionKey.OP_READ);
            return true;
        }
        return false;
    }

    public void read(SelectionKey key) throws IOException {
        HostAttachment hostAttachment = (HostAttachment) key.attachment();
        SocketChannel hostChannel = (SocketChannel) key.channel();

        buffer.clear();
        int totallyRead = hostChannel.read(buffer);
        if (totallyRead == -1) {
            return;
        }
        buffer.flip();

        int wrote;
        int totallyWrote = 0;
        while (totallyWrote != totallyRead) {
            wrote = hostAttachment.clientSocket().write(buffer);
            totallyWrote += wrote;
        }
    }
}
