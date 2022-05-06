package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.Uint;

import java.math.BigInteger;


public class Uint24 extends Uint {
    public static final Uint24 DEFAULT = new Uint24(BigInteger.ZERO);

    public Uint24(BigInteger value) {
        super(24, value);
    }

    public Uint24(long value) {
        this(BigInteger.valueOf(value));
    }
}
