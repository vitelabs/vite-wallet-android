package net.vite.wallet.abi.datatypes;

import org.bouncycastle.util.encoders.Hex;
import org.vitelabs.mobile.Mobile;

public class Address implements Type<String> {

    public static final String TYPE_NAME = "address";
    public static final int Size = 21;
    private final org.vitelabs.mobile.Address value;

    public Address(org.vitelabs.mobile.Address address) throws Exception {
        value = address;
    }

    public Address(String pureHexBytes) throws Exception {
        value = Mobile.newAddressFromByte(Hex.decode(pureHexBytes.substring(64 - (Size << 1), 64)));
    }


    public byte[] getBytes() {
        return value.getBytes();
    }

    @Override
    public String getTypeAsString() {
        return TYPE_NAME;
    }

    @Override
    public String toString() {
        try {
            return value.getHex();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @Override
    public String getValue() {
        return toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Address address = (Address) o;

        return value.equals(address.value);
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }
}
