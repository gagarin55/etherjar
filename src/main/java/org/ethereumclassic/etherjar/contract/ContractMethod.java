package org.ethereumclassic.etherjar.contract;

import org.ethereumclassic.etherjar.contract.type.Type;
import org.ethereumclassic.etherjar.model.HexData;
import org.ethereumclassic.etherjar.model.MethodId;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A smart contract methods (either a constructor or a function).
 *
 * @author Igor Artamonov
 * @see Contract
 */
public class ContractMethod {

    final static Pattern ABI_PATTERN =
            Pattern.compile("([_a-zA-Z]\\w*)\\(([^:()\\s]*)\\)(?::\\((\\S*)\\))?");

    /**
     * Check contract method ABI signature.
     *
     * @param signature a contract method signature string representation
     * @return {@code true} if <code>signature</code> is valid, otherwise
     * {@code false}
     *
     * @see #ABI_PATTERN
     */
    static boolean isAbiValid(String signature) {
        return ABI_PATTERN.matcher(signature).matches();
    }

    /**
     * Create a {@link Builder} instance from methods signature like
     * <tt>name(datatype1,datatype2)</tt>, or <tt>transfer(address,uint256)</tt>.
     *
     * <p>The signature is defined as the canonical expression of the basic prototype,
     * i.e. the function name with the parenthesised list of parameter types.
     * Parameter types are split by a single comma - no spaces are used.
     *
     * @param repo a {@link Type} parsers repository
     * @param signature a contract method signature string representation
     * @return a {@link ContractMethod} instance
     */
    public static ContractMethod fromAbi(Type.Repository repo, String signature) {
        Matcher m = ABI_PATTERN.matcher(signature);

        if (!m.matches())
            throw new IllegalArgumentException("Wrong ABI method signature: " + signature);

        String name = m.group(1);

        ContractParametersTypes in = ContractParametersTypes.fromAbi(repo, m.group(2));
        ContractParametersTypes out = ContractParametersTypes.fromAbi(repo, m.group(3));

        return new Builder().withName(name).withInputTypes(in).withOutputTypes(out).build();
    }

    public static class Builder {

        private String name = null;

        private boolean isConstant = false;

        private ContractParametersTypes inputTypes = ContractParametersTypes.EMPTY;

        private ContractParametersTypes outputTypes = ContractParametersTypes.EMPTY;

        /**
         * @param name a contract methods name
         * @return builder instance
         */
        public Builder withName(String name) {
            this.name = Objects.requireNonNull(name);

            return this;
        }

        /**
         * Mark a contract methods as a constant methods.
         *
         * @return builder instance
         */
        public Builder asConstant() {
            isConstant = true;

            return this;
        }

        /**
         * @param types input parameters types
         * @return builder instance
         */
        public Builder withInputTypes(Type... types) {
            return withInputTypes(new ContractParametersTypes(types));
        }

        /**
         * @param types input parameters types
         * @return builder instance
         */
        public Builder withInputTypes(Collection<? extends Type> types) {
            return withInputTypes(new ContractParametersTypes(types));
        }

        /**
         * @param types input parameters types
         * @return builder instance
         */
        public Builder withInputTypes(ContractParametersTypes types) {
            inputTypes = Objects.requireNonNull(types);

            return this;
        }

        /**
         * @param types output parameters types
         * @return builder instance
         */
        public Builder withOutputTypes(Type... types) {
            return withOutputTypes(new ContractParametersTypes(types));
        }

        /**
         * @param types output parameters types
         * @return builder instance
         */
        public Builder withOutputTypes(Collection<? extends Type> types) {
            return withOutputTypes(new ContractParametersTypes(types));
        }

        /**
         * @param types output parameters types
         * @return builder instance
         */
        public Builder withOutputTypes(ContractParametersTypes types) {
            outputTypes = Objects.requireNonNull(types);

            return this;
        }

        /**
         * Build a {@link ContractMethod} instance with predefined conditions.
         *
         * @return a {@link ContractMethod} instance
         */
        public ContractMethod build() {
            if (Objects.isNull(name))
                throw new IllegalStateException("Undefined contract method name");

            return new ContractMethod(name, isConstant, inputTypes, outputTypes);
        }
    }

