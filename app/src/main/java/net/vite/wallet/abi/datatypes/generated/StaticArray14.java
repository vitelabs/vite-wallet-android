package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.StaticArray;
import net.vite.wallet.abi.datatypes.Type;

import java.util.List;


public class StaticArray14<T extends Type> extends StaticArray<T> {
    @Deprecated
    public StaticArray14(List<T> values) {
        super(14, values);
    }

    @Deprecated
    @SafeVarargs
    public StaticArray14(T... values) {
        super(14, values);
    }

    public StaticArray14(Class<T> type, List<T> values) {
        super(type, 14, values);
    }

    @SafeVarargs
    public StaticArray14(Class<T> type, T... values) {
        super(type, 14, values);
    }
}
