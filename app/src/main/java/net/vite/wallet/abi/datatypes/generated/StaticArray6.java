package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.StaticArray;
import net.vite.wallet.abi.datatypes.Type;

import java.util.List;


public class StaticArray6<T extends Type> extends StaticArray<T> {
    @Deprecated
    public StaticArray6(List<T> values) {
        super(6, values);
    }

    @Deprecated
    @SafeVarargs
    public StaticArray6(T... values) {
        super(6, values);
    }

    public StaticArray6(Class<T> type, List<T> values) {
        super(type, 6, values);
    }

    @SafeVarargs
    public StaticArray6(Class<T> type, T... values) {
        super(type, 6, values);
    }
}
