```scala
for (i <- Range.inclusive(1, 100)) {
  println(
    if (i % 3 == 0 && i % 5 == 0) "FizzBuzz"
    else if (i % 3 == 0) "Fizz"
    else if (i % 5 == 0) "Buzz"
    else i
  )
}
```

`%Snippet 3.1: the popular "FizzBuzz" programming challenge, implemented in
Scala`

This chapter is a quick tour of the Scala language. For now we will focus on the
basics of Scala that are similar to what you might find in any mainstream
programming language.

The goal of this chapter is to get familiar you enough that you can take the
same sort of code you are used to writing in some other language and write it in
Scala without difficulty. This chapter will not cover more Scala-specific
programming styles or language features: those will be left for `%Chapter 5:
Notable Scala Features`.

-------------------------------------------------------------------------------

For this chapter, we will write our code in the Ammonite Scala REPL:

```bash
$ amm
Loading...
Welcome to the Ammonite Repl 2.2.0 (Scala 2.13.2 Java 11.0.7)
@
```

## Values
### Primitives

Scala has the following sets of primitive types:

> %horizontal-50-no-line
>
> | Type | Values |
> |------|--------|
> | `Byte` | `-128` to `127` |
> | `Short` | `-32,768` to `32,767` |
> | `Int` | `-2,147,483,648` to `2,147,483,647` |
> | `Long` | `-9,223,372,036,854,775,808` to `9,223,372,036,854,775,807` |
>
> ---------------------------------------------------------------------------
>
> | Type | Values |
> |------|--------|
> | `Boolean` | `true`, `false` |
> | `Char` | `'a'`, `'0'`, `'Z'`, `'包'`, ... |
> | `Float` |  32-bit Floating point |
> | `Double` | 64-bit Floating point |
>


These types are identical to the primitive types in Java, and would be similar
to those in C#, C++, or any other statically typed programming language. Each
type supports the typical operations, e.g. booleans support boolean logic `||`
`&&`, numbers support arithmetic `+` `-` `*` `/` and bitwise operations `|` `&`,
and so on. All values support `==` to check for equality and `!=` to check for
inequality.

Numbers default to 32-bit `Int`s. Precedence for arithmetic operations follows
other programming languages: `*` and `/` have higher precedence than `+` or `-`,
and parentheses can be used for grouping.

```scala
@ 1 + 2 * 3
res0: Int = 7

@ (1 + 2) * 3
res1: Int = 9
```

`Int`s are signed and wrap-around on overflow, while 64-bit `Long`s suffixed
with `L` have a bigger range and do not overflow as easily:

> %horizontal-50
>
> ```scala
> @ 2147483647
> res2: Int = 2147483647
> 
> @ 2147483647 + 1
> res3: Int = -2147483648
> ```
>
> ---------------------------------------------------------------------------
>
> ```scala
> @ 2147483647L
> res4: Long = 2147483647L
> 
> @ 2147483647L + 1L
> res5: Long = 2147483648L
> ```

Apart from the basic operators, there are a lot of useful methods on
`java.lang.Integer` and `java.lang.Long`:

```scala
@ java.lang.Integer.<tab>
BYTES                    decode                   numberOfTrailingZeros
signum                   MAX_VALUE                divideUnsigned
getInteger               parseUnsignedInt         toBinaryString
...

@ java.lang.Integer.toBinaryString(123)
res6: String = "1111011"

@ java.lang.Integer.numberOfTrailingZeros(24)
res7: Int = 3
```

64-bit `Double`s are specified using the `1.0` syntax, and have a similar set of
arithmetic operations. You can also use the `1.0F` syntax to ask for 32-bit
`Float`s:

```scala
@ 1.0 / 3.0
res8: Double = 0.3333333333333333

@ 1.0F / 3.0F
res9: Float = 0.33333334F
```

32-bit `Float`s take up half as much memory as 64-bit `Double`s, but are more
prone to rounding errors during arithmetic operations. `java.lang.Float` and
`java.lang.Double` have a similar set of useful operations you can perform on
`Float`s and `Double`s.

### Strings

`String`s in Scala are arrays of 16-bit `Char`s:

```scala
@ "hello world"
res10: String = "hello world"
```

`String`s can be sliced with `.substring`, constructed via
concatenation using `+`, or via string interpolation by prefixing the literal
with `s"..."` and interpolating the values with `$` or `${...}`:

