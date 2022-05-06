package net.vite.wallet.abi;

import net.vite.wallet.abi.datatypes.Function;
import net.vite.wallet.abi.datatypes.StaticArray;
import net.vite.wallet.abi.datatypes.Type;
import net.vite.wallet.abi.datatypes.Uint;
import org.vitelabs.mobile.Mobile;
import org.web3j.utils.Collection;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.List;


public class FunctionEncoder {

    private FunctionEncoder() {
    }

    public static String encode(Function function) {
        List<Type> parameters = function.getInputParameters();

        String methodSignature = buildMethodSignature(function.getName(), parameters);
        String methodId = buildMethodId(methodSignature);

        StringBuilder result = new StringBuilder();
        result.append(methodId);

        return encodeParameters(parameters, result);
    }

    public static String encodeConstructor(List<Type> parameters) {
        return encodeParameters(parameters, new StringBuilder());
    }

    private static String encodeParameters(List<Type> parameters, StringBuilder result) {
        int dynamicDataOffset = getLength(parameters) * Type.MAX_BYTE_LENGTH;
        StringBuilder dynamicData = new StringBuilder();

        for (Type parameter : parameters) {
            String encodedValue = TypeEncoder.encode(parameter);

            if (TypeEncoder.isDynamic(parameter)) {
                String encodedDataOffset = TypeEncoder.encodeNumeric(
                        new Uint(BigInteger.valueOf(dynamicDataOffset)));
                result.append(encodedDataOffset);
                dynamicData.append(encodedValue);
                dynamicDataOffset += encodedValue.length() >> 1;
            } else {
                result.append(encodedValue);
            }
        }
        result.append(dynamicData);

        return result.toString();
    }

    private static int getLength(List<Type> parameters) {
        int count = 0;
        for (Type type : parameters) {
            if (type instanceof StaticArray) {
                count += ((StaticArray) type).getValue().size();
            } else {
                count++;
            }
        }
        return count;
    }

    static String buildMethodSignature(String methodName, List<Type> parameters) {
        StringBuilder result = new StringBuilder();
        result.append(methodName);
        result.append("(");

        final String params = Collection.join(parameters, ",", Type::getTypeAsString);
        result.append(params);
        result.append(")");
        return result.toString();
    }

    static String buildMethodId(String methodSignature) {
        byte[] input = methodSignature.getBytes();
        byte[] hash = Mobile.hash256(input);
        return Numeric.toHexString(hash, 0, hash.length, false).substring(0, 8);
    }
}
