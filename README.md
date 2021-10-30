# Fast 128-bit math library for Java

![CI](https://github.com/martint/int128/actions/workflows/main.yml/badge.svg)
       
## Examples     

```java
System.out.println(Int128.multiply(Int128.valueOf(Long.MAX_VALUE), Int128.valueOf(10)));
// => 92233720368547758070

System.out.println(Int128.divide(Int128.MAX_VALUE, Int128.valueOf(123456)));
// => 1378152406205200490309805142851575

System.out.println(Int128.remainder(Int128.MAX_VALUE, Int128.valueOf(123456)));
// => 62527

System.out.println(Int128.shiftRight(Int128.MAX_VALUE, 100));
// => 134217727

System.out.println(Int128.shiftLeft(Int128.ONE, 126));
// => 85070591730234615865843651857942052864

System.out.println(Int128.subtract(Int128.MAX_VALUE, Int128.ONE));
// => 170141183460469231731687303715884105726

System.out.println(Int128.bitCount(Int128.valueOf(-1)));
// => 128

System.out.println(Int128.numberOfLeadingZeros(Int128.valueOf(1)));
// => 127
```
