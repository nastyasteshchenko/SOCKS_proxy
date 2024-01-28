package nsu.networks.handlers;

import java.net.InetAddress;

record DNSResponse(InetAddress ip, ClientInfo info) {
}
