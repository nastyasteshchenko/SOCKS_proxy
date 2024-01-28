package nsu.networks.bytesvals;

public enum Commands {
    ESTABLISH_TCP_IP_CONNECTION((byte) 0x01);
    private final byte val;

    Commands(byte val) {
        this.val = val;
    }

    public byte getVal() {
        return val;
    }
}
