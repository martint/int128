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

final class Primitives
{
    private Primitives() {}

    /**
     * Compute carry of addition with carry
     */
    public static long unsignedCarry(long a, long b, long c)
    {
        // HD 2-13
        return (a & b) | ((a | b) & ~(a + b + c)) >>> 63;
    }

    public static long signedCarry(long a, long b, long c)
    {
        // HD 2-13
        return ((a + b + c) ^ a ^ b) >>> 63;
    }

    public static long signedBorrow(long a, long b, long c)
    {
        // HD 2-13
        return ((a - b - c) ^ a ^ b) >>> 63;
    }

    public static long unsignedCarry(long a, long b)
    {
        // HD 2-13
        return ((a >>> 1) + (b >>> 1) + ((a & b) & 1)) >>> 63;
    }

    public static long unsignedBorrow(long a, long b)
    {
        // HD 2-13
        return ((~a & b) | (~(a ^ b) & (a - b))) >>> 63;
    }

    // TODO: replace with JDK 18's Math.unsignedMultiplyHigh
    public static long unsignedMultiplyHigh(long x, long y)
    {
        // HD 8-3: High-Order Product Signed from/to Unsigned
        return Math.multiplyHigh(x, y) + ifNegative(x, y) + ifNegative(y, x);
    }

    /**
     * Whether the 128-bit number formed by [high, low] has fewer than 64 significant bits
     * (i.e., it fits in a long without overflow)
     */
    public static boolean inLongRange(long high, long low)
    {
        return high == (low >> 63);
    }

    /**
     * Branchless form of
     * <pre>
     * if (test < 0) {
     *   return value;
     * }
     * else {
     *   return 0;
     * }
     * </pre>
     */
    public static long ifNegative(long test, long value)
    {
        return value & (test >> 63);
    }

    public static int compareUnsigned(long aHigh, long aLow, long bHigh, long bLow)
    {
        int result = Long.compareUnsigned(aHigh, bHigh);

        if (result == 0) {
            result = Long.compareUnsigned(aLow, bLow);
        }

        return result;
    }

    public static int compare(long aHigh, long aLow, long bHigh, long bLow)
    {
        int result = Long.compare(aHigh, bHigh);

        if (result == 0) {
            result = Long.compareUnsigned(aLow, bLow);
        }

        return result;
    }

    public static boolean isZero(long high, long low)
    {
        return (high | low) == 0;
    }

    public static long incrementHigh(long high, long low)
    {
        return high + ((low == -1) ? 1 : 0);
    }

    public static long incrementLow(long high, long low)
    {
        return low + 1;
    }

    public static long decrementHigh(long high, long low)
    {
        return high - ((low == 0) ? 1 : 0);
    }

    public static long decrementLow(long high, long low)
    {
        return low - 1;
    }

    public static long addHigh(long aHigh, long aLow, long bHigh, long bLow)
    {
        return aHigh + bHigh + unsignedCarry(aLow, bLow);
    }

    public static long addLow(long aHigh, long aLow, long bHigh, long bLow)
    {
        return aLow + bLow;
    }

    public static long subtractHigh(long aHigh, long aLow, long bHigh, long bLow)
    {
        return aHigh - bHigh - unsignedBorrow(aLow, bLow);
    }

    public static long subtractLow(long aHigh, long aLow, long bHigh, long bLow)
    {
        return aLow - bLow;
    }

    public static long multiplyHigh(long aHigh, long aLow, long bHigh, long bLow)
    {
        long z1High = unsignedMultiplyHigh(aLow, bLow);
        long z2Low = aLow * bHigh;
        long z3Low = aHigh * bLow;

        return z1High + z2Low + z3Low;
    }

    public static long multiplyLow(long aHigh, long aLow, long bHigh, long bLow)
    {
        return aLow * bLow;
    }

    public static long shiftLeftHigh(long high, long low, int shift)
    {
        if (shift < 64) {
            return (high << shift) | (low >>> 1 >>> ~shift);
        }
        else {
            return low << (shift - 64);
        }
    }

    public static long shiftLeftLow(long high, long low, int shift)
    {
        if (shift < 64) {
            return low << shift;
        }
        else {
            return 0;
        }
    }

    public static long shiftRightUnsignedHigh(long high, long low, int shift)
    {
        if (shift < 64) {
            return high >>> shift;
        }
        else {
            return 0;
        }
    }

    public static long shiftRightUnsignedLow(long high, long low, int shift)
    {
        if (shift < 64) {
            return (high << 1 << ~shift) | (low >>> shift);
        }
        else {
            return high >>> (shift - 64);
        }
    }

    public static long shiftRightHigh(long high, long low, int shift)
    {
        if (shift < 64) {
            return high >> shift;
        }
        else {
            return high >> 63;
        }
    }

    public static long shiftRightLow(long high, long low, int shift)
    {
        // HD 2-18
        if (shift < 64) {
            return (high << 1 << ~shift) | (low >>> shift);
        }
        else {
            return high >> (shift - 64);
        }
    }

    public static long andHigh(long aHigh, long aLow, long bHigh, long bLow)
    {
        return aHigh & bHigh;
    }

    public static long andLow(long aHigh, long aLow, long bHigh, long bLow)
    {
        return aLow & bLow;
    }

    public static int numberOfLeadingZeros(long high, long low)
    {
        int count = Long.numberOfLeadingZeros(high);
        if (count == 64) {
            count += Long.numberOfLeadingZeros(low);
        }

        return count;
    }

    public static int numberOfTrailingZeros(long high, long low)
    {
        int count = Long.numberOfTrailingZeros(low);
        if (count == 64) {
            count += Long.numberOfTrailingZeros(high);
        }

        return count;
    }

    public static long negateHigh(long high, long low)
    {
        return -high - (low != 0 ? 1 : 0);
    }

    public static long negateLow(long high, long low)
    {
        return -low;
    }
}
