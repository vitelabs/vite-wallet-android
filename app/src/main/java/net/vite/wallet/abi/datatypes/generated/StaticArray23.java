package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.StaticArray;
import net.vite.wallet.abi.datatypes.Type;

import java.util.List;


public class StaticArray23<T extends Type> extends StaticArray<T> {
    @Deprecated
    public StaticArray23(List<T> values) {
        super(23, values);
    }

    @Deprecated
    @SafeVarargs
    public StaticArray23(T... values) {
        super(23, values);
    }

    public StaticArray23(Class<T> type, List<T> values) {
        super(type, 23, values);
    }

    @SafeVarargs
    public StaticArray23(Class<T> type, T... values) {
        super(type, 23, values);
    }
}
