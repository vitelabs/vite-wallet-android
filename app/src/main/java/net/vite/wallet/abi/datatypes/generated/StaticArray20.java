package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.StaticArray;
import net.vite.wallet.abi.datatypes.Type;

import java.util.List;


public class StaticArray20<T extends Type> extends StaticArray<T> {
    @Deprecated
    public StaticArray20(List<T> values) {
        super(20, values);
    }

    @Deprecated
    @SafeVarargs
    public StaticArray20(T... values) {
        super(20, values);
    }

    public StaticArray20(Class<T> type, List<T> values) {
        super(type, 20, values);
    }

    @SafeVarargs
    public StaticArray20(Class<T> type, T... values) {
        super(type, 20, values);
    }
}
