package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.StaticArray;
import net.vite.wallet.abi.datatypes.Type;

import java.util.List;


public class StaticArray8<T extends Type> extends StaticArray<T> {
    @Deprecated
    public StaticArray8(List<T> values) {
        super(8, values);
    }

    @Deprecated
    @SafeVarargs
    public StaticArray8(T... values) {
        super(8, values);
    }

    public StaticArray8(Class<T> type, List<T> values) {
        super(type, 8, values);
    }

    @SafeVarargs
    public StaticArray8(Class<T> type, T... values) {
        super(type, 8, values);
    }
}
