package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.Bytes;


public class Bytes21 extends Bytes {
    public static final Bytes21 DEFAULT = new Bytes21(new byte[21]);

    public Bytes21(byte[] value) {
        super(21, value);
    }
}
