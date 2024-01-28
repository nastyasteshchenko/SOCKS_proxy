package nsu.networks.handlers;

import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
import java.util.List;

class DomainNamesResolver {
    HashMap<Integer, ClientInfo> currentQueries = new HashMap<>();
    HashMap<String, InetAddress> cash = new HashMap<>();
    HashMap<Integer, String> hostDomainNames = new HashMap<>();
    private final DatagramChannel DNSChannel;

    DomainNamesResolver(DatagramChannel DNSChannel) {
        this.DNSChannel = DNSChannel;
    }

    InetAddress checkCash(String domainName) {
        String domain = domainName + ".";
        return cash.getOrDefault(domain, null);
    }

    void sendQuery(ClientInfo clientInfo, String domainName) throws IOException {
        String domain = domainName + ".";
        byte[] queryData = createQuery(clientInfo, domain);
        DNSChannel.write(ByteBuffer.wrap(queryData));
    }

    DNSResponse getDNSResponse() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.clear();
        DNSChannel.read(buffer);
        buffer.flip();

        Message response = new Message(buffer.array());
        Header header = response.getHeader();
        ClientInfo clientInfo = currentQueries.get(header.getID());
        currentQueries.remove(header.getID());

        if (clientInfo == null) {
            return new DNSResponse(null, null);
        }
        List<Record> answers = response.getSection(Section.ANSWER);
        for (Record answer : answers) {
            if (answer instanceof ARecord aRecord) {
                cash.put(hostDomainNames.get(header.getID()), aRecord.getAddress());
                return new DNSResponse(aRecord.getAddress(), clientInfo);
            }
        }
        return new DNSResponse(null, clientInfo);
    }

    private byte[] createQuery(ClientInfo clientInfo, String domain) throws TextParseException {
        Message query = new Message();
        Header header = query.getHeader();
        header.setOpcode(Opcode.QUERY);
        header.setFlag(Flags.RD);
        currentQueries.put(header.getID(), clientInfo);
        hostDomainNames.put(header.getID(), domain);
        Name name = new Name(domain);
        query.addRecord(Record.newRecord(name, Type.A, DClass.IN), Section.QUESTION);
        return query.toWire();
    }
}
