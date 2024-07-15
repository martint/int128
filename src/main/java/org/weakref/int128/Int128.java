/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.weakref.int128;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.unsignedMultiplyHigh;
import static org.weakref.int128.MoreMath.ifNegative;
import static org.weakref.int128.MoreMath.unsignedBorrow;
import static org.weakref.int128.MoreMath.unsignedCarry;

public record Int128(long high, long low)
        implements Comparable<Int128>
{
    public static final Int128 ZERO = new Int128(0, 0);
    public static final Int128 ONE = new Int128(0, 1);
    public static final Int128 MAX_VALUE = new Int128(0x7FFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
    public static final Int128 MIN_VALUE = new Int128(0x8000000000000000L, 0x0000000000000000L);

    public static final int SIZE = 128;
    public static final int BYTES = SIZE / 8;

    private static final VarHandle BIG_ENDIAN_LONG_VIEW = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);

    /**
     * Whether the value fits in a <code>long</code> without truncation.
     */
    public boolean inLongRange()
    {
        return Int128Math.inLongRange(high, low);
    }

    /**
     * Whether the value is < 0.
     */
    public boolean isNegative()
    {
        return high < 0;
    }

    /**
     * Whether the value is > 0.
     */
    public boolean isPositive()
    {
        return high > 0 || (high == 0 && low != 0);
    }

    /**
     * Whether the value is equal to 0.
     */
    public boolean isZero()
    {
        return Int128Math.isZero(high, low);
    }

    // --- conversion functions

    public static Int128 valueOf(BigInteger value)
    {
        long low = value.longValue();
        long high = value.shiftRight(64).longValueExact();

        return new Int128(high, low);
    }

    public static Int128 valueOf(long value)
    {
        return new Int128(value >> 63, value);
    }

    public static Int128 valueOf(String value)
    {
        return Int128.valueOf(new BigInteger(value));
    }

    public static Int128 fromBigEndian(byte[] bytes)
    {
        if (bytes.length >= 16) {
            int offset = bytes.length - Long.BYTES;
            long low = (long) BIG_ENDIAN_LONG_VIEW.get(bytes, offset);

            offset -= Long.BYTES;
            long high = (long) BIG_ENDIAN_LONG_VIEW.get(bytes, offset);

            for (int i = 0; i < offset; i++) {
                if (bytes[i] != (high >> 63)) {
                    throw new ArithmeticException("Overflow");
                }
            }

            return Int128.valueOf(high, low);
        }
        else if (bytes.length > 8) {
            // read the last 8 bytes into low
            int offset = bytes.length - Long.BYTES;
            long low = (long) BIG_ENDIAN_LONG_VIEW.get(bytes, offset);

            // At this point, we're guaranteed to have between 9 and 15 bytes available.
            // Read 8 bytes into high, starting at offset 0. There will be some over-read
            // of bytes belonging to low, so adjust by shifting them out
            long high = (long) BIG_ENDIAN_LONG_VIEW.get(bytes, 0);
            offset -= Long.BYTES;
            high >>= (-offset * Byte.SIZE);

            return Int128.valueOf(high, low);
        }
        else if (bytes.length == 8) {
            long low = (long) BIG_ENDIAN_LONG_VIEW.get(bytes, 0);
            long high = (low >> 63);

            return Int128.valueOf(high, low);
        }
        else {
            long high = (bytes[0] >> 7);
            long low = high;
            for (int i = 0; i < bytes.length; i++) {
                low = (low << 8) | (bytes[i] & 0xFF);
            }

            return Int128.valueOf(high, low);
        }
    }

    public static Int128 valueOf(long high, long low)
    {
        return new Int128(high, low);
    }

    public BigInteger toBigInteger()
    {
        return new BigInteger(toBigEndianBytes());
    }

    public byte[] toBigEndianBytes()
    {
        byte[] bytes = new byte[16];
        toBigEndianBytes(bytes, 0);
        return bytes;
    }

    public void toBigEndianBytes(byte[] bytes, int offset)
    {
        BIG_ENDIAN_LONG_VIEW.set(bytes, offset, high);
        BIG_ENDIAN_LONG_VIEW.set(bytes, offset + Long.BYTES, low);
    }

    public long toLong()
    {
        return low;
    }

    public long toLongExact()
    {
        if (!Int128Math.inLongRange(high, low)) {
            throw new ArithmeticException("value too big for a long: " + toBigInteger());
        }

        return low;
    }

    // --- Logical operations

    public static int compare(Int128 a, Int128 b)
    {
        return Int128Math.compare(a.high(), a.low(), b.high(), b.low());
    }

    // --- Bitwise operations

    public static Int128 and(Int128 a, Int128 b)
    {
        return new Int128(
                Int128Math.andHigh(a.high(), a.low(), b.high(), b.low()),
                Int128Math.andLow(a.high(), a.low(), b.high(), b.low()));
    }

    public static Int128 or(Int128 a, Int128 b)
    {
        return new Int128(
                Int128Math.orHigh(a.high(), a.low(), b.high(), b.low()),
                Int128Math.orLow(a.high(), a.low(), b.high(), b.low()));
    }

    public static Int128 xor(Int128 a, Int128 b)
    {
        return new Int128(
                Int128Math.xorHigh(a.high(), a.low(), b.high(), b.low()),
                Int128Math.xorLow(a.high(), a.low(), b.high(), b.low()));
    }

    public static Int128 not(Int128 value)
    {
        return new Int128(
                Int128Math.notHigh(value.high(), value.low()),
                Int128Math.notLow(value.high(), value.low()));
    }

    public static Int128 shiftRight(Int128 value, int shift)
    {
        return new Int128(
                Int128Math.shiftRightHigh(value.high(), value.low(), shift),
                Int128Math.shiftRightLow(value.high(), value.low(), shift));
    }

    public static Int128 shiftLeft(Int128 value, int shift)
    {
        return new Int128(
                Int128Math.shiftLeftHigh(value.high(), value.low(), shift),
                Int128Math.shiftLeftLow(value.high(), value.low(), shift));
    }

    public static Int128 shiftRightUnsigned(Int128 value, int shift)
    {
        return new Int128(
                Int128Math.shiftRightUnsignedHigh(value.high(), value.low(), shift),
                Int128Math.shiftRightUnsignedLow(value.high(), value.low(), shift));
    }

    public static int numberOfLeadingZeros(Int128 value)
    {
        return Int128Math.numberOfLeadingZeros(value.high(), value.low());
    }

    public static int numberOfTrailingZeros(Int128 value)
    {
        return Int128Math.numberOfTrailingZeros(value.high(), value.low());
    }

    public static int bitCount(Int128 value)
    {
        return Int128Math.bitCount(value.high(), value.low());
    }

    // --- Arithmetic operations

    public static Int128 add(Int128 a, Int128 b)
    {
        return new Int128(
                Int128Math.addHigh(a.high(), a.low(), b.high(), b.low()),
                Int128Math.addLow(a.high(), a.low(), b.high(), b.low()));
    }

    public static Int128 addExact(Int128 a, Int128 b)
    {
        return new Int128(
                Int128Math.addHighExact(a.high(), a.low(), b.high(), b.low()),
                Int128Math.addLowExact(a.high(), a.low(), b.high(), b.low()));
    }

    public static Int128 subtract(Int128 a, Int128 b)
    {
        return new Int128(
                Int128Math.subtractHigh(a.high(), a.low(), b.high(), b.low()),
                Int128Math.subtractLow(a.high(), a.low(), b.high(), b.low()));
    }

    public static Int128 subtractExact(Int128 a, Int128 b)
    {
        return new Int128(
                Int128Math.subtractHighExact(a.high(), a.low(), b.high(), b.low()),
                Int128Math.subtractLowExact(a.high(), a.low(), b.high(), b.low()));
    }

    public static Int128 abs(Int128 value)
    {
//        long mask = high >> 63;
//
//        long borrow = (low | -low) >>> 63; // low != 0 ? 1 : 0
//        long tmpHigh = (high ^ mask) - mask - (borrow & mask);
//        long tmpLow = (low ^ mask) - mask;
//
//        return new Int128(tmpHigh, tmpLow);

        if (value.isNegative()) {
            return negate(value);
        }

        return value;
    }

    public static Int128 absExact(Int128 value)
    {
//        // HD 2-4
//        long high = value.high();
//        long low = value.low();
//
//        if (high == Int128.MIN_VALUE.high() && low == Int128.MIN_VALUE.low()) {
//            throw new ArithmeticException("Overflow");
//        }
//
//        long mask = high >> 63;
//
//        long borrow = (low | -low) >>> 63; // low != 0 ? 1 : 0
//        long tmpHigh = (high ^ mask) - mask - (borrow & mask);
//        long tmpLow = (low ^ mask) - mask;
//
//        return Int128.valueOf(tmpHigh, tmpLow);

        if (value.isNegative()) {
            return negateExact(value);
        }

        return value;
    }

    public static Int128 increment(Int128 value)
    {
        return new Int128(
                Int128Math.incrementHigh(value.high(), value.low()),
                Int128Math.incrementLow(value.high(), value.low()));
    }

    public static Int128 incrementExact(Int128 value)
    {
        if (value.equals(MAX_VALUE)) {
            throw new ArithmeticException("Integer overflow");
        }

        return increment(value);
    }

    public static Int128 decrement(Int128 value)
    {
        return new Int128(
                Int128Math.decrementHigh(value.high(), value.low()),
                Int128Math.decrementLow(value.high(), value.low()));
    }

    public static Int128 decrementExact(Int128 value)
    {
        if (value.equals(MIN_VALUE)) {
            throw new ArithmeticException("Integer overflow");
        }

        return decrement(value);
    }

    public static Int128 negate(Int128 value)
    {
        return new Int128(
                Int128Math.negateHigh(value.high(), value.low()),
                Int128Math.negateLow(value.high(), value.low()));
    }

    public static Int128 negateExact(Int128 value)
    {
        if (value.equals(MIN_VALUE)) {
            throw new ArithmeticException("overflow");
        }

        return new Int128(
                Int128Math.negateHigh(value.high(), value.low()),
                Int128Math.negateLow(value.high(), value.low()));
    }

    public static Int128 multiply(Int128 a, Int128 b)
    {
        return new Int128(
                Int128Math.multiplyHigh(a.high(), a.low(), b.high(), b.low()),
                Int128Math.multiplyLow(a.high(), a.low(), b.high(), b.low()));
    }

    public static Int128 multiplyExact(Int128 a, Int128 b)
    {
        long aLow = a.low();
        long aHigh = a.high();
        long bLow = b.low();
        long bHigh = b.high();

        long z1High = unsignedMultiplyHigh(aLow, bLow);
        long z1Low = aLow * bLow;

        long z2High = unsignedMultiplyHigh(bHigh, aLow);
        long z2Low = aLow * bHigh;

        long z3High = unsignedMultiplyHigh(aHigh, bLow);
        long z3Low = aHigh * bLow;

        long resultLow = z1Low;
        long resultHigh = z1High + z2Low + z3Low;

        if (productOverflows(aHigh, aLow, bHigh, bLow, z1High, z2High, z2Low, z3High, z3Low, resultHigh)) {
            throw new ArithmeticException("overflow");
        }

        return new Int128(resultHigh, resultLow);
    }

    public static Int128 divide(Int128 dividend, Int128 divisor)
    {
        Int128Holder quotient = new Int128Holder();
        Int128Holder remainder = new Int128Holder();
        divide(dividend, divisor, quotient, remainder);

        return quotient.get();
    }

    public static Int128 remainder(Int128 dividend, Int128 divisor)
    {
        Int128Holder quotient = new Int128Holder();
        Int128Holder remainder = new Int128Holder();
        divide(dividend, divisor, quotient, remainder);

        return remainder.get();
    }

    private static boolean productOverflows(
            long aHigh,
            long aLow,
            long bHigh,
            long bLow,
            long z1High,
            long z2High,
            long z2Low,
            long z3High,
            long z3Low,
            long resultHigh)
    {
        boolean aInLongRange = Int128Math.inLongRange(aHigh, aLow);
        boolean bInLongRange = Int128Math.inLongRange(bHigh, bLow);

        if (aInLongRange && bInLongRange) {
            return false;
        }

        if (!aInLongRange && !bInLongRange) {
            return aHigh == bHigh && resultHigh <= 0
                    || aHigh != bHigh && resultHigh >= 0
                    || aHigh != 0 && aHigh != -1
                    || bHigh != 0 && bHigh != -1;
        }

        // If a fits in a long, z3High is effectively "0", so we only care about z2 for
        // checking whether z2 + z3 + z1High overflows.
        // Similarly, if b fits in a long, we only care about z3.
        long wHigh;
        long wLow;
        if (!aInLongRange) {
//            // correct z3 due to effects of computing the product of values in 2's complement representation
//            if (aHigh < 0) {
//                z3High -= bLow;
//            }
//
//            if (bLow < 0) {
//                z3High = z3High - aHigh - unsignedBorrow(z3Low, aLow);
//                z3Low = z3Low - aLow;
//            }
//
//            wHigh = z3High;
//            wLow= z3Low;
            wHigh = z3High - ifNegative(aHigh, bLow) - ifNegative(bLow, aHigh + unsignedBorrow(z3Low, aLow));
            wLow = z3Low - ifNegative(bLow, aLow);
        }
        else { // !bInLongRange
//            // correct z2 due to effects of computing the product of values in 2's complement representation
//            if (bHigh < 0) {
//                z2High -= aLow;
//            }
//
//            if (aLow < 0) {
//                z2High = z2High - bHigh - unsignedBorrow(z2Low, bLow);
//                z2Low = z2Low - bLow;
//            }
//
//            wHigh = z2High;
//            wLow= z2Low;
//
            wHigh = z2High - ifNegative(bHigh, aLow) - ifNegative(aLow, bHigh + unsignedBorrow(z2Low, bLow));
            wLow = z2Low - ifNegative(aLow, bLow);
        }

        // t = w + z1High
        long tLow = wLow + z1High;
        long tHigh = wHigh + unsignedCarry(wLow, z1High);

        return !Int128Math.inLongRange(tHigh, tLow);
    }

    public static void divide(Int128 dividend, Int128 divisor, Int128Holder quotient, Int128Holder remainder)
    {
        Int128Math.divide(dividend.high(), dividend.low(), divisor.high(), divisor.low(), quotient, remainder);
    }

    public static DivisionResult divideWithRemainder(Int128 dividend, Int128 divisor)
    {
        Int128Holder quotient = new Int128Holder();
        Int128Holder remainder = new Int128Holder();

        divide(dividend, divisor, quotient, remainder);

        return new DivisionResult(quotient.get(), remainder.get());
    }

    public static Int128 multiply(Int128 a, long b)
    {
        // TODO: optimize
        return multiply(a, valueOf(b));
    }

    /**
     * 64 x 64 -> 128
     */
    public static Int128 multiply(long a, long b)
    {
        // TODO: optimize
        return multiply(valueOf(a), valueOf(b));
    }

    /**
     * @return a random value between <code>2^(magnitude - 1)</code> (inclusive) and <code>2^magnitude</code> (exclusive).
     * Returns 0 if <code>magnitude</code> is 0.
     */
    public static Int128 random(int magnitude)
    {
        if (magnitude > 126 || magnitude < 0) {
            throw new IllegalArgumentException("Magnitude must be in [0, 126] range");
        }

        if (magnitude == 0) {
            return ZERO;
        }

        if (magnitude == 1) {
            return ONE;
        }

        // TODO: optimize
        Int128 base = shiftLeft(Int128.valueOf(1), magnitude - 1);
        Int128 value = random(base);
        return add(base, value);
    }

    /**
     * @return a random value between {@link #MIN_VALUE} (inclusive) and {@link #MAX_VALUE} (inclusive)
     */
    public static Int128 random()
    {
        return new Int128(ThreadLocalRandom.current().nextLong(), ThreadLocalRandom.current().nextLong());
    }

    /**
     * @return a random value between 0 (inclusive) and given bound (exclusive)
     */
    public static Int128 random(Int128 bound)
    {
        // Based on Random.nextLong(bound)

        // TODO optimize
        if (!bound.isPositive()) {
            throw new IllegalArgumentException("bound must be positive: " + bound);
        }

        Int128 m = decrement(bound);
        Int128 result = random();
        if (bitCount(bound) == 1) {
            // power of two
            result = and(result, m);
        }
        else {
            Int128 value = shiftRightUnsigned(result, 1);
            while (true) {
                result = remainder(value, bound);
                if (!subtract(add(value, m), result).isNegative()) {
                    break;
                }
                value = shiftRightUnsigned(random(), 1);
            }
        }

        return result;
    }

    @Override
    public String toString()
    {
        return toString(10);
    }

    public String toString(int radix)
    {
        // TODO: format directly
        //   https://frinklang.org/frinksamp/baseconv.frink
        return toBigInteger().toString(radix);
    }

    @Override
    public int compareTo(Int128 other)
    {
        return compare(this, other);
    }

    public record DivisionResult(Int128 quotient, Int128 remainder) {}
}
