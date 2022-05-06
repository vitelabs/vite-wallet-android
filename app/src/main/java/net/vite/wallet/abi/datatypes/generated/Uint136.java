package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.Uint;

import java.math.BigInteger;


public class Uint136 extends Uint {
    public static final Uint136 DEFAULT = new Uint136(BigInteger.ZERO);

    public Uint136(BigInteger value) {
        super(136, value);
    }

    public Uint136(long value) {
        this(BigInteger.valueOf(value));
    }
}
