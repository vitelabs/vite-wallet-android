package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.Bytes;


public class Bytes18 extends Bytes {
    public static final Bytes18 DEFAULT = new Bytes18(new byte[18]);

    public Bytes18(byte[] value) {
        super(18, value);
    }
}
