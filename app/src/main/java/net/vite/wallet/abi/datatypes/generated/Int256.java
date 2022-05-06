package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.Int;

import java.math.BigInteger;


public class Int256 extends Int {
    public static final Int256 DEFAULT = new Int256(BigInteger.ZERO);

    public Int256(BigInteger value) {
        super(256, value);
    }

    public Int256(long value) {
        this(BigInteger.valueOf(value));
    }
}
