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

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.weakref.int128.Int128.MAX_VALUE;
import static org.weakref.int128.Int128.MIN_VALUE;
import static org.weakref.int128.Int128.ONE;
import static org.weakref.int128.Int128.ZERO;
import static org.weakref.int128.Int128.add;
import static org.weakref.int128.Int128.addExact;
import static org.weakref.int128.Int128.and;
import static org.weakref.int128.Int128.compare;
import static org.weakref.int128.Int128.multiply;
import static org.weakref.int128.Int128.multiplyExact;
import static org.weakref.int128.Int128.or;
import static org.weakref.int128.Int128.subtract;
import static org.weakref.int128.Int128.subtractExact;
import static org.weakref.int128.Int128.xor;
import static org.weakref.int128.TestInt128.DivideAssert.divide;

public class TestInt128
{
    @Test
    public void testToBigInteger()
    {
        assertThat(ZERO.toBigInteger())
                .isEqualTo(BigInteger.ZERO);

        assertThat(ONE.toBigInteger())
                .isEqualTo(BigInteger.ONE);

        assertThat(MAX_VALUE.toBigInteger())
                .isEqualTo(BigInteger.TWO.pow(127).subtract(BigInteger.ONE));

        assertThat(MIN_VALUE.toBigInteger())
                .isEqualTo(BigInteger.TWO.pow(127)
                        .subtract(BigInteger.ONE)
                        .negate()
                        .subtract(BigInteger.ONE));
    }

    @Test
    public void testInLongRange()
    {
        assertThat(Int128.valueOf(Long.MAX_VALUE).inLongRange())
                .isTrue();

        assertThat(Int128.valueOf(Long.MIN_VALUE).inLongRange())
                .isTrue();

        assertThat(Int128.add(Int128.valueOf(Long.MAX_VALUE), ONE).inLongRange())
                .isFalse();

        assertThat(Int128.subtract(Int128.valueOf(Long.MIN_VALUE), ONE).inLongRange())
                .isFalse();

        assertThat(MAX_VALUE.inLongRange())
                .isFalse();

        assertThat(MIN_VALUE.inLongRange())
                .isFalse();

        assertThat(ZERO.inLongRange())
                .isTrue();
    }

