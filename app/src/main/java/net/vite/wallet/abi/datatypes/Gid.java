package net.vite.wallet.abi.datatypes;

import org.bouncycastle.util.encoders.Hex;
import org.vitelabs.mobile.Mobile;


public class Gid implements Type<String> {

    public static final int Size = 10;
    public static final String TYPE_NAME = "gid";

    private final org.vitelabs.mobile.Gid value;


    public Gid(org.vitelabs.mobile.Gid gid) {
        value = gid;
    }

    public Gid(String paddedHexValue) throws Exception {
        value = Mobile.newGidFromByte(Hex.decode(paddedHexValue.substring(64 - (Size << 1), 64)));
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

        Gid address = (Gid) o;

        return value.equals(address.value);
    }

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    public byte[] getBytes() {
        return value.getBytes();
    }
}
