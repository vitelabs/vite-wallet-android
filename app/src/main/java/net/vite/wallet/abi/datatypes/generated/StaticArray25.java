package net.vite.wallet.abi.datatypes.generated;

import net.vite.wallet.abi.datatypes.StaticArray;
import net.vite.wallet.abi.datatypes.Type;

import java.util.List;


public class StaticArray25<T extends Type> extends StaticArray<T> {
    @Deprecated
    public StaticArray25(List<T> values) {
        super(25, values);
    }

    @Deprecated
    @SafeVarargs
    public StaticArray25(T... values) {
        super(25, values);
    }

    public StaticArray25(Class<T> type, List<T> values) {
        super(type, 25, values);
    }

    @SafeVarargs
    public StaticArray25(Class<T> type, T... values) {
        super(type, 25, values);
    }
}