    @Test
    public void testAdd()
    {
        assertThat(add(Int128.valueOf(0), Int128.valueOf(0)))
                .isEqualTo(ZERO);

        assertThat(add(MAX_VALUE, ONE))
                .isEqualTo(MIN_VALUE);

        assertThat(add(ZERO, ONE))
                .isEqualTo(ONE);

        assertThat(add(Int128.valueOf(Long.MAX_VALUE - 1), ONE))
                .isEqualTo(Int128.valueOf(Long.MAX_VALUE));

        assertThat(add(Int128.valueOf(-1), ONE))
                .isEqualTo(ZERO);

        assertThatThrownBy(() -> addExact(MAX_VALUE, ONE))
                .isInstanceOf(ArithmeticException.class);

        assertThat(addExact(Int128.valueOf(Long.MIN_VALUE + 1), Int128.valueOf(-1)))
                .isEqualTo(Int128.valueOf(Long.MIN_VALUE));

        assertThatThrownBy(() -> addExact(MAX_VALUE, MAX_VALUE))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    public void testSubtract()
    {
        assertThat(subtract(Int128.valueOf(0), Int128.valueOf(0)))
                .isEqualTo(ZERO);

        assertThat(subtract(MIN_VALUE, ONE))
                .isEqualTo(MAX_VALUE);

        assertThatThrownBy(() -> subtractExact(MIN_VALUE, ONE))
                .isInstanceOf(ArithmeticException.class);

        assertThatThrownBy(() -> subtractExact(MIN_VALUE, MAX_VALUE))
                .isInstanceOf(ArithmeticException.class);

        assertThat(subtractExact(Int128.valueOf(Long.MAX_VALUE - 1), Int128.valueOf(-1)))
                .isEqualTo(Int128.valueOf(Long.MAX_VALUE));
    }

    @Test
    public void testAbs()
    {
        assertThat(ZERO.abs())
                .isEqualTo(ZERO);

        assertThat(ONE.abs())
                .isEqualTo(ONE);

        assertThat(ONE.negate().abs())
                .isEqualTo(ONE);

        assertThat(MAX_VALUE.abs())
                .isEqualTo(MAX_VALUE);

        assertThat(MAX_VALUE.negate().abs())
                .isEqualTo(MAX_VALUE);

        assertThat(MIN_VALUE.abs())
                .isEqualTo(MIN_VALUE);

        assertThatThrownBy(MIN_VALUE::absExact)
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    public void testNegate()
    {
        assertThat(ZERO.negate())
                .isEqualTo(ZERO);

        assertThat(ONE.negate())
                .isEqualTo(Int128.valueOf(-1));

        assertThat(ONE.negateExact())
                .isEqualTo(Int128.valueOf(-1));

        assertThat(MAX_VALUE.negate())
                .isEqualTo(Int128.valueOf(MAX_VALUE.toBigInteger().negate()));

        assertThat(MAX_VALUE.negateExact())
                .isEqualTo(Int128.valueOf(MAX_VALUE.toBigInteger().negate()));

        assertThat(MIN_VALUE.negate())
                .isEqualTo(MIN_VALUE);

        assertThatThrownBy(MIN_VALUE::negateExact)
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    public void testMultiply()
    {
        assertThat(multiply(ZERO, ONE))
                .isEqualTo(ZERO);

        assertThat(multiply(ZERO, ONE.negate()))
                .isEqualTo(ZERO);

        assertThat(multiply(ONE, MAX_VALUE))
                .isEqualTo(MAX_VALUE);

        assertThat(multiply(ONE, MIN_VALUE))
                .isEqualTo(MIN_VALUE);

        assertThat(multiply(ONE, MAX_VALUE))
                .isEqualTo(MAX_VALUE);

        assertThat(multiply(ONE.negate(), MAX_VALUE))
                .isEqualTo(MAX_VALUE.negate());

        assertThat(multiply(ONE.negate(), MIN_VALUE))
                .describedAs("Overflow when negating MIN_VALUE")
                .isEqualTo(MIN_VALUE);

        assertThatThrownBy(() -> multiplyExact(ONE.negate(), MIN_VALUE))
                .isInstanceOf(ArithmeticException.class);

        assertThat(multiply(Int128.valueOf(Long.MAX_VALUE), Int128.valueOf(2)))
                .isEqualTo(Int128.valueOf(BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TWO)));

        assertThat(multiply(Int128.valueOf(Long.MAX_VALUE), Int128.valueOf(Long.MAX_VALUE)))
                .isEqualTo(Int128.valueOf(BigInteger.valueOf(Long.MAX_VALUE).pow(2)));

        assertThat(multiply(Int128.valueOf(Long.MIN_VALUE), Int128.valueOf(Long.MIN_VALUE)))
                .isEqualTo(Int128.valueOf(BigInteger.valueOf(Long.MIN_VALUE).pow(2)));

        assertThat(multiplyExact(Int128.valueOf("18446744073709551614"), Int128.valueOf(2)))
                .isEqualTo(Int128.valueOf("36893488147419103228"));
    }

    @Test
    public void testTwosComplementCorrectionsNoOverflow()
    {
        assertThat(multiplyExact(
                new Int128(0x0000000000000000L, 0xFFFFFFFFFFFFFFFFL),
                new Int128(0x0000000000000000L, 0x0000000000000002L)))
                .describedAs("aHigh positive, bLow positive")
                .isEqualTo(new Int128(0x0000000000000001L, 0xFFFFFFFFFFFFFFFEL));

        assertThat(multiplyExact(
                new Int128(0x0000000000000000L, 0xFFFFFFFFFFFFFFFFL),
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFEL)))
                .describedAs("aHigh positive, bLow negative")
                .isEqualTo(new Int128(0xFFFFFFFFFFFFFFFEL, 0x0000000000000002L));

        assertThat(multiplyExact(
                new Int128(0xFFFFFFFFFFFFFFFFL, 0x7FFFFFFFFFFFFFFFL),
                new Int128(0x0000000000000000L, 0x0000000000000002L)))
                .describedAs("aHigh negative, bLow positive")
                .isEqualTo(new Int128(0xFFFFFFFFFFFFFFFEL, 0xFFFFFFFFFFFFFFFEL));

        assertThat(multiplyExact(
                new Int128(0xFFFFFFFFFFFFFFFFL, 0x7FFFFFFFFFFFFFFFL),
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFEL)))
                .describedAs("aHigh negative, bLow negative")
                .isEqualTo(new Int128(0x0000000000000001L, 0x0000000000000002L));

        // Left fits in java long
        // Test bHigh vs aLow positive and negative combinations
        assertThat(multiplyExact(
                new Int128(0x0000000000000000L, 0x0000000000000002L),
                new Int128(0x0000000000000000L, 0xFFFFFFFFFFFFFFFFL)))
                .describedAs("bHigh positive, aLow positive")
                .isEqualTo(new Int128(0x0000000000000001L, 0xFFFFFFFFFFFFFFFEL));

        assertThat(multiplyExact(
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFEL),
                new Int128(0x0000000000000000L, 0xFFFFFFFFFFFFFFFFL)))
                .describedAs("bHigh positive, aLow negative")
                .isEqualTo(new Int128(0xFFFFFFFFFFFFFFFEL, 0x0000000000000002L));

