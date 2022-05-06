package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.StaticArray;
import net.vite.wallet.abi.datatypes.Type;

import java.util.List;


public class StaticArray27<T extends Type> extends StaticArray<T> {
    @Deprecated
    public StaticArray27(List<T> values) {
        super(27, values);
    }

    @Deprecated
    @SafeVarargs
    public StaticArray27(T... values) {
        super(27, values);
    }

    public StaticArray27(Class<T> type, List<T> values) {
        super(type, 27, values);
    }

    @SafeVarargs
    public StaticArray27(Class<T> type, T... values) {
        super(type, 27, values);
    }
}
