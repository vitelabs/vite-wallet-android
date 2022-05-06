package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.Uint;

import java.math.BigInteger;


public class Uint32 extends Uint {
    public static final Uint32 DEFAULT = new Uint32(BigInteger.ZERO);

    public Uint32(BigInteger value) {
        super(32, value);
    }

    public Uint32(long value) {
        this(BigInteger.valueOf(value));
    }
}
