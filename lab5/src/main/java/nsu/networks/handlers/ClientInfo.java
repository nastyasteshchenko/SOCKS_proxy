package nsu.networks.handlers;

import nsu.networks.attachments.ClientAttachment;

import java.nio.channels.SocketChannel;

public record ClientInfo(SocketChannel clientSocket, ClientAttachment clientAttachment) {
}
