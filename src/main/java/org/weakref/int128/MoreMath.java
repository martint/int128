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

class MoreMath
{
    private MoreMath() {}

    /**
     * Compute carry of addition with carry
     */
    public static long unsignedCarry(long a, long b, long c)
    {
        // HD 2-13
        return (a & b) | ((a | b) & ~(a + b + c)) >>> 63; // TODO: verify
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

    // TODO: replace with JDK 18's Math.unsignedMultiplyHigh
    public static long unsignedMultiplyHigh(long x, long y)
    {
        // HD 8-3: High-Order Product Signed from/to Unsigned
        return Math.multiplyHigh(x, y) + ifNegative(x, y) + ifNegative(y, x);
    }
}
