package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.Int;

import java.math.BigInteger;


public class Int32 extends Int {
    public static final Int32 DEFAULT = new Int32(BigInteger.ZERO);

    public Int32(BigInteger value) {
        super(32, value);
    }

    public Int32(long value) {
        this(BigInteger.valueOf(value));
    }
}
