package nsu.networks.bytesvals;

public enum AddressType {
    IPV4((byte) 0x01),
    DOMAIN_NAME((byte) 0x03),
    IPV6((byte) 0x04);
    private final byte val;

    AddressType(byte val) {
        this.val = val;
    }

    public byte getVal() {
        return val;
    }
}