        assertThat(multiplyExact(
                new Int128(0x0000000000000000L, 0x0000000000000002L),
                new Int128(0xFFFFFFFFFFFFFFFFL, 0x7FFFFFFFFFFFFFFFL)))
                .describedAs("bHigh negative, aLow positive")
                .isEqualTo(new Int128(0xFFFFFFFFFFFFFFFEL, 0xFFFFFFFFFFFFFFFEL));

        assertThat(multiplyExact(
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFEL),
                new Int128(0xFFFFFFFFFFFFFFFFL, 0x7FFFFFFFFFFFFFFFL)))
                .describedAs("bHigh negative, aLow negative")
                .isEqualTo(new Int128(0x0000000000000001L, 0x0000000000000002L));
    }

    @Test
    public void testTwosComplementCorrectionsOverflow()
    {
        // Right fits in java long
        // Test aHigh vs bLow positive and negative combinations
        assertThatThrownBy(() -> multiplyExact(
                new Int128(0x7FFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL),
                new Int128(0x0000000000000000L, 0x0000000000000002L)))
                .describedAs("aHigh positive, bLow positive, overflow")
                .isInstanceOf(ArithmeticException.class);

        assertThatThrownBy(() -> multiplyExact(
                new Int128(0x8000000000000000L, 0x7FFFFFFFFFFFFFFFL),
                new Int128(0x0000000000000000L, 0x0000000000000002L)))
                .describedAs("aHigh negative, bLow positive, overflow")
                .isInstanceOf(ArithmeticException.class);

        assertThatThrownBy(() -> multiplyExact(
                new Int128(0x8000000000000000L, 0x7FFFFFFFFFFFFFFFL),
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFEL)))
                .describedAs("aHigh negative, bLow negative, overflow")
                .isInstanceOf(ArithmeticException.class);

        assertThatThrownBy(() -> multiplyExact(
                new Int128(0x7FFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL),
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFEL)))
                .describedAs("aHigh positive, bLow negative, overflow")
                .isInstanceOf(ArithmeticException.class);

        // Left fits in java long
        // Test bHigh vs aLow positive and negative combinations
        assertThatThrownBy(() -> multiplyExact(
                new Int128(0x0000000000000000L, 0x0000000000000002L),
                new Int128(0x7FFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL)))
                .describedAs("bHigh positive, aLow positive, overflow")
                .isInstanceOf(ArithmeticException.class);

        assertThatThrownBy(() -> multiplyExact(
                new Int128(0x0000000000000000L, 0x0000000000000002L),
                new Int128(0x8000000000000000L, 0x7FFFFFFFFFFFFFFFL)))
                .describedAs("bHigh negative, aLow positive, overflow")
                .isInstanceOf(ArithmeticException.class);

        assertThatThrownBy(() -> multiplyExact(
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFEL),
                new Int128(0x8000000000000000L, 0x7FFFFFFFFFFFFFFFL)))
                .describedAs("bHigh negative, aLow negative, overflow")
                .isInstanceOf(ArithmeticException.class);

        assertThatThrownBy(() -> multiplyExact(
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFEL),
                new Int128(0x7FFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL)))
                .describedAs("bHigh positive, aLow negative, overflow")
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    public void testOverflow()
    {
        assertThatThrownBy(() ->
                multiplyExact(
                        new Int128(0xFFFFFFFFFFFFFFFEL, 0x0000000000000000L),
                        new Int128(0xFFFFFFFFFFFFFFFEL, 0x0000000000000000L)))
                .describedAs("aHigh == bHigh, resultHigh <= 0")
                .isInstanceOf(ArithmeticException.class);

        assertThatThrownBy(() ->
                multiplyExact(
                        new Int128(0xFFFFFFFFFFFFFFFEL, 0x0000000000000000L),
                        new Int128(0xFFFFFFFFFFFFFFFFL, 0x0000000000000000L)))
                .describedAs("aHigh != bHigh, resultHigh >= 0")
                .isInstanceOf(ArithmeticException.class);

        assertThatThrownBy(() ->
                multiplyExact(
                        new Int128(0xFFFFFFFFFFFFFFFEL, 0x0000000000000001L),
                        new Int128(0xFFFFFFFFFFFFFFFFL, 0x0000000000000000L)))
                .describedAs("aHigh != 0, aHigh != 1")
                .isInstanceOf(ArithmeticException.class);

        assertThatThrownBy(() ->
                multiplyExact(
                        new Int128(0xFFFFFFFFFFFFFFFFL, 0x0000000000000000L),
                        new Int128(0xFFFFFFFFFFFFFFFEL, 0x0000000000000001L)))
                .describedAs("bHigh != 0, bHigh != -1")
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    public void testBiggerThanLongButNoOverflow()
    {
        // Values that are outside of the range of signed long, but don't overflow when multiplied together
        Int128 minValueMinusOne = new Int128(0xFFFFFFFFFFFFFFFFL, 0x7FFFFFFFFFFFFFFFL); // Long.MIN_VALUE - 1
        Int128 maxValuePlusOne = new Int128(0x0000000000000000L, 0x8000000000000000L); // Long.MAX_VALUE + 1

        assertThat(Int128.multiplyExact(maxValuePlusOne, maxValuePlusOne))
                .isEqualTo(Int128.valueOf(maxValuePlusOne.toBigInteger().pow(2)));

        assertThat(Int128.multiplyExact(maxValuePlusOne, minValueMinusOne))
                .isEqualTo(Int128.valueOf(maxValuePlusOne.toBigInteger().multiply(minValueMinusOne.toBigInteger())));

        assertThat(Int128.multiplyExact(minValueMinusOne, minValueMinusOne))
                .isEqualTo(Int128.valueOf(minValueMinusOne.toBigInteger().multiply(minValueMinusOne.toBigInteger())));

        assertThat(multiplyExact(Int128.valueOf("13043817825332782212"), Int128.valueOf("13043817825332782212")))
                .isEqualTo(Int128.valueOf("170141183460469231722567801800623612944"));

        assertThat(multiplyExact(Int128.valueOf("-13043817825332782212"), Int128.valueOf("13043817825332782212")))
                .isEqualTo(Int128.valueOf("-170141183460469231722567801800623612944"));

        assertThat(multiplyExact(Int128.valueOf("13043817825332782212"), Int128.valueOf("-13043817825332782212")))
                .isEqualTo(Int128.valueOf("-170141183460469231722567801800623612944"));

        assertThat(multiplyExact(Int128.valueOf("-13043817825332782212"), Int128.valueOf("-13043817825332782212")))
                .isEqualTo(Int128.valueOf("170141183460469231722567801800623612944"));
    }

    @Test
    public void testShiftLeft()
    {
        for (int shift = 0; shift < 127; shift++) {
            assertThat(ONE.shiftLeft(shift))
                    .describedAs("<< " + shift)
                    .isEqualTo(Int128.valueOf(BigInteger.ONE.shiftLeft(shift)));
        }
    }

    @Test
    public void testShiftRight()
    {
        // positive
        for (int shift = 0; shift < 127; shift++) {
            assertThat(MAX_VALUE.shiftRight(shift))
                    .describedAs(">> " + shift)
                    .isEqualTo(Int128.valueOf(MAX_VALUE.toBigInteger().shiftRight(shift)));
        }

        // negative
        for (int shift = 0; shift < 127; shift++) {
            assertThat(MIN_VALUE.shiftRight(shift))
                    .describedAs(">> " + shift)
                    .isEqualTo(Int128.valueOf(MIN_VALUE.toBigInteger().shiftRight(shift)));
        }
    }

    @Test
    public void testShiftRightUnsigned()
    {
        // positive
        for (int shift = 0; shift < 127; shift++) {
            assertThat(MAX_VALUE.shiftRightUnsigned(shift))
                    .describedAs(">>> " + shift)
                    .isEqualTo(Int128.valueOf(MAX_VALUE.toBigInteger().shiftRight(shift)));
        }

        // negative
        for (int shift = 1; shift < 127; shift++) {
            assertThat(MIN_VALUE.shiftRightUnsigned(shift))
                    .describedAs(">> " + shift)
                    .isEqualTo(Int128.valueOf(MIN_VALUE.toBigInteger().abs().shiftRight(shift)));
        }
    }

    @Test
    public void testNumberOfLeadingZeros()
    {
        for (int shift = 0; shift < 127; shift++) {
            assertThat(ONE.shiftLeft(shift).numberOfLeadingZeros())
                    .describedAs("numberOfLeadingZeros(1 << " + shift + ")")
                    .isEqualTo(127 - shift);
        }
    }

    @Test
    public void testNumberOfTrailingZeros()
    {
        for (int shift = 0; shift < 127; shift++) {
            assertThat(ONE.shiftLeft(shift).numberOfTrailingZeros())
                    .describedAs("numberOfTrailingZeros(1 << " + shift + ")")
                    .isEqualTo(shift);
        }
    }

    @Test
    public void testBitCount()
    {
        assertThat(new Int128(0x0000000000000000L, 0x0000000000000000L).bitCount())
                .isEqualTo(0);

        assertThat(new Int128(0xFFFFFFFFFFFFFFFFL, 0x0000000000000000L).bitCount())
                .isEqualTo(64);

        assertThat(new Int128(0x0000000000000000L, 0xFFFFFFFFFFFFFFFFL).bitCount())
                .isEqualTo(64);

        assertThat(new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL).bitCount())
                .isEqualTo(128);

        assertThat(new Int128(0x0000000000000000L, 0x0000000000000001L).bitCount())
                .isEqualTo(1);

        assertThat(new Int128(0x0000000000000001L, 0x0000000000000000L).bitCount())
                .isEqualTo(1);
    }

    @Test
    public void testNot()
    {
        assertThat(new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL).not())
                .isEqualTo(new Int128(0x0000000000000000L, 0x0000000000000000L));

        assertThat(new Int128(0x0000000000000000L, 0x0000000000000000L).not())
                .isEqualTo(new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL));

        assertThat(new Int128(0x0000000000000000L, 0xFFFFFFFFFFFFFFFFL).not())
                .isEqualTo(new Int128(0xFFFFFFFFFFFFFFFFL, 0x0000000000000000L));
    }

    @Test
    public void testAnd()
    {
        assertThat(and(
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL),
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL)))
                .isEqualTo(new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL));

        assertThat(and(
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL),
                new Int128(0x0000000000000000L, 0x0000000000000000L)))
                .isEqualTo(new Int128(0x0000000000000000L, 0x0000000000000000L));

        assertThat(and(
                new Int128(0x0000000000000000L, 0x0000000000000000L),
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL)))
                .isEqualTo(new Int128(0x0000000000000000L, 0x0000000000000000L));

        assertThat(and(
                new Int128(0x0000000000000000L, 0x0000000000000000L),
                new Int128(0x0000000000000000L, 0x0000000000000000L)))
                .isEqualTo(new Int128(0x0000000000000000L, 0x0000000000000000L));

        assertThat(and(
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL),
                new Int128(0x0000000000000000L, 0xFFFFFFFFFFFFFFFFL)))
                .isEqualTo(new Int128(0x0000000000000000L, 0xFFFFFFFFFFFFFFFFL));

        assertThat(and(
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL),
                new Int128(0xFFFFFFFFFFFFFFFFL, 0x0000000000000000L)))
                .isEqualTo(new Int128(0xFFFFFFFFFFFFFFFFL, 0x0000000000000000L));
    }

    @Test
    public void testOr()
    {
        assertThat(or(
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL),
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL)))
                .isEqualTo(new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL));

        assertThat(or(
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL),
                new Int128(0x0000000000000000L, 0x0000000000000000L)))
                .isEqualTo(new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL));

        assertThat(or(
                new Int128(0x0000000000000000L, 0x0000000000000000L),
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL)))
                .isEqualTo(new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL));

        assertThat(or(
                new Int128(0x0000000000000000L, 0x0000000000000000L),
                new Int128(0x0000000000000000L, 0x0000000000000000L)))
                .isEqualTo(new Int128(0x0000000000000000L, 0x0000000000000000L));

        assertThat(or(
                new Int128(0xFFFFFFFFFFFFFFFFL, 0x0000000000000000L),
                new Int128(0x0000000000000000L, 0x0000000000000000L)))
                .isEqualTo(new Int128(0xFFFFFFFFFFFFFFFFL, 0x0000000000000000L));

        assertThat(or(
                new Int128(0x0000000000000000L, 0xFFFFFFFFFFFFFFFFL),
                new Int128(0x0000000000000000L, 0x0000000000000000L)))
                .isEqualTo(new Int128(0x0000000000000000L, 0xFFFFFFFFFFFFFFFFL));
    }

    @Test
    public void testXor()
    {
        assertThat(xor(
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL),
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL)))
                .isEqualTo(new Int128(0x0000000000000000L, 0x0000000000000000L));

        assertThat(xor(
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL),
                new Int128(0x0000000000000000L, 0x0000000000000000L)))
                .isEqualTo(new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL));

        assertThat(xor(
                new Int128(0x0000000000000000L, 0x0000000000000000L),
                new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL)))
                .isEqualTo(new Int128(0xFFFFFFFFFFFFFFFFL, 0xFFFFFFFFFFFFFFFFL));

        assertThat(xor(
                new Int128(0x0000000000000000L, 0x0000000000000000L),
                new Int128(0x0000000000000000L, 0x0000000000000000L)))
                .isEqualTo(new Int128(0x0000000000000000L, 0x0000000000000000L));

        assertThat(xor(
                new Int128(0xFFFFFFFFFFFFFFFFL, 0x0000000000000000L),
                new Int128(0x0000000000000000L, 0x0000000000000000L)))
                .isEqualTo(new Int128(0xFFFFFFFFFFFFFFFFL, 0x0000000000000000L));

        assertThat(xor(
                new Int128(0x0000000000000000L, 0xFFFFFFFFFFFFFFFFL),
                new Int128(0x0000000000000000L, 0x0000000000000000L)))
                .isEqualTo(new Int128(0x0000000000000000L, 0xFFFFFFFFFFFFFFFFL));
    }

    @Test
    public void testToLong()
    {
        assertThat(Int128.valueOf(Long.MAX_VALUE).toLong())
                .isEqualTo(Long.MAX_VALUE);

        assertThat(Int128.valueOf(Long.MAX_VALUE).toLongExact())
                .isEqualTo(Long.MAX_VALUE);

        assertThat(Int128.valueOf(Long.MIN_VALUE).toLong())
                .isEqualTo(Long.MIN_VALUE);

        assertThat(Int128.valueOf(Long.MIN_VALUE).toLongExact())
                .isEqualTo(Long.MIN_VALUE);

        assertThat(ZERO.toLong())
                .isEqualTo(0);

        assertThat(ZERO.toLongExact())
                .isEqualTo(0);

        assertThat(MAX_VALUE.toLong())
                .isEqualTo(-1);

        assertThat(MIN_VALUE.toLong())
                .isEqualTo(0);

        assertThatThrownBy(MAX_VALUE::toLongExact)
                .isInstanceOf(ArithmeticException.class);

        assertThatThrownBy(MIN_VALUE::toLongExact)
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    public void testCompare()
    {
        assertThat(compare(ZERO, ZERO))
                .isEqualTo(0);

        assertThat(compare(ONE, ONE))
                .isEqualTo(0);

        assertThat(compare(MAX_VALUE, MAX_VALUE))
                .isEqualTo(0);

        assertThat(compare(MIN_VALUE, MIN_VALUE))
                .isEqualTo(0);

        assertThat(compare(MIN_VALUE, ZERO))
                .isEqualTo(-1);

        assertThat(compare(MAX_VALUE, ZERO))
                .isEqualTo(1);

        assertThat(compare(ZERO, MIN_VALUE))
                .isEqualTo(1);

        assertThat(compare(ZERO, MAX_VALUE))
                .isEqualTo(-1);

        assertThat(compare(MIN_VALUE, MAX_VALUE))
                .isEqualTo(-1);

        assertThat(compare(MAX_VALUE, MIN_VALUE))
                .isEqualTo(1);

        assertThat(compare(Int128.valueOf(Long.MAX_VALUE), MAX_VALUE))
                .isEqualTo(-1);

        assertThat(compare(Int128.valueOf(Long.MIN_VALUE), MAX_VALUE))
                .isEqualTo(-1);

        assertThat(compare(Int128.valueOf(Long.MAX_VALUE), MIN_VALUE))
                .isEqualTo(1);

        assertThat(compare(Int128.valueOf(Long.MIN_VALUE), MIN_VALUE))
                .isEqualTo(1);

        assertThat(compare(new Int128(0x0000000000000000L, 0xFFFFFFFFFFFFFFFFL), new Int128(0x0000000000000000L, 0xFFFFFFFFFFFFFFFEL)))
                .isEqualTo(1);

        assertThat(compare(new Int128(0xFFFFFFFFFFFFFFFFL, 0x0000000000000001L), new Int128(0xFFFFFFFFFFFFFFFFL, 0x0000000000000000L)))
                .isEqualTo(1);
    }

    @Test
    public void testIncrement()
    {
        assertThat(ZERO.increment())
                .isEqualTo(ONE);

        assertThat(ZERO.incrementExact())
                .isEqualTo(ONE);

        assertThat(MAX_VALUE.increment())
                .isEqualTo(MIN_VALUE);

        assertThatThrownBy(MAX_VALUE::incrementExact)
                .hasMessage("Integer overflow")
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    public void testDecrement()
    {
        assertThat(ZERO.decrement())
                .isEqualTo(ONE.negate());

        assertThat(ZERO.decrementExact())
                .isEqualTo(ONE.negate());

        assertThat(MIN_VALUE.decrement())
                .isEqualTo(MAX_VALUE);

        assertThatThrownBy(MIN_VALUE::decrementExact)
                .hasMessage("Integer overflow")
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    public void testDivide()
    {
        assertThatThrownBy(() -> Int128.divide(ONE, ZERO))
                .hasMessage("Divide by zero")
                .isInstanceOf(ArithmeticException.class);

        assertThat(divide(ONE, MAX_VALUE)).isValid();
        assertThat(divide(ONE, MIN_VALUE)).isValid();
        assertThat(divide(MAX_VALUE, ONE)).isValid();
        assertThat(divide(MIN_VALUE, ONE)).isValid();
        assertThat(divide(MAX_VALUE, MAX_VALUE)).isValid();
        assertThat(divide(MIN_VALUE, MIN_VALUE)).isValid();
        assertThat(divide(Int128.valueOf(Long.MAX_VALUE), Int128.valueOf(12345))).isValid();
        assertThat(divide(Int128.valueOf(Long.MIN_VALUE), Int128.valueOf(12345))).isValid();

        assertThat(divide(MAX_VALUE, Int128.valueOf(12345))).isValid();
        assertThat(divide(MIN_VALUE, Int128.valueOf(12345))).isValid();
        assertThat(divide(MIN_VALUE, subtract(MIN_VALUE, ONE))).isValid();
    }

    @Test
    public void testDivideByPowersOf2()
    {
        for (int i = 0; i < 127; i++) {
            assertThat(divide(MAX_VALUE, ONE.shiftLeft(i)))
                    .describedAs(format("MAX_VALUE / 2^%s", i))
                    .isValid();
        }

        for (int i = 0; i < 127; i++) {
            assertThat(divide(MIN_VALUE, ONE.shiftLeft(i)))
                    .describedAs(format("MAX_VALUE / 2^%s", i))
                    .isValid();
        }
    }

    @Test
    public void testDivideAllMagnitudes()
    {
        // positive / positive
        for (int dividendMagnitude = 127; dividendMagnitude >= 0; dividendMagnitude--) {
            Int128 dividend = MAX_VALUE.shiftRightUnsigned(127 - dividendMagnitude);
            for (int divisorMagnitude = 127; divisorMagnitude > 0; divisorMagnitude--) {
                Int128 divisor = MAX_VALUE.shiftRightUnsigned(127 - divisorMagnitude);

                assertThat(divide(dividend, divisor))
                        .describedAs(format("dividend magnitude: %s, divisor magnitude: %s", dividendMagnitude, divisorMagnitude))
                        .isValid();
            }
        }

        // positive / negative
        for (int dividendMagnitude = 127; dividendMagnitude >= 0; dividendMagnitude--) {
            Int128 dividend = MAX_VALUE.shiftRightUnsigned(127 - dividendMagnitude);
            for (int divisorMagnitude = 127; divisorMagnitude > 0; divisorMagnitude--) {
                Int128 divisor = MIN_VALUE.shiftRight(127 - divisorMagnitude);

                assertThat(divide(dividend, divisor))
                        .describedAs(format("dividend magnitude: %s, divisor magnitude: %s", dividendMagnitude, divisorMagnitude))
                        .isValid();
            }
        }

        // negative / positive
        for (int dividendMagnitude = 127; dividendMagnitude >= 0; dividendMagnitude--) {
            Int128 dividend = MIN_VALUE.shiftRight(127 - dividendMagnitude);
            for (int divisorMagnitude = 127; divisorMagnitude > 0; divisorMagnitude--) {
                Int128 divisor = MAX_VALUE.shiftRightUnsigned(127 - divisorMagnitude);

                assertThat(divide(dividend, divisor))
                        .describedAs(format("dividend magnitude: %s, divisor magnitude: %s", dividendMagnitude, divisorMagnitude))
                        .isValid();
            }
        }

        // negative / negative
        for (int dividendMagnitude = 127; dividendMagnitude >= 0; dividendMagnitude--) {
            Int128 dividend = MIN_VALUE.shiftRight(127 - dividendMagnitude);
            for (int divisorMagnitude = 127; divisorMagnitude > 0; divisorMagnitude--) {
                Int128 divisor = MIN_VALUE.shiftRight(127 - divisorMagnitude);

                assertThat(divide(dividend, divisor))
                        .describedAs(format("dividend magnitude: %s, divisor magnitude: %s", dividendMagnitude, divisorMagnitude))
                        .isValid();
            }
        }
    }

    public static class DivideAssert
            extends AbstractAssert<DivideAssert, DivideAssert.Result>
    {
        private final Int128 dividend;
        private final Int128 divisor;

        public DivideAssert(Int128 dividend, Int128 divisor)
        {
            super(compute(dividend, divisor), DivideAssert.class);
            this.dividend = dividend;
            this.divisor = divisor;
        }

        private static Result compute(Int128 dividend, Int128 divisor)
        {
            try {
                Int128.DivisionResult result = Int128.divideWithRemainder(dividend, divisor);
                return new Result(result.quotient(), result.remainder(), null);
            }
            catch (Exception e) {
                return new Result(null, null, e);
            }
        }

        public static AssertProvider<DivideAssert> divide(Int128 dividend, Int128 divisor)
        {
            return () -> new DivideAssert(dividend, divisor);
        }

        public DivideAssert isValid()
        {
            return satisfies(result -> {
                assertThat(actual.error())
                        .isNull();

                assertThat(addExact(multiplyExact(actual.quotient(), divisor), actual.remainder()))
                        .isEqualTo(dividend);

                assertThat(result.quotient())
                        .isEqualTo(Int128.valueOf(dividend.toBigInteger().divide(divisor.toBigInteger())));

                assertThat(result.remainder())
                        .isEqualTo(Int128.valueOf(dividend.toBigInteger().remainder(divisor.toBigInteger())));
            });
        }

        record Result(Int128 quotient, Int128 remainder, Exception error) {}
    }
}
