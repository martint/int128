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

import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.CompilerControl;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.RunnerException;

import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {
        "-XX:+UnlockDiagnosticVMOptions",
//        "-XX:CompileCommand=print,*int128*.*",
        "-XX:PrintAssemblyOptions=intel"})
@Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
public class BenchmarkDivision
{
    @Benchmark
    @OperationsPerInvocation(BenchmarkData.COUNT)
    public void divide128(BenchmarkData data)
    {
        for (int i = 0; i < BenchmarkData.COUNT; i++) {
            sink(Int128.divideWithRemainder(data.dividends[i], data.divisors[i]));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BenchmarkData.COUNT)
    public void divideBigint(BenchmarkData data)
    {
        for (int i = 0; i < BenchmarkData.COUNT; i++) {
            sink(data.bigintDividends[i].divide(data.bigintDivisors[i]));
        }
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static void sink(Int128.DivisionResult value)
    {
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static void sink(BigInteger value)
    {
    }

    @State(Scope.Thread)
    public static class BenchmarkData
    {
        private static final int COUNT = 1000;

        private final Int128[] dividends = new Int128[COUNT];
        private final Int128[] divisors = new Int128[COUNT];

        private final BigInteger[] bigintDividends = new BigInteger[COUNT];
        private final BigInteger[] bigintDivisors = new BigInteger[COUNT];

        @Param(value = {"126", "90", "65", "64", "63", "32", "10", "1", "0"})
        private int dividendMagnitude = 126;

        @Param(value = {"126", "90", "65", "64", "63", "32", "10", "1"})
        private int divisorMagnitude = 90;

        @Setup
        public void setup()
        {
            int count = 0;
            while (count < COUNT) {
                Int128 dividend = Int128.random(dividendMagnitude);
                Int128 divisor = Int128.random(divisorMagnitude);

                if (ThreadLocalRandom.current().nextBoolean()) {
                    dividend = dividend.negate();
                }

                if (ThreadLocalRandom.current().nextBoolean()) {
                    divisor = divisor.negate();
                }

                if (!divisor.isZero()) {
                    dividends[count] = dividend;
                    divisors[count] = divisor;

                    bigintDividends[count] = dividend.toBigInteger();
                    bigintDivisors[count] = divisor.toBigInteger();

                    count++;
                }
            }
        }
    }

    @Test
    public void test()
    {
        BenchmarkData data = new BenchmarkData();
        data.setup();

        divide128(data);
        divideBigint(data);
    }

    public static void main(String[] args)
            throws RunnerException
    {
        BenchmarkRunner.benchmark(BenchmarkDivision.class);
    }
}
