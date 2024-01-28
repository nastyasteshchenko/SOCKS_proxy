package nsu.networks.attachments;

import java.nio.channels.SocketChannel;

public record HostAttachment(SocketChannel clientSocket) {
}
