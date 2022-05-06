package net.vite.wallet.abi.datatypes;

import org.bouncycastle.util.encoders.Hex;
import org.vitelabs.mobile.Mobile;


public class TokenId implements Type<String> {
    public static final int Size = 10;
    public static final String TYPE_NAME = "tokenId";

    private final org.vitelabs.mobile.TokenTypeId value;


    public TokenId(org.vitelabs.mobile.TokenTypeId tti) throws Exception {
        value = tti;
    }

    public TokenId(String hexValue) throws Exception {
        value = Mobile.newTokenTypeIdFromByte(Hex.decode(hexValue.substring(64 - (Size << 1), 64)));
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

        TokenId address = (TokenId) o;

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
