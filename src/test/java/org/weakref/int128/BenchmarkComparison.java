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

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 1, jvmArgsAppend = {
        "-XX:+UnlockDiagnosticVMOptions",
//        "-XX:CompileCommand=print,*int128*.*",
        "-XX:PrintAssemblyOptions=intel"})
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
public class BenchmarkComparison
{
    @Benchmark
    @OperationsPerInvocation(BenchmarkData.COUNT)
    public void compare(BenchmarkData data)
    {
        for (int i = 0; i < BenchmarkData.COUNT; i++) {
            Int128 left = data.left[i];
            Int128 right = data.right[i];

            sink(Int128Math.compare(left.high(), left.low(), right.high(), right.low()));
        }
    }

    @CompilerControl(CompilerControl.Mode.DONT_INLINE)
    public static void sink(int value)
    {
    }

    @State(Scope.Thread)
    public static class BenchmarkData
    {
        private static final int COUNT = 1000;

        private final Int128[] left = new Int128[COUNT];
        private final Int128[] right = new Int128[COUNT];

        @Param(value = {"63", "126"})
        public int magnitude;

        @Setup
        public void setup()
        {
            for (int i = 0; i < COUNT; i++) {
                left[i] = Int128.random(magnitude);
                right[i] = Int128.random(magnitude);
            }
        }
    }

    @Test
    public void test()
    {
        BenchmarkData data = new BenchmarkData();
        data.setup();

        compare(data);
    }

    public static void main(String[] args)
            throws RunnerException
    {
        BenchmarkRunner.benchmark(BenchmarkComparison.class);
    }
}