> %horizontal-50
>
> ```scala
> @ "hello world".substring(0, 5)
> res11: String = "hello"
> 
> @ "hello world".substring(5, 10)
> res12: String = " worl"
> ```
> ---------------------------------------------------------------------------
>
> ```scala
> @ "hello" + 1 + " " + "world" + 2
> res13: String = "hello1 world2"
> 
> @ val x = 1; val y = 2
> 
> @ s"Hello $x World $y"
> res15: String = "Hello 1 World 2"
> 
> @ s"Hello ${x + y} World ${x - y}"
> res16: String = "Hello 3 World -1"
> ```

### Local Values and Variables

You can name local values with the `val` keyword:

```scala
@ val x = 1

@ x + 2
res18: Int = 3
```

Note that `val`s are immutable: you cannot re-assign the `val x` to a different
value after the fact. If you want a local variable that can be re-assigned, you
must use the `var` keyword.

> %horizontal-50
> ```scala
> @ x = 3
> cmd41.sc:1: reassignment to val
> val res26 = x = 3
>               ^
> Compilation Failed
> ```
>
> ---------------------------------------------------------------------------
>
> ```scala
> @ var y = 1
> 
> @ y + 2
> res20: Int = 3
> 
> @ y = 3
> 
> @ y + 2
> res22: Int = 5
> ```

In general, you should try to use `val` where possible: most named values
 in your program likely do not need to be re-assigned, and using `val`
helps prevent mistakes where you re-assign something accidentally. Use `var`
only if you are sure you will need to re-assign something later.

Both `val`s and `var`s can be annotated with explicit types. These can serve as
documentation for people reading your code, as well as a way to catch errors if
you accidentally assign the wrong type of value to a variable

> %horizontal-50
>
> ```scala
> @ val x: Int = 1
> 
> @ var s: String = "Hello"
> s: String = "Hello"
> 
> @ s = "World"
> ```
>
> ---------------------------------------------------------------------------
>
> ```scala
> @ val z: Int = "Hello"
> cmd33.sc:1: type mismatch;
>  found   : String("Hello")
>  required: Int
> val z: Int = "Hello"
>              ^
> Compilation Failed
> ```

### Tuples

Tuples are fixed-length collections of values, which may be of different types:

```scala
@ val t = (1, true, "hello")
t: (Int, Boolean, String) = (1, true, "hello")

@ t._1
res27: Int = 1

@ t._2
res28: Boolean = true

@ t._3
res29: String = "hello"
```

Above, we are storing a tuple into the local value `t` using the `(a, b, c)`
syntax, and then using `._1`, `._2` and `._3` to extract the values out of it.
The fields in a tuple are immutable.

The type of the local value `t` can be annotated as a tuple type:

```scala
@ val t: (Int, Boolean, String) = (1, true, "hello")
```

You can also use the `val (a, b, c) = t` syntax to extract all the values at
once, and assign them to meaningful names:

> %horizontal-50
>
> ```scala
> @ val (a, b, c) = t
> a: Int = 1
> b: Boolean = true
> c: String = "hello"
> ```
> ---------------------------------------------------------------------------
> ```scala
> @ a
> res31: Int = 1
> 
> @ b
> res32: Boolean = true
> 
> @ c
> res33: String = "hello"
> ```


Tuples come in any size from 1 to 22 items long:

```scala
@ val t = (1, true, "hello", 'c', 0.2, 0.5f, 12345678912345L)
t: (Int, Boolean, String, Char, Double, Float, Long) = (
  1,
  true,
  "hello",
  'c',
  0.2,
  0.5F,
  12345678912345L
)
```

