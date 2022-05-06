package net.vite.wallet.abi;

import net.vite.wallet.abi.datatypes.Event;
import net.vite.wallet.abi.datatypes.Type;
import org.vitelabs.mobile.Mobile;
import org.web3j.utils.Collection;
import org.web3j.utils.Numeric;

import java.util.List;

public class EventEncoder {

    private EventEncoder() {
    }

    public static String encode(Event event) {

        String methodSignature = buildMethodSignature(
                event.getName(),
                event.getParameters());

        return buildEventSignature(methodSignature);
    }

    static <T extends Type> String buildMethodSignature(
            String methodName, List<TypeReference<T>> parameters) {

        StringBuilder result = new StringBuilder();
        result.append(methodName);
        result.append("(");
        final String params = Collection.join(parameters, ",", Utils::getTypeName);
        result.append(params);
        result.append(")");
        return result.toString();
    }

    public static String buildEventSignature(String methodSignature) {
        byte[] input = methodSignature.getBytes();
        byte[] hash = Mobile.hash256(input);
        return Numeric.toHexString(hash, 0, hash.length, false);
    }
}
