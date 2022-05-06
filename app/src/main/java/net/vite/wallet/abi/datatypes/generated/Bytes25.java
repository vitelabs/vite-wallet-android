package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.Bytes;


public class Bytes25 extends Bytes {
    public static final Bytes25 DEFAULT = new Bytes25(new byte[25]);

    public Bytes25(byte[] value) {
        super(25, value);
    }
}