Most tuples should be relatively small. Large tuples can easily get confusing:
while working with `._1` `._2` and `._3` is probably fine, when you end up
working with `._11` `._13` it becomes easy to mix up the different fields. If
you find yourself working with large tuples, consider defining a
[Class](#classes-and-traits) or Case Class that we will see in `%Chapter 5:
Notable Scala Features`.

### Arrays

Arrays are instantiated using the `Array[T](a, b, c)` syntax, and entries within
each array are retrieved using `a(n)`:

```scala
@ val a = Array[Int](1, 2, 3, 4)

@ a(0) // first entry, array indices start from 0
res36: Int = 1

@ a(3) // last entry
res37: Int = 4

@ val a2 = Array[String]("one", "two", "three", "four")
a2: Array[String] = Array("one", "two", "three", "four")

@ a2(1) // second entry
res39: String = "two"
```

The type parameter inside the square brackets `[Int]` or `[String]` determines
the type of the array, while the parameters inside the parenthesis `(1, 2, 3,
4)` determine its initial contents. Note that looking up an Array by index is
done via parentheses `a(3)` rather than square brackets `a[3]` as is common in
many other programming languages.

You can omit the explicit type parameter and let the compiler infer the Array's
type, or create an empty array of a specified type using `new Array[T](length)`,
and assign values to each index later:


> %horizontal-50
>
> ```scala
> @ val a = Array(1, 2, 3, 4)
> a: Array[Int] = Array(1, 2, 3, 4)
> 
> @ val a2 = Array(
>     "one", "two",
>     "three", "four"
>   )
> a2: Array[String] = Array(
>   "one", "two",
>   "three", "four"
> )
> ```
>
> ---------------------------------------------------------------------------
>
> ```scala
> @ val a = new Array[Int](4)
> a: Array[Int] = Array(0, 0, 0, 0)
> 
> @ a(0) = 1
> 
> @ a(2) = 100
> 
> @ a
> res45: Array[Int] = Array(1, 0, 100, 0)
> ```

For `Array`s created using `new Array`, all entries start off with the value `0`
for numeric arrays, `false` for `Boolean` arrays, and `null` for `String`s and
other types. `Array`s are mutable but fixed-length: you can change the value of
each entry but cannot change the number of entries by adding or removing values.
We will see how to create variable-length collections later in `%Chapter 4:
Scala Collections`.

Multi-dimensional arrays, or arrays-of-arrays, are also supported:

```scala
@ val multi = Array(Array(1, 2), Array(3, 4))
multi: Array[Array[Int]] = Array(Array(1, 2), Array(3, 4))

@ multi(0)(0)
res47: Int = 1

@ multi(0)(1)
res48: Int = 2

@ multi(1)(0)
res49: Int = 3

@ multi(1)(1)
res50: Int = 4
```

Multi-dimensional arrays can be useful to represent grids, matrices, and similar
values.


### Options

Scala's `Option[T]` type allows you to represent a value that may or may not
exist. An `Option[T]` can either be `Some(v: T)` indicating that a value is
present, or `None` indicating that it is absent:

```scala
@ def hello(title: String, firstName: String, lastNameOpt: Option[String]) = {
    lastNameOpt match {
      case Some(lastName) => println(s"Hello $title. $lastName")
      case None => println(s"Hello $firstName")
    }
  }

@ hello("Mr", "Haoyi", None)
Hello Haoyi

@ hello("Mr", "Haoyi", Some("Li"))
Hello Mr. Li
```

The above example shows you how to construct `Option`s using `Some` and `None`,
as well as `match`ing on them in the same way. Many APIs in Scala rely on
`Option`s rather than `null`s for values that may or may not exist. In general,
`Option`s force you to handle both cases of present/absent, whereas when using
`null`s it is easy to forget whether or not a value is null-able, resulting in
confusing `NullPointerException`s at runtime. We will go deeper into pattern
matching in `%Chapter 5: Notable Scala Features`.

`Option`s contain some helper methods that make it easy to work with the
optional value, such as `getOrElse`, which substitutes an alternate value if the
`Option` is `None`:

```scala
@ Some("Li").getOrElse("<unknown>")
res54: String = "Li"

@ None.getOrElse("<unknown>")
res55: String = "<unknown>"
```

`Option`s are very similar to a collection whose size is `0` or `1`. You can
loop over them like normal collections, or transform them with standard
collection operations like `.map`.

> %horizontal-50
> ```scala
> @ def hello2(name: Option[String]) = {
>     for (s <- name) println(s"Hello $s")
>   }
> 
> @ hello2(None) // does nothing
> 
> @ hello2(Some("Haoyi"))
> Hello Haoyi
> ```
>
> ---------------------------------------------------------------------------
>
> ```scala
> @ def nameLength(name: Option[String]) = {
>     name.map(_.length).getOrElse(-1)
>   }
> 
> @ nameLength(Some("Haoyi"))
> res60: Int = 5
> 
> @ nameLength(None)
> res61: Int = -1
> ```

Above, we combine `.map` and `.getOrElse` to print out the length of the name if
present, and otherwise print `-1`. We will learn more about collection
operations in `%Chapter 4: Scala Collections`.

> %example 1 - Values

## Loops, Conditionals, Comprehensions
### For-Loops

For-loops in Scala are similar to "foreach" loops in other languages: they
directly loop over the elements in a collection, without needing to explicitly
maintain and increment an index. If you want to loop over a range of indices,
you can loop over a `Range` such as `Range(0, 5)`:

> %horizontal-50
> ```scala
> @ var total = 0
> 
> @ val items = Array(1, 10, 100, 1000)
> 
> @ for (item <- items) total += item
> 
> @ total
> res65: Int = 1111
> ```
> ---------------------------------------------------------------------------
>
> ```scala
> @ var total = 0
> 
> @ for (i <- Range(0, 5)) {
>     println("Looping " + i)
>     total = total + i
>   }
> Looping 0
> Looping 1
> Looping 2
> Looping 3
> Looping 4
> 
> @ total
> res68: Int = 10
> ```

You can loop over nested `Array`s by placing multiple `<-`s in the header of the
loop:

```scala
@ val multi = Array(Array(1, 2, 3), Array(4, 5, 6))

@ for (arr <- multi; i <- arr) println(i)
1
2
3
4
5
6
```

Loops can have guards using an `if` syntax:

```scala
@ for (arr <- multi; i <- arr; if i % 2 == 0) println(i)
2
4
6
```

### If-Else

`if`-`else` conditionals are similar to those in any other programming language.
One thing to note is that in Scala `if`-`else` can also be used as an
expression, similar to the `a ? b : c` ternary expressions in other languages.
Scala does not have a separate ternary expression syntax, and so the `if`-`else`
can be directly used as the right-hand-side of the `total +=` below.

> %horizontal-50
>
> ```scala
> @ var total = 0
> 
> @ for (i <- Range(0, 10)) {
>     if (i % 2 == 0) total += i
>     else total += 2
>   }
> 
> @ total
> res74: Int = 30
> ```
>
> ---------------------------------------------------------------------------
>
> ```scala
> @ var total = 0
> 
> @ for (i <- Range(0, 10)) {
>     total += (if (i % 2 == 0) i else 2)
>   }
> 
> @ total
> res77: Int = 30
> ```

### Fizzbuzz

Now that we know the basics of Scala syntax, let's consider the common
"Fizzbuzz" programming challenge:

> Write a short program that prints each number from 1 to 100 on a new line.
>
> For each multiple of 3, print "Fizz" instead of the number.
>
> For each multiple of 5, print "Buzz" instead of the number.
>
> For numbers which are multiples of both 3 and 5, print "FizzBuzz" instead of the number.

We can accomplish this as follows:

```scala
@ for (i <- Range.inclusive(1, 100)) {
    if (i % 3 == 0 && i % 5 == 0) println("FizzBuzz")
    else if (i % 3 == 0) println("Fizz")
    else if (i % 5 == 0) println("Buzz")
    else println(i)
  }
1
2
Fizz
4
Buzz
Fizz
7
8
Fizz
Buzz
11
Fizz
13
14
FizzBuzz
...
```

Since `if`-`else` is an expression, we can also write it as:

```scala
@ for (i <- Range.inclusive(1, 100)) {
    println(
      if (i % 3 == 0 && i % 5 == 0) "FizzBuzz"
      else if (i % 3 == 0) "Fizz"
      else if (i % 5 == 0) "Buzz"
      else i
    )
  }
```

### Comprehensions

Apart from using `for` to define loops that perform some action, you can also
use `for` together with `yield` to transform a collection into a new collection:

```scala
@ val a = Array(1, 2, 3, 4)

@ val a2 = for (i <- a) yield i * i
a2: Array[Int] = Array(1, 4, 9, 16)

@ val a3 = for (i <- a) yield "hello " + i
a3: Array[String] = Array("hello 1", "hello 2", "hello 3", "hello 4")
```

Similar to loops, you can filter which items end up in the final collection
using an `if` guard inside the parentheses:

```scala
@ val a4 = for (i <- a if i % 2 == 0) yield "hello " + i
a4: Array[String] = Array("hello 2", "hello 4")
```

Comprehensions can also take multiple input arrays, `a` and `b` below. This
flattens them out into one final output `Array`, similar to using a nested
for-loop:

```scala
@ val a = Array(1, 2); val b = Array("hello", "world")

@ val flattened = for (i <- a; s <- b) yield s + i
flattened: Array[String] = Array("hello1", "world1", "hello2", "world2")
```

You can also replace the parentheses `()` with curly brackets `{}` if you wish
to spread out the nested loops over multiple lines, for easier reading. Note
that the order of `<-`s within the nested comprehension matters, just like how
the order of nested loops affects the order in which the loop actions will take
place:

```scala
@ val flattened = for{
    i <- a
    s <- b
  } yield s + i
flattened: Array[String] = Array("hello1", "world1", "hello2", "world2")

@ val flattened2 = for{
    s <- b
    i <- a
  } yield s + i
flattened2: Array[String] = Array("hello1", "hello2", "world1", "world2")
```

We can use comprehensions to write a version of FizzBuzz that doesn't print its
results immediately to the console, but returns them as a `Seq` (short for
"sequence"):

```scala
@ val fizzbuzz = for (i <- Range.inclusive(1, 100)) yield {
    if (i % 3 == 0 && i % 5 == 0) "FizzBuzz"
    else if (i % 3 == 0) "Fizz"
    else if (i % 5 == 0) "Buzz"
    else i.toString
  }
fizzbuzz: IndexedSeq[String] = Vector(
  "1",
  "2",
  "Fizz",
  "4",
  "Buzz",
...
```

We can then use the `fizzbuzz` collection however we like: storing it in a
variable, passing it into methods, or processing it in other ways. We will cover
what you can do with these collections later, in `%Chapter 4: Scala
Collections`.


> %example 2 - LoopsConditionals

## Methods and Functions
### Methods

You can define methods using the `def` keyword:

```scala
@ def printHello(times: Int) = {
    println("hello " + times)
  }

@ printHello(1)
hello 1

@ printHello(times = 2) // argument name provided explicitly
hello 2
```

Passing in the wrong type of argument, or missing required arguments, is a
compiler error. However, if the argument has a default value, then passing it is
optional.


> %horizontal-50
> ```scala
> @ printHello("1") // wrong type of argument
> cmd128.sc:1: type mismatch;
>  found   : String("1")
>  required: Int
> val res128 = printHello("1")
>                          ^
> Compilation Failed
> ```
>
> ---------------------------------------------------------------------------
>
> ```scala
> @ def printHello2(times: Int = 0) = {
>     println("hello " + times)
>   }
> 
> @ printHello2(1)
> hello 1
> 
> @ printHello2()
> hello 0
> ```

#### Returning Values from Methods

Apart from performing actions like printing, methods can also return values. The
last expression within the curly brace `{}` block is treated as the return value
of a Scala method.


```scala
@ def hello(i: Int = 0) = {
    "hello " + i
  }

@ hello(1)
res96: String = "hello 1"
```

You can call the method and print out or perform other computation on the
returned value:

```scala
@ println(hello())
hello 0

@ val helloHello = hello(123) + " " + hello(456)
helloHello: String = "hello 123 hello 456"

@ helloHello.reverse
res99: String = "654 olleh 321 olleh"
```

### Function Values

You can define function values using the `=>` syntax. Functions values are
similar to methods, in that you call them with arguments and they can perform
some action or return some value. Unlike methods, functions themselves are
values: you can pass them around, store them in variables, and call them later.

```scala
@ var g: Int => Int = i => i + 1

@ g(10)
res101: Int = 11

@ g = i => i * 2

@ g(10)
res103: Int = 20
```

Note that unlike methods, function values cannot have optional arguments (i.e.
with default values) and cannot take type parameters via the `[T]` syntax. When
a method is converted into a function value, any optional arguments must be
explicitly included, and type parameters fixed to concrete types. Function
values are also anonymous, which makes stack traces involving them less
convenient to read than those using methods.

In general, you should prefer using methods unless you really need the
flexibility to pass as parameters or store them in variables. But if you need
that flexibility, function values are a great tool to have.

#### Methods taking Functions

One common use case of function values is to pass them into methods that take
function parameters. Such methods are often called "higher order methods".
Below, we have a class `Box` with a method `printMsg` that prints its contents
(an `Int`), and a separate method `update` that takes a function of type `Int =>
Int` that can be used to update `x`. You can then pass a function literal into
`update` in order to change the value of `x`:

> %horizontal-50
>
> ```scala
> @ class Box(var x: Int) {
>     def update(f: Int => Int) = x = f(x)
>     def printMsg(msg: String) = {
>       println(msg + x)
>     }
>   }
> ```
> ---------------------------------------------------------------------------
>
> ```scala
> @ val b = new Box(1)
> 
> @ b.printMsg("Hello")
> Hello1
> 
> @ b.update(i => i + 5)
> 
> @ b.printMsg("Hello")
> Hello6
> ```

Simple functions literals like `i => i + 5` can also be written via the
shorthand `_ + 5`, with the underscore `_` standing in for the function
parameter.

```scala
@ b.update(_ + 5)

@ b.printMsg("Hello")
Hello11
```

This placeholder syntax for function literals also works for multi-argument
functions, e.g. `(x, y) => x + y` can be written as `_ + _`.

Any method that takes a function as an argument can also be given a method
reference, as long as the method's signature matches that of the function type,
here `Int => Int`:

```scala
@ def increment(i: Int) = i + 1

@ val b = new Box(123)

@ b.update(increment) // Providing a method reference

@ b.update(x => increment(x)) // Explicitly writing out the function literal

@ b.update{x => increment(x)} // Methods taking a single function can be called with {}s

@ b.update(increment(_)) // You can also use the `_` placeholder syntax

@ b.printMsg("result: ")
result: 127
```

#### Multiple Parameter Lists

Methods can be defined to take multiple parameter lists. This is useful for
writing higher-order methods that can be used like control structures, such as
the `myLoop` method below:

> %horizontal-50
>
> ```scala
> @ def myLoop(start: Int, end: Int)
>             (callback: Int => Unit) = {
>     for (i <- Range(start, end)) {
>       callback(i)
>     }
>   }
> ```
> ---------------------------------------------------------------------------
> ```scala
> @ myLoop(start = 5, end = 10) { i =>
>     println(s"i has value ${i}")
>   }
> i has value 5
> i has value 6
> i has value 7
> i has value 8
> i has value 9
> ```

The ability to pass function literals to methods is used to great effect in the
standard library, to concisely perform transformations on collections. We will
see more of that in `%Chapter 4: Scala Collections`.

> %example 3 - MethodsFunctions

## Classes and Traits

You can define classes using the `class` keyword, and instantiate them using
`new`. By default, all arguments passed into the class constructor are available
in all of the class' methods: the `(x: Int)` above defines both the private
fields as well as the class' constructor. `x` is thus accessible in the
`printMsg` function, but cannot be accessed outside the class:

> %horizontal-50
>
> ```scala
> @ class Foo(x: Int) {
>     def printMsg(msg: String) = {
>       println(msg + x)
>     }
>   }
> ```
>
> ---------------------------------------------------------------------------
>
> ```scala
> @ val f = new Foo(1)
> 
> @ f.printMsg("hello")
> hello1
>
> @ f.x
> cmd120.sc:1: value x is not a member of Foo
> Compilation Failed
> ```

To make `x` publicly accessible you can make it a `val`, and to make it mutable
you can make it a `var`:

> %horizontal-50
>
> ```scala
> @ class Bar(val x: Int) {
>     def printMsg(msg: String) = {
>       println(msg + x)
>     }
>   }
> ```
> ---------------------------------------------------------------------------
> ```scala
> @ val b = new Bar(1)
> 
> @ b.x
> res122: Int = 1
> ```

> %horizontal-50
> ```scala
> @ class Qux(var x: Int) {
>     def printMsg(msg: String) = {
>       x += 1
>       println(msg + x)
>     }
>   }
> ```
> ---------------------------------------------------------------------------
> ```scala
> @ val q = new Qux(1)
> 
> @ q.printMsg("hello")
> hello2
> 
> @ q.printMsg("hello")
> hello3
> ```

You can also use `val`s or `var`s in the body of a class to store data. These
get computed once when the class is instantiated:

> %horizontal-50
>
> ```scala
> @ class Baz(x: Int) {
>     val bangs = "!" * x
>     def printMsg(msg: String) = {
>       println(msg + bangs)
>     }
>   }
> ```
> ---------------------------------------------------------------------------
> ```scala
> @ val z = new Baz(3)
> 
> @ z.printMsg("hello")
> hello!!!
> ```


### Traits

`trait`s are similar to `interface`s in traditional object-oriented languages: a
set of methods that multiple classes can inherit. Instances of these classes can
then be used interchangeably.

```scala
@ trait Point{ def hypotenuse: Double }

@ class Point2D(x: Double, y: Double) extends Point{
    def hypotenuse = math.sqrt(x * x + y * y)
  }

@ class Point3D(x: Double, y: Double, z: Double) extends Point{
    def hypotenuse = math.sqrt(x * x + y * y + z * z)
  }

@ val points: Array[Point] = Array(new Point2D(1, 2), new Point3D(4, 5, 6))

@ for (p <- points) println(p.hypotenuse)
2.23606797749979
8.774964387392123
```

Above, we have defined a `Point` trait with a single method `def hypotenuse:
Double`. The subclasses `Point2D` and `Point3D` both have different sets of
parameters, but they both implement `def hypotenuse`. Thus we can put both
`Point2D`s and `Point3D`s into our `points: Array[Point]` and treat them all
uniformly as objects with a `def hypotenuse` method, regardless of what their
actual class is.

> %example 4 - ClassesTraits

## Conclusion

In this chapter, we have gone through a lightning tour of the core Scala
language. While the exact syntax may be new to you, the concepts should be
mostly familiar: primitives, arrays, loops, conditionals, methods, and classes
are part of almost every programming language. Next we will look at the core of
the Scala standard library: the Scala Collections.

> %exercise 5 - FlexibleFizzBuzz
>
> Define a `def flexibleFizzBuzz` method that takes a `String => Unit` callback
> function as its argument, and allows the caller to decide what they want to do
> with the output. The caller can choose to ignore the output, `println` the
> output directly, or store the output in a previously-allocated array they
> already have handy.
>
> > %horizontal-50
> >
> > ```scala
> > @ flexibleFizzBuzz(s => {} /* do nothing */)
> >
> > @ flexibleFizzBuzz(s => println(s))
> > 1
> > 2
> > Fizz
> > 4
> > Buzz
> > ...
> > ```
> > ---------------------------------------------------------------------------
> > ```scala
> > @ var i = 0
> >
> > @ val output = new Array[String](100)
> >
> > @ flexibleFizzBuzz{s =>
> >     output(i) = s
> >     i += 1
> >   }
> >
> > @ output
> > res125: Array[String] = Array(
> >   "1",
> >   "2",
> >   "Fizz",
> >   "4",
> >   "Buzz",
> > ...
> > ```

> %exercise 6 - PrintMessages
>
> Write a recursive method `printMessages` that can receive an array of `Msg`
> class instances, each with an optional `parent` ID, and use it to print out a
> threaded fashion. That means that child messages are print out indented
> underneath their parents, and the nesting can be arbitrarily deep.
>
> ```scala
> class Msg(val id: Int, val parent: Option[Int], val txt: String)
> def printMessages(messages: Array[Msg]): Unit = ...
> ```
>
> > %horizontal-50
> >
> > > %code-example 6 - PrintMessages/TestPrintMessages.sc
> >
> > ---------------------------------------------------------------------------
> >
> > > %code-example 6 - PrintMessages/expected.txt

> %exercise 7 - ContextManagers
>
> Define a pair of methods `withFileWriter` and `withFileReader` that can be
> called as shown below. Each method should take the name of a file, and a
> function value that is called with a `java.io.BufferedReader` or
> `java.io.BufferedWriter` that it can use to read or write data. Opening and
> closing of the reader/writer should be automatic, such that a caller cannot
> forget to close the file. This is similar to Python "context managers" or Java
> "try-with-resource" syntax.
>
> > %code-example 7 - ContextManagers/TestContextManagers.sc
>
> You can use the Java standard library APIs
> `java.nio.file.Files.newBufferedWriter` and `newBufferedReader` for working
> with file readers and writers. We will get more familiar with working with
> files and the filesystem in `%Chapter 7: Files and Subprocesses`.
