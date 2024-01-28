package nsu.networks.bytesvals;

public enum AuthMethods {
    NO_AUTH((byte) 0x00),
    NO_ACCEPTABLE_METHOD((byte) 0xFF);
    private final byte val;

    AuthMethods(byte val) {
        this.val = val;
    }

    public byte getVal() {
        return val;
    }
}
