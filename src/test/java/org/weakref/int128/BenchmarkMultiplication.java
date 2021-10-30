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
public class BenchmarkMultiplication
{
    @Benchmark
    @OperationsPerInvocation(BenchmarkData.COUNT)
    public void multiplyExact128(BenchmarkData data)
    {
        for (int i = 0; i < BenchmarkData.COUNT; i++) {
            sink(Int128.multiplyExact(data.lefts[i], data.rights[i]));
        }
    }

    @Benchmark
    @OperationsPerInvocation(BenchmarkData.COUNT)
    public void multiplyBigint(BenchmarkData data)
    {
        for (int i = 0; i < BenchmarkData.COUNT; i++) {
            sink(data.bigintLefts[i].multiply(data.bigintRights[i]));
        }
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static void sink(Int128 value)
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

        private final Int128[] lefts = new Int128[COUNT];
        private final Int128[] rights = new Int128[COUNT];

        private final BigInteger[] bigintLefts = new BigInteger[COUNT];
        private final BigInteger[] bigintRights = new BigInteger[COUNT];

        @Param(value = {"126", "90", "65", "64", "63", "32", "10", "1", "0"})
        private int leftMagnitude;

        @Param(value = {"126", "90", "65", "64", "63", "32", "10", "1", "0"})
        private int rightMagnitude;

        @Setup
        public void setup()
        {
            int count = 0;
            while (count < COUNT) {
                Int128 left = Int128.random(leftMagnitude);
                Int128 right = Int128.random(rightMagnitude);

                if (ThreadLocalRandom.current().nextBoolean()) {
                    left = left.negate();
                }
                if (ThreadLocalRandom.current().nextBoolean()) {
                    right = right.negate();
                }

                if (left.toBigInteger().multiply(right.toBigInteger()).bitLength() > 126) {
                    System.exit(0); // overflow
                }

                lefts[count] = left;
                rights[count] = right;

                bigintLefts[count] = left.toBigInteger();
                bigintRights[count] = right.toBigInteger();

                count++;
            }
        }
    }

    @Test
    public void test()
    {
        BenchmarkData data = new BenchmarkData();
        data.setup();

        multiplyExact128(data);
        multiplyBigint(data);
    }

    public static void main(String[] args)
            throws RunnerException
    {
        BenchmarkRunner.benchmark(BenchmarkMultiplication.class);
    }
}