    private final MethodId id;

    private final String name;

    private final boolean isConstant;

    private final ContractParametersTypes inputTypes;

    private final ContractParametersTypes outputTypes;

    public ContractMethod(String name) {
        this(name, false, ContractParametersTypes.EMPTY, ContractParametersTypes.EMPTY);
    }

    public ContractMethod(String name, ContractParametersTypes inputTypes) {
        this(name, false, inputTypes, ContractParametersTypes.EMPTY);
    }

    public ContractMethod(String name, boolean isConstant, ContractParametersTypes inputTypes) {
        this(name, isConstant, inputTypes, ContractParametersTypes.EMPTY);
    }

    public ContractMethod(String name, boolean isConstant,
                          ContractParametersTypes inputTypes, ContractParametersTypes outputTypes) {
        this.id = MethodId.fromSignature(name, inputTypes.toCanonicalNames());
        this.name = Objects.requireNonNull(name);
        this.isConstant = isConstant;
        this.inputTypes = Objects.requireNonNull(inputTypes);
        this.outputTypes = Objects.requireNonNull(outputTypes);
    }

    /**
     * @return the methods id
     */
    public MethodId getId() {
        return id;
    }

    /**
     * @return the method name
     */
    public String getName() {
        return name;
    }

    /**
     * @return {@code true} if this method change contract's state,
     * otherwise {@code false}
     *
     * @see Contract
     */
    public boolean isConstant() {
        return isConstant;
    }

    /**
     * @return the method's expect parameters
     */
    public ContractParametersTypes getInputTypes() {
        return inputTypes;
    }

    /**
     * @return the method's return parameters
     */
    public ContractParametersTypes getOutputTypes() {
        return outputTypes;
    }

    /**
     * Encode call data, so you can call the contract through some other means (for example, through RPC).
     *
     * @param args parameters of the call
     * @return {@link HexData} encoded call
     *
     * @see #encodeCall(Collection)
     * @see #decodeResponse(HexData)
     */
    public HexData encodeCall(Object... args) {
        return encodeCall(Arrays.asList(args));
    }

    /**
     * Encode call data, so you can call the contract through some other means (for example, through RPC).
     *
     * <p><b>Example:</b> <code>baz(uint32,bool)</code> with arguments <tt>(69, true)</tt> becomes
     * <tt>0xcdcd77c000000000000000000000000000000000000000000000000000000000000000450000000000000000000000000000000000000000000000000000000000000001</tt>
     *
     * @param args arguments values of the call
     * @return {@link HexData} encoded call
     *
     * @see #encodeCall(Object...)
     * @see #decodeResponse(HexData)
     *
     * @see <a href="https://github.com/ethereum/wiki/wiki/Ethereum-Contract-ABI#function-selector-and-argument-encoding">Function Selector and Argument Encoding</a>
     * @see <a href="https://github.com/ethereum/wiki/wiki/Ethereum-Contract-ABI#examples">Examples</a>
     */
    public HexData encodeCall(Collection<?> args) {
        return id.concat(inputTypes.encode(args));
    }

    /**
     * Decode contract method response {@link HexData}.
     *
     * @param data a hex data
     * @return a list of decoded objects
     *
     * @see #encodeCall(Object...)
     * @see #encodeCall(Collection)
     */
    public List<?> decodeResponse(HexData data) {
        return outputTypes.decode(data);
    }

    /**
     * ABI encoded contract method signature.
     *
     * @return a string
     */
    public String toAbi() {
        return String.format("%s(%s)", name, inputTypes.toAbi())
                + (outputTypes.isEmpty() ? "" : String.format(":(%s)", outputTypes.toAbi()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;

        if (Objects.isNull(obj)) return false;

        if (!Objects.equals(getClass(), obj.getClass()))
            return false;

        ContractMethod other = (ContractMethod) obj;

        return Objects.equals(id, other.id);
    }

    @Override
    public String toString() {
        return toAbi();
    }
}
