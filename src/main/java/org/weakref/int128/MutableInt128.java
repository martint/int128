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

class MutableInt128
{
    private long high;
    private long low;

    public MutableInt128()
    {
    }

    public long high()
    {
        return high;
    }

    public void high(long high)
    {
        this.high = high;
    }

    public long low()
    {
        return low;
    }

    public void low(long low)
    {
        this.low = low;
    }

    public void set(long high, long low)
    {
        this.high = high;
        this.low = low;
    }

    public void set(Int128 value)
    {
        this.high = value.high();
        this.low = value.low();
    }

    public Int128 get()
    {
        return new Int128(high, low);
    }
}
