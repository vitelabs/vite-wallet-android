package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.StaticArray;
import net.vite.wallet.abi.datatypes.Type;

import java.util.List;


public class StaticArray26<T extends Type> extends StaticArray<T> {
    @Deprecated
    public StaticArray26(List<T> values) {
        super(26, values);
    }

    @Deprecated
    @SafeVarargs
    public StaticArray26(T... values) {
        super(26, values);
    }

    public StaticArray26(Class<T> type, List<T> values) {
        super(type, 26, values);
    }

    @SafeVarargs
    public StaticArray26(Class<T> type, T... values) {
        super(type, 26, values);
    }
}
