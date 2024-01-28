package nsu.networks.bytesvals;

public enum SocksVersion {
    SOCKS5((byte) 0x05);
    private final byte val;

    SocksVersion(byte val) {
        this.val = val;
    }

    public byte getVal() {
        return val;
    }
}
