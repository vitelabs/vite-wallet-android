package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.Int;

import java.math.BigInteger;


public class Int16 extends Int {
    public static final Int16 DEFAULT = new Int16(BigInteger.ZERO);

    public Int16(BigInteger value) {
        super(16, value);
    }

    public Int16(long value) {
        this(BigInteger.valueOf(value));
    }
}
