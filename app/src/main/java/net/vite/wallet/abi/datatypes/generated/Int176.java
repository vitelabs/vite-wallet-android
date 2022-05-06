package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.Int;

import java.math.BigInteger;


public class Int176 extends Int {
    public static final Int176 DEFAULT = new Int176(BigInteger.ZERO);

    public Int176(BigInteger value) {
        super(176, value);
    }

    public Int176(long value) {
        this(BigInteger.valueOf(value));
    }
}
