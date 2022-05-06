package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.Int;

import java.math.BigInteger;


public class Int224 extends Int {
    public static final Int224 DEFAULT = new Int224(BigInteger.ZERO);

    public Int224(BigInteger value) {
        super(224, value);
    }

    public Int224(long value) {
        this(BigInteger.valueOf(value));
    }
}
