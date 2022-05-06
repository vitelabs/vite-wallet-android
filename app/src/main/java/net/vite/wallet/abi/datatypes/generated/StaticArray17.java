package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.StaticArray;
import net.vite.wallet.abi.datatypes.Type;

import java.util.List;


public class StaticArray17<T extends Type> extends StaticArray<T> {
    @Deprecated
    public StaticArray17(List<T> values) {
        super(17, values);
    }

    @Deprecated
    @SafeVarargs
    public StaticArray17(T... values) {
        super(17, values);
    }

    public StaticArray17(Class<T> type, List<T> values) {
        super(type, 17, values);
    }

    @SafeVarargs
    public StaticArray17(Class<T> type, T... values) {
        super(type, 17, values);
    }
}
