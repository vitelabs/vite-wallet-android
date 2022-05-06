package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.StaticArray;
import net.vite.wallet.abi.datatypes.Type;

import java.util.List;


public class StaticArray15<T extends Type> extends StaticArray<T> {
    @Deprecated
    public StaticArray15(List<T> values) {
        super(15, values);
    }

    @Deprecated
    @SafeVarargs
    public StaticArray15(T... values) {
        super(15, values);
    }

    public StaticArray15(Class<T> type, List<T> values) {
        super(type, 15, values);
    }

    @SafeVarargs
    public StaticArray15(Class<T> type, T... values) {
        super(type, 15, values);
    }
}
