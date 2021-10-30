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

import static java.lang.String.format;
import static org.weakref.int128.Primitives.ifNegative;
import static org.weakref.int128.Primitives.unsignedBorrow;
import static org.weakref.int128.Primitives.unsignedCarry;
import static org.weakref.int128.Primitives.unsignedMultiplyHigh;

public record Int128(long high, long low)
{
    public static final Int128 ZERO = new Int128(0, 0);
    public static final Int128 ONE = new Int128(0, 1);
    public static final Int128 MAX_VALUE = new Int128(0x7FFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL);
    public static final Int128 MIN_VALUE = new Int128(0x8000000000000000L, 0x0000000000000000L);

    private static final VarHandle LONG_VIEW = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);

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

    /**
     * @return a random value between <code>2^(magnitude - 1)</code> (inclusive) and <code>2^magnitude</code> (exclusive).
     *         Returns 0 if <code>magnitude</code> is 0.
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
        Int128 base = Int128.valueOf(1).shiftLeft(magnitude - 1);
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

        Int128 m = bound.decrement();
        Int128 result = random();
        if (bound.bitCount() == 1) {
            // power of two
            result = and(result, m);
        }
        else {
            Int128 value = result.shiftRightUnsigned(1);
            while (true) {
                result = remainder(value, bound);
                if (!subtract(add(value, m), result).isNegative()) {
                    break;
                }
                value = random().shiftRightUnsigned(1);
            }
        }

        return result;
    }

    /**
     * Whether the value fits in a <code>long</code> without truncation.
     */
    public boolean inLongRange()
    {
        return Primitives.inLongRange(high, low);
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
        return Primitives.isZero(high, low);
    }

    public Int128 abs()
    {
        if (high < 0) {
            return negate();
        }

        return this;
    }

    public Int128 absExact()
    {
        if (high < 0) {
            return negateExact();
        }

        return this;
    }

    public Int128 increment()
    {
        return new Int128(
                Primitives.incrementHigh(high, low),
                Primitives.incrementLow(high, low));
    }

    public Int128 incrementExact()
    {
        if (this.equals(MAX_VALUE)) {
            throw new ArithmeticException("Integer overflow");
        }

        return increment();
    }

    public Int128 decrement()
    {
        return new Int128(
                Primitives.decrementHigh(high, low),
                Primitives.decrementLow(high, low));
    }

    public Int128 decrementExact()
    {
        if (this.equals(MIN_VALUE)) {
            throw new ArithmeticException("Integer overflow");
        }

        return decrement();
    }

    public Int128 negate()
    {
        return new Int128(
                Primitives.negateHigh(high, low),
                Primitives.negateLow(high, low));
    }

    public Int128 negateExact()
    {
        long high = Primitives.negateHigh(this.high, this.low);
        long low = Primitives.negateLow(this.high, this.low);

        if (high == this.high) {
            throw new ArithmeticException("overflow");
        }

        return new Int128(high, low);
    }

    public static Int128 add(Int128 a, Int128 b)
    {
        return new Int128(
                Primitives.addHigh(a.high, a.low, b.high, b.low),
                Primitives.addLow(a.high, a.low, b.high, b.low));
    }

    public static Int128 addExact(Int128 a, Int128 b)
    {
        long low = Primitives.addLow(a.high, a.low, b.high, b.low);
        long high = Primitives.addHigh(a.high, a.low, b.high, b.low);

        // HD 2-13 Overflow iff both arguments have the opposite sign of the result
        if (((high ^ a.high) & (high ^ b.high)) < 0) {
            throw new ArithmeticException("Integer overflow");
        }

        return new Int128(high, low);
    }

    public static Int128 subtract(Int128 a, Int128 b)
    {
        return new Int128(
                Primitives.subtractHigh(a.high, a.low, b.high, b.low),
                Primitives.subtractLow(a.high, a.low, b.high, b.low));
    }

    public static Int128 subtractExact(Int128 a, Int128 b)
    {
        long high = Primitives.subtractHigh(a.high, a.low, b.high, b.low);
        long low = Primitives.subtractLow(a.high, a.low, b.high, b.low);

        // HD 2-13 Overflow iff the arguments have different signs and
        // the sign of the result is different from the sign of x
        if (((a.high ^ b.high) & (a.high ^ high)) < 0) {
            throw new ArithmeticException("Integer overflow");
        }

        return new Int128(high, low);
    }

    public static Int128 multiply(Int128 a, Int128 b)
    {
        long aLow = a.low();
        long aHigh = a.high();
        long bLow = b.low();
        long bHigh = b.high();

        long z1High = unsignedMultiplyHigh(aLow, bLow);
        long z1Low = aLow * bLow;
        long z2Low = aLow * bHigh;
        long z3Low = aHigh * bLow;

        long resultLow = z1Low;
        long resultHigh = z1High + z2Low + z3Low;

        return new Int128(resultHigh, resultLow);
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
        boolean aInLongRange = Primitives.inLongRange(aHigh, aLow);
        boolean bInLongRange = Primitives.inLongRange(bHigh, bLow);

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

        return !Primitives.inLongRange(tHigh, tLow);
    }

    public static int compare(Int128 a, Int128 b)
    {
        return Primitives.compare(a.high, a.low, b.high, b.low);
    }

    public static Int128 divide(Int128 dividend, Int128 divisor)
    {
        MutableInt128 quotient = new MutableInt128();
        MutableInt128 remainder = new MutableInt128();
        divide(dividend, divisor, quotient, remainder);

        return quotient.get();
    }

    public static Int128 remainder(Int128 dividend, Int128 divisor)
    {
        MutableInt128 quotient = new MutableInt128();
        MutableInt128 remainder = new MutableInt128();
        divide(dividend, divisor, quotient, remainder);

        return remainder.get();
    }

    public static DivisionResult divideWithRemainder(Int128 dividend, Int128 divisor)
    {
        MutableInt128 quotient = new MutableInt128();
        MutableInt128 remainder = new MutableInt128();
        divide(dividend, divisor, quotient, remainder);

        return new DivisionResult(quotient.get(), remainder.get());
    }

    public static Int128 and(Int128 a, Int128 b)
    {
        return new Int128(a.high & b.high, a.low & b.low);
    }

    public static Int128 or(Int128 a, Int128 b)
    {
        return new Int128(a.high | b.high, a.low | b.low);
    }

    public static Int128 xor(Int128 a, Int128 b)
    {
        return new Int128(a.high ^ b.high, a.low ^ b.low);
    }

    public Int128 not()
    {
        return new Int128(~high, ~low);
    }

    public Int128 shiftRight(int shift)
    {
        return new Int128(
                Primitives.shiftRightHigh(high, low, shift),
                Primitives.shiftRightLow(high, low, shift));
    }

    public Int128 shiftLeft(int shift)
    {
        return new Int128(
                Primitives.shiftLeftHigh(high, low, shift),
                Primitives.shiftLeftLow(high, low, shift));
    }

    public Int128 shiftRightUnsigned(int shift)
    {
        return new Int128(
                Primitives.shiftRightUnsignedHigh(high, low, shift),
                Primitives.shiftRightUnsignedLow(high, low, shift));
    }

    public int numberOfLeadingZeros()
    {
        return Primitives.numberOfLeadingZeros(high, low);
    }

    public int numberOfTrailingZeros()
    {
        return Primitives.numberOfTrailingZeros(high, low);
    }

    public int bitCount()
    {
        return Long.bitCount(high) + Long.bitCount(low);
    }

    @Override
    public String toString()
    {
        return format("0x%016XL 0x%016XL: %s", high, low, toBigInteger());
    }

    public BigInteger toBigInteger()
    {
        return new BigInteger(toByteArray());
    }

    public byte[] toByteArray()
    {
        byte[] bytes = new byte[16];
        toByteArray(bytes, 0);
        return bytes;
    }

    public void toByteArray(byte[] bytes, int offset)
    {
        LONG_VIEW.set(bytes, offset, high);
        LONG_VIEW.set(bytes, offset + Long.BYTES, low);
    }

    public long toLong()
    {
        return low;
    }

    public long toLongExact()
    {
        if (!inLongRange()) {
            throw new ArithmeticException("value too big for a long: " + toBigInteger());
        }

        return low;
    }

    private static void divide(Int128 dividend, Int128 divisor, MutableInt128 quotient, MutableInt128 remainder)
    {
        long dividendHigh = dividend.high;
        long dividendLow = dividend.low;
        long divisorHigh = divisor.high;
        long divisorLow = divisor.low;

        boolean dividendNegative = dividend.isNegative();
        boolean divisorNegative = divisor.isNegative();

        // for self assignments
        long tmpHigh;
        long tmpLow;

        if (dividendNegative) {
            tmpHigh = Primitives.negateHigh(dividendHigh, dividendLow);
            tmpLow = Primitives.negateLow(dividendHigh, dividendLow);
            dividendHigh = tmpHigh;
            dividendLow = tmpLow;
        }

        if (divisorNegative) {
            tmpHigh = Primitives.negateHigh(divisorHigh, divisorLow);
            tmpLow = Primitives.negateLow(divisorHigh, divisorLow);
            divisorHigh = tmpHigh;
            divisorLow = tmpLow;
        }

        dividePositive(dividendHigh, dividendLow, divisorHigh, divisorLow, quotient, remainder);

        boolean resultNegative = dividendNegative ^ divisorNegative;
        if (resultNegative) {
            tmpHigh = Primitives.negateHigh(quotient.high(), quotient.low());
            tmpLow = Primitives.negateLow(quotient.high(), quotient.low());
            quotient.set(tmpHigh, tmpLow);
        }

        if (dividendNegative) {
            // negate remainder
            tmpHigh = Primitives.negateHigh(remainder.high(), remainder.low());
            tmpLow = Primitives.negateLow(remainder.high(), remainder.low());
            remainder.set(tmpHigh, tmpLow);
        }
    }

    private static void dividePositive(long dividendHigh, long dividendLow, long divisorHigh, long divisorLow, MutableInt128 quotient, MutableInt128 remainder)
    {
        int dividendLeadingZeros = Primitives.numberOfLeadingZeros(dividendHigh, dividendLow);
        int divisorLeadingZeros = Primitives.numberOfLeadingZeros(divisorHigh, divisorLow);
        int divisorTrailingZeros = Primitives.numberOfTrailingZeros(divisorHigh, divisorLow);

        int comparison = Primitives.compareUnsigned(dividendHigh, dividendLow, divisorHigh, divisorLow);
        if (comparison < 0) {
            quotient.set(ZERO);
            remainder.set(dividendHigh, dividendLow);
            return;
        }
        else if (comparison == 0) {
            quotient.set(ONE);
            remainder.set(ZERO);
            return;
        }

        if (divisorLeadingZeros == 128) {
            throw new ArithmeticException("Divide by zero");
        }
        else if ((dividendHigh | divisorHigh) == 0) {
            // dividend and divisor fit in an unsigned
            quotient.set(0, Long.divideUnsigned(dividendLow, divisorLow));
            remainder.set(0, Long.remainderUnsigned(dividendLow, divisorLow));
            return;
        }
        else if (divisorLeadingZeros == 127) {
            // divisor is 1
            quotient.set(dividendHigh, dividendLow);
            remainder.set(ZERO);
            return;
        }
        else if ((divisorTrailingZeros + divisorLeadingZeros) == 127) {
            // only one bit set (i.e., power of 2), so just shift

            //  quotient = dividend >>> divisorTrailingZeros
            quotient.set(
                    Primitives.shiftRightUnsignedHigh(dividendHigh, dividendLow, divisorTrailingZeros),
                    Primitives.shiftRightUnsignedLow(dividendHigh, dividendLow, divisorTrailingZeros));

            //  remainder = dividend & (divisor - 1)
            long dLow = Primitives.decrementLow(divisorHigh, divisorLow);
            long dHigh = Primitives.decrementHigh(divisorHigh, divisorLow);

            // and
            remainder.set(
                    Primitives.andHigh(dividendHigh, dividendLow, dHigh, dLow),
                    Primitives.andLow(dividendHigh, dividendLow, dHigh, dLow));
            return;
        }

        if (divisorLeadingZeros - dividendLeadingZeros > 15) { // fastDivide when the values differ by this many orders of magnitude
            fastDivide(dividendHigh, dividendLow, divisorHigh, divisorLow, quotient, remainder);
        }
        else {
            binaryDivide(dividendHigh, dividendLow, divisorHigh, divisorLow, quotient, remainder);
        }
    }

    private static void binaryDivide(long dividendHigh, long dividendLow, long divisorHigh, long divisorLow, MutableInt128 quotient, MutableInt128 remainder)
    {
        int shift = Primitives.numberOfLeadingZeros(divisorHigh, divisorLow) - Primitives.numberOfLeadingZeros(dividendHigh, dividendLow);

        // for self assignments
        long tmpHigh;
        long tmpLow;

        // divisor = divisor << shift
        tmpHigh = Primitives.shiftLeftHigh(divisorHigh, divisorLow, shift);
        tmpLow = Primitives.shiftLeftLow(divisorHigh, divisorLow, shift);
        divisorHigh = tmpHigh;
        divisorLow = tmpLow;

        long quotientHigh = 0;
        long quotientLow = 0;

        do {
            // quotient = quotient << 1
            tmpHigh = Primitives.shiftLeftHigh(quotientHigh, quotientLow, 1);
            tmpLow = Primitives.shiftLeftLow(quotientHigh, quotientLow, 1);
            quotientHigh = tmpHigh;
            quotientLow = tmpLow;

            // if (dividend >= divisor)
            int comparison = Primitives.compareUnsigned(dividendHigh, dividendLow, divisorHigh, divisorLow);
            if (comparison >= 0) {
                // dividend = dividend - divisor
                tmpHigh = Primitives.subtractHigh(dividendHigh, dividendLow, divisorHigh, divisorLow);
                tmpLow = Primitives.subtractLow(dividendHigh, dividendLow, divisorHigh, divisorLow);
                dividendHigh = tmpHigh;
                dividendLow = tmpLow;

                // quotient = quotient | 1
                quotientLow = quotientLow | 1;
            }

            // divisor = divisor >>> 1
            tmpHigh = Primitives.shiftRightUnsignedHigh(divisorHigh, divisorLow, 1);
            tmpLow = Primitives.shiftRightUnsignedLow(divisorHigh, divisorLow, 1);
            divisorHigh = tmpHigh;
            divisorLow = tmpLow;
        }
        while (shift-- != 0);

        quotient.set(quotientHigh, quotientLow);
        remainder.set(dividendHigh, dividendLow);
    }

    private static void fastDivide(long dividendHigh, long dividendLow, long divisorHigh, long divisorLow, MutableInt128 quotient, MutableInt128 remainder)
    {
        if (divisorHigh == 0) {
            if (Long.compareUnsigned(dividendHigh, divisorLow) < 0) {
                quotient.high(0);
                remainder.high(0);
                divide128by64(dividendHigh, dividendLow, divisorLow, quotient, remainder);
            }
            else {
                quotient.high(Long.divideUnsigned(dividendHigh, divisorLow));
                remainder.high(0);
                divide128by64(Long.remainderUnsigned(dividendHigh, divisorLow), dividendLow, divisorLow, quotient, remainder);
            }
        }
        else {
            // used for self assignments
            long tempHigh;
            long tempLow;

            int n = Long.numberOfLeadingZeros(divisorHigh);

            // v1 = divisor << n
            long v1High = Primitives.shiftLeftHigh(divisorHigh, divisorLow, n);

            // u1 = dividend >>> 1
            long u1High = Primitives.shiftRightUnsignedHigh(dividendHigh, dividendLow, 1);
            long u1Low = Primitives.shiftRightUnsignedLow(dividendHigh, dividendLow, 1);

            divide128by64(u1High, u1Low, v1High, quotient, remainder);

            long q1High = 0;
            long q1Low = quotient.low();

            // q1 = q1 >>> (63 - n)
            tempLow = Primitives.shiftRightUnsignedLow(q1High, q1Low, 63 - n);
            tempHigh = Primitives.shiftRightUnsignedHigh(q1High, q1Low, 63 - n);
            q1Low = tempLow;
            q1High = tempHigh;

            // if (q1 != 0)
            if (!Primitives.isZero(q1High, q1Low)) {
                // q1--
                tempLow = Primitives.decrementLow(q1High, q1Low);
                tempHigh = Primitives.decrementHigh(q1High, q1Low);
                q1Low = tempLow;
                q1High = tempHigh;
            }

            long quotientHigh = q1High;
            long quotientLow = q1Low;

            // r = dividend - q1 * divisor
            long productHigh = Primitives.multiplyHigh(q1High, q1Low, divisorHigh, divisorLow);
            long productLow = Primitives.multiplyLow(q1High, q1Low, divisorHigh, divisorLow);

            long remainderHigh = Primitives.subtractHigh(dividendHigh, dividendLow, productHigh, productLow);
            long remainderLow = Primitives.subtractLow(dividendHigh, dividendLow, productHigh, productLow);

            if (Primitives.compare(remainderHigh, remainderLow, divisorHigh, divisorLow) >= 0) {
                // quotient++
                tempLow = Primitives.incrementLow(quotientHigh, quotientLow);
                tempHigh = Primitives.incrementHigh(quotientHigh, quotientLow);
                quotientLow = tempLow;
                quotientHigh = tempHigh;

                tempLow = Primitives.subtractLow(remainderHigh, remainderLow, divisorHigh, divisorLow);
                tempHigh = Primitives.subtractHigh(remainderHigh, remainderLow, divisorHigh, divisorLow);
                remainderHigh = tempHigh;
                remainderLow = tempLow;
            }

            quotient.set(quotientHigh, quotientLow);
            remainder.set(remainderHigh, remainderLow);
        }
    }

    private static void divide128by64(long high, long low, long divisor, MutableInt128 quotient, MutableInt128 remainder)
    {
        int shift = Long.numberOfLeadingZeros(divisor);
        if (shift != 0) {
            divisor <<= shift;
            high <<= shift;
            high |= low >>> (64 - shift);
            low <<= shift;
        }

        long divisorHigh = divisor >>> 32;
        long divisorLow = divisor & 0xFFFFFFFFL;
        long lowHigh = low >>> 32;
        long lowLow = low & 0xFFFFFFFFL;

        // Compute high quotient digit.
        long quotientHigh = Long.divideUnsigned(high, divisorHigh);
        long rhat = Long.remainderUnsigned(high, divisorHigh);

        // qhat >>> 32 == qhat > base
        while ((quotientHigh >>> 32) != 0 || Long.compareUnsigned(quotientHigh * divisorLow, (rhat << 32) | lowHigh) > 0) {
            quotientHigh -= 1;
            rhat += divisorHigh;
            if ((rhat >>> 32) != 0) {
                break;
            }
        }

        long uhat = ((high << 32) | lowHigh) - quotientHigh * divisor;

        // Compute low quotient digit.
        long quotientLow = Long.divideUnsigned(uhat, divisorHigh);
        rhat = Long.remainderUnsigned(uhat, divisorHigh);

        while ((quotientLow >>> 32) != 0 || Long.compareUnsigned(quotientLow * divisorLow, ((rhat << 32) | lowLow)) > 0) {
            quotientLow -= 1;
            rhat += divisorHigh;
            if ((rhat >>> 32) != 0) {
                break;
            }
        }

        quotient.low(quotientHigh << 32 | quotientLow);
        remainder.low((uhat << 32 | lowLow) - quotientLow * divisor >>> shift);
    }

    public record DivisionResult(Int128 quotient, Int128 remainder) {}
}
