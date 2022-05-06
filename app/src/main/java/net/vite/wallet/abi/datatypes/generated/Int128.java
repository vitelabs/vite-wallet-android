package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.Int;

import java.math.BigInteger;


public class Int128 extends Int {
    public static final Int128 DEFAULT = new Int128(BigInteger.ZERO);

    public Int128(BigInteger value) {
        super(128, value);
    }

    public Int128(long value) {
        this(BigInteger.valueOf(value));
    }
}
