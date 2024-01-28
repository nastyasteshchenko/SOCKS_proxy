package nsu.networks.bytesvals;

public enum Answer {
    REQUEST_GRANTED((byte) 0x00),
    SOCKS_SERVER_ERROR((byte) 0x01),
    HOST_UNAVAILABLE((byte) 0x04),
    CONNECTION_FAILURE((byte) 0x05),
    COMMAND_UNSUPPORTED((byte) 0x07),
    ADDRESS_TYPE_UNSUPPORTED((byte) 0x08);
    private final byte val;

    Answer(byte val) {
        this.val = val;
    }

    public byte getVal() {
        return val;
    }
}
