```scala
@ def getDayMonthYear(s: String) = s match {
    case s"$day-$month-$year" => println(s"found day: $day, month: $month, year: $year")
    case _ => println("not a date")
  }

@ getDayMonthYear("9-8-1965")
found day: 9, month: 8, year: 1965

@ getDayMonthYear("9-8")
not a date
```

`%Snippet 5.1: using Scala's pattern matching feature to parse simple string
patterns`

This chapter will cover some of the more interesting and unusual features of
Scala. For each such feature, we will cover both what the feature does as well
as some common use cases to give you an intuition for what it is useful for.

Not every feature in this chapter will be something you use day-to-day.
Nevertheless, even these less-commonly-used features are used often enough that
it is valuable to have a high-level understanding for when you eventually
encounter them in the wild.

-------------------------------------------------------------------------------

## Case Classes and Sealed Traits
### Case Classes

`case class`es are like normal `class`es, but meant to represent `class`es which
are "just data": where all the data is immutable and public, without any mutable
state or encapsulation. Their use case is similar to "structs" in C/C++, "POJOs"
in Java or "Data Classes" in Python or Kotlin. Their name comes from the fact
that they support [pattern matching](#pattern-matching) via the `case` keyword.

Case classes are defined with the `case` keyword, and can be instantiated
without `new`. All of their constructor parameters are public fields by default.

> %horizontal-50
>
> ```scala
> @ case class Point(x: Int, y: Int)
> 
> @ val p = Point(1, 2)
> p: Point = Point(1, 2)
> ```
> ---------------------------------------------------------------------------
>
> ```scala
> @ p.x
> res2: Int = 1
> 
> @ p.y
> res3: Int = 2
> ```

`case class`s give you a few things for free:

- A `.toString` implemented to show you the constructor parameter values

- A `==` implemented to check if the constructor parameter values are equal

- A `.copy` method to conveniently create modified copies of the case class
  instance

> %horizontal-50
>
> ```scala
> @ p.toString
> res4: String = "Point(1,2)"
> 
> @ val p2 = Point(1, 2)
> 
> @ p == p2
> res6: Boolean = true
> ```
>
> ---------------------------------------------------------------------------
>
> ```scala
> @ val p = Point(1, 2)
> 
> @ val p3 = p.copy(y = 10)
> p3: Point = Point(1, 10)
> 
> @ val p4 = p3.copy(x = 20)
> p4: Point = Point(20, 10)
> ```

Like normal classes, you can define instance methods or properties in the body
of the `case class`:


> %horizontal-50
>
> ```scala
> @ case class Point(x: Int, y: Int) {
>     def z = x + y
>   }
> ```
> ---------------------------------------------------------------------------
> ```scala
> @ val p = Point(1, 2)
> 
> @ p.z
> res12: Int = 3
> ```

`case class`es are a good replacement for large tuples, since instead of
extracting their values via `._1` `._2` `._7` you can extract the values via
their names like `.x` and `.y`. That is much easier than trying to remember
exactly what field `._7` in a large tuple represents!

> %example 1 - CaseClass

### Sealed Traits

`trait`s can also be defined `sealed`, and only extended by a fixed set of `case
class`es. In the following example, we define a `sealed trait Point` extended by
two `case class`es: `Point2D` and `Point3D`:

```scala
@ {
  sealed trait Point
  case class Point2D(x: Double, y: Double) extends Point
  case class Point3D(x: Double, y: Double, z: Double) extends Point
  }

@ def hypotenuse(p: Point) = p match {
    case Point2D(x, y) => math.sqrt(x * x + y * y)
    case Point3D(x, y, z) => math.sqrt(x * x + y * y + z * z)
  }

@ val points: Array[Point] = Array(Point2D(1, 2), Point3D(4, 5, 6))

@ for (p <- points) println(hypotenuse(p))
2.23606797749979
8.774964387392123
```

The core difference between normal `trait`s and `sealed trait`s can be
summarized as follows:

- Normal `trait`s are *open*, so any number of classes can inherit from the
  trait as long as they provide all the required methods, and instances of those
  classes can be used interchangeably via the `trait`'s required methods.

- `sealed trait`s are *closed*: they only allow a fixed set of classes to
  inherit from them, and all inheriting classes must be defined together with
  the trait itself in the same file or REPL command (hence the curlies `{}`
  surrounding the `Point`/`Point2D`/`Point3D` definitions above).

Because there are only a fixed number of classes inheriting from `sealed trait
Point`, we can use pattern matching in the `def hypotenuse` function above to
define how each kind of `Point` should be handled.

### Use Cases for Normal v.s. Sealed Traits

Both normal `trait`s and `sealed trait`s are common in Scala applications:
normal `trait`s for interfaces which may have any number of subclasses, and
`sealed trait`s where the number of subclasses is fixed.

Normal `trait`s and `sealed trait`s make different things easy:

- A normal `trait` hierarchy makes it easy to add additional sub-classes: just
  define your class and implement the necessary methods. However, it makes it
  difficult to add new methods: a new method needs to be added to all existing
  subclasses, of which there may be many.

- A `sealed trait` hierarchy is the opposite: it is easy to add new methods,
  since a new method can simply pattern match on each sub-class and decide what
  it wants to do for each. However, adding new sub-classes is difficult, as you
  need to go to all existing pattern matches and add the `case` to handle your
  new sub-class

In general, `sealed trait`s are good for modelling hierarchies where you expect
the number of sub-classes to change very little or not-at-all. A good example of
something that can be modeled using `sealed trait` is JSON:

```scala
@ {
  sealed trait Json
  case class Null() extends Json
  case class Bool(value: Boolean) extends Json
  case class Str(value: String) extends Json
  case class Num(value: Double) extends Json
  case class Arr(value: Seq[Json]) extends Json
  case class Dict(value: Map[String, Json]) extends Json
  }
```

- A JSON value can only be JSON null, boolean, number, string, array, or
  dictionary.

- JSON has not changed in 20 years, so it is unlikely that anyone will need to
  extend our JSON `trait` with additional subclasses.

- While the set of sub-classes is fixed, the range of operations we may want to
  do on a JSON blob is unbounded: parse it, serialize it, pretty-print it,
  minify it, sanitize it, etc.

Thus it makes sense to model a JSON data structure as a closed `sealed trait`
hierarchy rather than a normal open `trait` hierarchy.

> %example 2 - SealedTrait

## Pattern Matching

### Match

Scala allows pattern matching on values using the `match` keyword. This is
similar to the `switch` statement found in other programming languages, but more
flexible: apart from `match`ing on primitive integers and strings, you can also
use `match` to extract values from ("destructure") composite data types like
tuples and `case class`es. Note that in many examples below, there is a `case _
=>` clause which defines the default case if none of the earlier cases matched.

> %horizontal-50
>
> #### Matching on `Int`s
>
> ```scala
> @ def dayOfWeek(x: Int) = x match {
>     case 1 => "Mon"; case 2 => "Tue"
>     case 3 => "Wed"; case 4 => "Thu"
>     case 5 => "Fri"; case 6 => "Sat"
>     case 7 => "Sun"; case _ => "Unknown"
>   }
>
> @ dayOfWeek(5)
> res19: String = "Fri"
>
> @ dayOfWeek(-1)
> res20: String = "Unknown"
> ```
>
> ---------------------------------------------------------------------------
>
> #### Matching on `String`s
>
> ```scala
> @ def indexOfDay(d: String) = d match {
>     case "Mon" => 1; case "Tue" => 2
>     case "Wed" => 3; case "Thu" => 4
>     case "Fri" => 5; case "Sat" => 6
>     case "Sun" => 7; case _ => -1
>   }
>
> @ indexOfDay("Fri")
> res22: Int = 5
>
> @ indexOfDay("???")
> res23: Int = -1
> ```

> %horizontal-50
>
> #### Matching on tuple `(Int, Int)`
> ```scala
> @ for (i <- Range.inclusive(1, 100)) {
>     val s =  (i % 3, i % 5) match {
>       case (0, 0) => "FizzBuzz"
>       case (0, _) => "Fizz"
>       case (_, 0) => "Buzz"
>       case _ => i
>     }
>     println(s)
>   }
> 1
> 2
> Fizz
> 4
> Buzz
> ...
> ```
>
> ---------------------------------------------------------------------------
>
> #### Matching on tuple `(Boolean, Boolean)`
>
> ```scala
> @ for (i <- Range.inclusive(1, 100)) {
>     val s = (i % 3 == 0, i % 5 == 0) match {
>       case (true, true) => "FizzBuzz"
>       case (true, false) => "Fizz"
>       case (false, true) => "Buzz"
>       case (false, false) => i
>     }
>     println(s)
>   }
> 1
> 2
> Fizz
> 4
> Buzz
> ...
> ```

> %horizontal-50
>
> #### Matching on Case Classes:
>
> ```scala
> @ case class Point(x: Int, y: Int)
>
> @ def direction(p: Point) = p match {
>     case Point(0, 0) => "origin"
>     case Point(_, 0) => "horizontal"
>     case Point(0, _) => "vertical"
>     case _ => "diagonal"
>   }
>
> @ direction(Point(0, 0))
> res28: String = "origin"
>
> @ direction(Point(1, 1))
> res29: String = "diagonal"
>
> @ direction(Point(10, 0))
> res30: String = "horizontal"
> ```
>
> ---------------------------------------------------------------------------
>
> #### Matching on String Patterns:
>
> ```scala
> @ def splitDate(s: String) = s match {
>     case s"$day-$month-$year" =>
>       s"day: $day, mon: $month, yr: $year"
>     case _ => "not a date"
>   }
>
> @ splitDate("9-8-1965")
> res32: String = "day: 9, mon: 8, yr: 1965"
>
> @ splitDate("9-8")
> res33: String = "not a date"
> ```
>
> *(Note that pattern matching on string patterns only supports simple glob-like
> patterns, and doesn't support richer patterns like Regular Expressions. For
> those, you can use the functionality of the `scala.util.matching.Regex`
> class)*

### Nested Matches

Patterns can also be nested, e.g. this example matches a string pattern within a
`case class` pattern:

```scala
@ case class Person(name: String, title: String)

@ def greet(p: Person) = p match {
    case Person(s"$firstName $lastName", title) => println(s"Hello $title $lastName")
    case Person(name, title) => println(s"Hello $title $name")
  }

@ greet(Person("Haoyi Li", "Mr"))
Hello Mr Li

@ greet(Person("Who?", "Dr"))
Hello Dr Who?
```

Patterns can be nested arbitrarily deeply. The following example matches string
patterns, inside a `case class`, inside a tuple:

```scala
@ def greet2(husband: Person, wife: Person) = (husband, wife) match {
    case (Person(s"$first1 $last1", _), Person(s"$first2 $last2", _)) if last1 == last2 =>
      println(s"Hello Mr and Ms $last1")

    case (Person(name1, _), Person(name2, _)) => println(s"Hello $name1 and $name2")
  }

@ greet2(Person("James Bond", "Mr"), Person("Jane Bond", "Ms"))
Hello Mr and Ms Bond

@ greet2(Person("James Bond", "Mr"), Person("Jane", "Ms"))
Hello James Bond and Jane
```

### Loops and Vals

The last two places you an use pattern matching are inside `for`-loops and `val`
definitions. Pattern matching in `for`-loops is useful when you need to iterate
over collections of tuples:

```scala
@ val a = Array[(Int, String)]((1, "one"), (2, "two"), (3, "three"))

@ for ((i, s) <- a) println(s + i)
one1
two2
three3
```

Pattern matching in `val` statements is useful when you are sure the value will
match the given pattern, and all you want to do is extract the parts you want.
If the value doesn't match, this fails with an exception:

> %horizontal-50
>
> ```scala
> @ case class Point(x: Int, y: Int)
> 
> @ val p = Point(123, 456)
> 
> @ val Point(x, y) = p
> x: Int = 123
> y: Int = 456
> ```
>
> ---------------------------------------------------------------------------
>
> ```scala
> @ val s"$first $second" = "Hello World"
> first: String = "Hello"
> second: String = "World"
> 
> @ val flipped = s"$second $first"
> flipped: String = "World Hello"
>
> @ val s"$first $second" = "Hello"
> scala.MatchError: Hello
> ```

### Pattern Matching on Sealed Traits and Case Classes

Pattern matching lets you elegantly work with structured data comprising case
classes and sealed traits. For example, let's consider a simple sealed trait
that represents arithmetic expressions:

```scala
@ {
  sealed trait Expr
  case class BinOp(left: Expr, op: String, right: Expr) extends Expr
  case class Literal(value: Int) extends Expr
  case class Variable(name: String) extends Expr
  }
```

Where `BinOp` stands for "Binary Operation". This can represent the arithmetic
expressions, such as the following

> %horizontal-50
>
> ```scala
> x + 1
> ```
>
> ---------------------------------------------------------------------------
>
> ```scala
> BinOp(Variable("x"), "+", Literal(1))
> ```
>
> ---------------------------------------------------------------------------
>
> ```scala
> x * (y - 1)
> ```
>
> ---------------------------------------------------------------------------
>
> ```scala
> BinOp(
>   Variable("x"),
>   "*",
>   BinOp(Variable("y"), "-", Literal(1))
> )
> ```
> ---------------------------------------------------------------------------
>
> ```scala
> (x + 1) * (y - 1)
> ```
>
> ---------------------------------------------------------------------------
>
> ```scala
> BinOp(
>   BinOp(Variable("x"), "+", Literal(1)),
>   "*",
>   BinOp(Variable("y"), "-", Literal(1))
> )
> ```

For now, we will ignore the parsing process that turns the string on the left
into the structured `case class` structure on the right: we will cover that in
`%Chapter 19: Parsing Structured Text`. Let us instead consider two things you
may want to do once you have parsed such an arithmetic expression to the `case
class`es we see above: we may want to print it to a human-friendly string, or we
may want to evaluate it given some variable values.

#### Stringifying Our Expressions

Converting the expressions to a string can be done using the following approach:

- If an `Expr` is a `Literal`, the string is the value of the literal
- If an `Expr` is a `Variable`, the string is the name of the variable
- If an `Expr` is a `BinOp`, the string is the stringified left expression,
  followed by the operation, followed by the stringified right expression

Converted to pattern matching code, this can be written as follows:

```scala
@ def stringify(expr: Expr): String = expr match {
    case BinOp(left, op, right) => s"(${stringify(left)} $op ${stringify(right)})"
    case Literal(value) => value.toString
    case Variable(name) => name
  }
```

We can construct some of `Expr`s we saw earlier and feed them into our
`stringify` function to see the output:


> %horizontal-50
>
> ```scala
> @ val smallExpr = BinOp(
>     Variable("x"),
>     "+",
>     Literal(1)
>   )
>
> @ stringify(smallExpr)
> res52: String = "(x + 1)"
> ```
> ---------------------------------------------------------------------------
> ```scala
> @ val largeExpr = BinOp(
>     BinOp(Variable("x"), "+", Literal(1)),
>     "*",
>     BinOp(Variable("y"), "-", Literal(1))
>   )
> 
> @ stringify(largeExpr)
> res54: String = "((x + 1) * (y - 1))"
> ```

#### Evaluating Our Expressions

Evaluation is a bit more complex than stringifying the expressions, but only
slightly. We need to pass in a `values` map that holds the numeric value of
every variable, and we need to treat `+`, `-`, and `*` operations differently:

```scala
@ def evaluate(expr: Expr, values: Map[String, Int]): Int = expr match {
    case BinOp(left, "+", right) => evaluate(left, values) + evaluate(right, values)
    case BinOp(left, "-", right) => evaluate(left, values) - evaluate(right, values)
    case BinOp(left, "*", right) => evaluate(left, values) * evaluate(right, values)
    case Literal(value) => value
    case Variable(name) => values(name)
  }

@ evaluate(smallExpr, Map("x" -> 10))
res56: Int = 11

@ evaluate(largeExpr, Map("x" -> 10, "y" -> 20))
res57: Int = 209
```

Overall, this looks relatively similar to the `stringify` function we wrote
earlier: a recursive function that pattern matches on the `expr: Expr` parameter
to handle each `case class` that implements `Expr`. The cases handling
child-free `Literal` and `Variable` are trivial, while in the `BinOp` case we
recurse on both left and right children before combining the result. This is a
common way of working with recursive data structures in any language, and
Scala's `sealed trait`s, `case class`es and pattern matching make it concise and
easy.

This `Expr` structure and the printer and evaluator we have written are
intentionally simplistic, just to give us a chance to see how pattern matching
can be used to easily work with structured data modeled as `case class`es and
`sealed trait`s. We will be exploring these techniques much more deeply in
`%Chapter 20: Implementing a Programming Language`.

> %example 3 - PatternMatching

## By-Name Parameters

```scala
@ def func(arg: => String) = ...
```

Scala also supports "by-name" method parameters using a `: => T` syntax, which
are evaluated each time they are referenced in the method body. This has three
primary use cases:

1. Avoiding evaluation if the argument does not end up being used
2. Wrapping evaluation to run setup and teardown code before and after the
   argument evaluates
3. Repeating evaluation of the argument more than once

### Avoiding Evaluation

The following `log` method uses a by-name parameter to avoid evaluating the
`msg: => String` unless it is actually going to get printed. This can help avoid
spending CPU time constructing log messages (here via `"Hello " + 123 + "
World"`) even when logging is disabled:

> %horizontal-50
>
> ```scala
> @ var logLevel = 1
> 
> @ def log(level: Int, msg: => String) = {
>     if (level > logLevel) println(msg)
>   }
> ```
>
> ---------------------------------------------------------------------------
> ```scala
> @ log(2, "Hello " + 123 + " World")
> Hello 123 World
> 
> @ logLevel = 3
> 
> @ log(2, "Hello " + 123 + " World")
> <no output>
> ```

Often a method does not end up using all of its arguments all the time. In the
above example, by not computing log messages when they are not needed, we can
save a significant amount of CPU time and object allocations which may make a
difference in performance-sensitive applications.

The `getOrElse` and `getOrElseUpdate` methods we saw in `%Chapter 4: Scala
Collections` are similar: these methods do not use the argument representing the
default value if the value we are looking for is already present. By making the
default value a by-name parameter, we do not have to evaluate it in the case
where it does not get used.

### Wrapping Evaluation

Using by-name parameters to "wrap" the evaluation of your method in some
setup/teardown code is another common pattern. The following `measureTime`
function defers evaluation of `f: => Unit`, allowing us to run
`System.currentTimeMillis()` before and after the argument is evaluated and thus
print out the time taken:


```scala
@ def measureTime(f: => Unit) = {
    val start = System.currentTimeMillis()
    f
    val end = System.currentTimeMillis()
    println("Evaluation took " + (end - start) + " milliseconds")
  }

@ measureTime(new Array[String](10 * 1000 * 1000).hashCode())
Evaluation took 24 milliseconds

@ measureTime { // methods taking a single arg can also be called with curly brackets
    new Array[String](100 * 1000 * 1000).hashCode()
  }
Evaluation took 287 milliseconds
```

There are many other use cases for such wrapping:

- Setting some thread-local context while the argument is being evaluated
- Evaluating the argument inside a `try`-`catch` block so we can handle
  exceptions
- Evaluating the argument in a `Future` so the logic runs asynchronously on
  another thread

These are all cases where using by-name parameter can help.

### Repeating Evaluation

The last use case we will cover for by-name parameters is repeating evaluation
of the method argument. The following snippet defines a generic `retry` method:
this method takes in an argument, evaluates it within a `try`-`catch` block, and
re-executes it on failure with a maximum number of attempts. We test this by
using it to wrap a call which may fail, and seeing the `retrying` messages get
printed to the console.

> %horizontal-50
>
> ```scala
> @ def retry[T](max: Int)(f: => T): T = {
>     var tries = 0
>     var result: Option[T] = None
>     while (result == None) {
>       try { result = Some(f) }
>       catch {case e: Throwable =>
>         tries += 1
>         if (tries > max) throw e
>         else {
>           println(s"failed, retry #$tries")
>         }
>       }
>     }
>     result.get
>   }
> ```
> ---------------------------------------------------------------------------
> ```scala
> @ val httpbin = "https://httpbin.org"
>
> @ retry(max = 5) {
>     // Only succeeds with a 200 response
>     // code 1/3 of the time
>     requests.get(
>       s"$httpbin/status/200,400,500"
>     )
>   }
> call failed, retry #1
> call failed, retry #2
> res68: requests.Response = Response(
>   "https://httpbin.org/status/200,400,500",
>   200,
> ...
> ```

Above we define `retry` as a generic function taking a type parameter `[T]`,
taking a by-name parameter that computes a value of type `T`, and returning a
`T` once the code block is successful. We can then use `retry` to wrap a code
block of any type, and it will retry that block and return the first `T` it
successfully computes.


Making `retry` take a by-name parameter is what allows it to repeat evaluation
of the `requests.get` block where necessary. Other use cases for repetition
include running performance benchmarks or performing load tests. In general,
by-name parameters aren't something you use very often, but when necessary they
let you write code that manipulates the evaluation of a method argument in a
variety of useful ways: instrumenting it, retrying it, eliding it, etc.

We will learn more about the `requests` library that we used in the above
snippet in `%Chapter 12: Working with HTTP APIs`.

> %example 4 - ByName

## Implicit Parameters

An *implicit parameter* is a parameter that is automatically filled in for you
when calling a function. For example, consider the following class `Foo` and the
function `bar` that takes an `implicit foo: Foo` parameter:

```scala
@ class Foo(val value: Int)

@ def bar(implicit foo: Foo) = foo.value + 10
```

If you try to call `bar` without an implicit `Foo` in scope, you get a
compilation error. To call `bar`, you need to define an implicit value of the
type `Foo`, such that the call to `bar` can automatically resolve it from the
enclosing scope:

> %horizontal-50
>
> ```scala
> @ bar
> cmd4.sc:1: could not find implicit
>            value for parameter foo: Foo
> val res4 = bar
>            ^
> Compilation Failed
> ```
>
> ---------------------------------------------------------------------------
>
> ```scala
> @ implicit val foo: Foo = new Foo(1)
> foo: Foo = ammonite.$sess.cmd1$Foo@451882b2
> 
> @ bar // `foo` is resolved implicitly
> res72: Int = 11
> 
> @ bar(foo) // passing in `foo` explicitly
> res73: Int = 11
> ```

Implicit parameters are similar to the *default values* we saw in `%Chapter 3:
Basic Scala`. Both of them allow you to pass in a value explicitly or fall back
to some default. The main difference is that while default values are "hard
coded" at the definition site of the method, implicit parameters take their
default value from whatever `implicit` is in scope at the call-site.

We'll now look into a more concrete example where using implicit parameters can
help keep your code clean and readable, before going into a more advanced use
case of the feature for [Typeclass Inference](#typeclass-inference).

### Passing ExecutionContext to Futures

As an example, code using `Future` needs an `ExecutionContext` value in order to
work. As a result, we end up passing this `ExecutionContext` everywhere, which
is tedious and verbose:

```scala
def getEmployee(ec: ExecutionContext, id: Int): Future[Employee] = ...
def getRole(ec: ExecutionContext, employee: Employee): Future[Role] = ...

val executionContext: ExecutionContext = ...

val bigEmployee: Future[EmployeeWithRole] = {
  getEmployee(executionContext, 100).flatMap(
    executionContext,
    e =>
      getRole(executionContext, e)
        .map(executionContext, r => EmployeeWithRole(e, r))
  )
}
```

`getEmployee` and `getRole` perform asynchronous actions, which we then `map`
and `flatMap` to do further work. Exactly how the `Future`s work is beyond the
scope of this section: for now, what is notable is how every operation needs to
be passed the `executionContext` to do their work. We will will revisit these
APIs in `%Chapter 13: Fork-Join Parallelism with Futures`.

Without implicit parameters, we have the following options:

- Passing `executionContext` explicitly is verbose and can make your code harder
  to read: the logic we care about is drowned in a sea of boilerplate
  `executionContext` passing

- Making `executionContext` global would be concise, but would lose the
  flexibility of passing different values in different parts of your program

- Putting `executionContext` into a thread-local variable would maintain
  flexibility and conciseness, but it is error-prone and easy to forget to set
  the thread-local before running code that needs it

All of these options have tradeoffs, forcing us to either sacrifice conciseness,
flexibility, or safety. Scala's implicit parameters provide a fourth option:
passing `executionContext` implicitly, which gives us the conciseness,
flexibility, and safety that the above options are unable to give us.

### Dependency Injection via Implicits

To resolve these issues, we can make all these functions take the
`executionContext` as an implicit parameter. This is already the case for
standard library operations like `flatMap` and `map` on `Future`s, and we can
modify our `getEmployee` and `getRole` functions to follow suit. By defining
`executionContext` as an `implicit`, it will automatically get picked up by all
the method calls below.

```scala
def getEmployee(id: Int)(implicit ec: ExecutionContext): Future[Employee] = ...
def getRole(employee: Employee)(implicit ec: ExecutionContext): Future[Role] = ...

implicit val executionContext: ExecutionContext = ...

val bigEmployee: Future[EmployeeWithRole] = {
  getEmployee(100).flatMap(e =>
    getRole(e).map(r =>
      EmployeeWithRole(e, r)
    )
  )
}
```

Using implicit parameters can help clean up code where we pass the same shared
context or configuration object throughout your entire application:

- By making the "uninteresting" parameter passing implicit, it can focus the
  reader's attention on the core logic of your application.

- Since implicit parameters can be passed explicitly, they preserve the
  flexibility for the developer in case they want to manually specify or
  override the implicit parameter being passed.

- The fact that missing implicits are a compile time error makes their usage
  much less error-prone than thread-locals. A missing implicit will be caught
  early on before code is compiled and deployed to production.

> %example 5 - ImplicitParameters

## Typeclass Inference

A second way that implicit parameters are useful is by using them to associate
values to types. This is often called a *typeclass*, the term originating from
the Haskell programming language, although it has nothing to do with types and
`class`es in Scala. While typeclasses are a technique built on the same
`implicit` language feature described earlier, they are an interesting and
important enough technique to deserve their own section in this chapter.

### Problem Statement: Parsing Command Line Arguments

Let us consider the task of parsing command-line arguments, given as `String`s,
into Scala values of various types: `Int`s, `Boolean`s, `Double`s, etc. This is
a common task that almost every program has to deal with, either directly or by
using a library.

A first sketch may be writing a generic method to parse the values. The
signature might look something like this:

```scala
def parseFromString[T](s: String): T = ...

val args = Seq("123", "true", "7.5")
val myInt = parseFromString[Int](args(0))
val myBoolean = parseFromString[Boolean](args(1))
val myDouble = parseFromString[Double](args(2))
```

On the surface this seems impossible to implement:

- How does the `parseCliArgument` know how to convert the given `String` into an
  arbitrary `T`?

- How does it know what types `T` a command-line argument can be parsed into,
  and which it cannot? For example, we should not be able to parse a
  `java.net.DatagramSocket` from an input string.

### Separate Parser Objects

A second sketch at a solution may be to define separate parser objects, one for
each type we need to be able to parse. For example:

```scala
trait StrParser[T]{ def parse(s: String): T }
object ParseInt extends StrParser[Int]{ def parse(s: String) = s.toInt }
object ParseBoolean extends StrParser[Boolean]{ def parse(s: String) = s.toBoolean }
object ParseDouble extends StrParser[Double]{ def parse(s: String) = s.toDouble }
```

We can then call these as follows:

```scala
val args = Seq("123", "true", "7.5")
val myInt = ParseInt.parse(args(0))
val myBoolean = ParseBoolean.parse(args(1))
val myDouble = ParseDouble.parse(args(2))
```

This works. However, it then leads to another problem: if we wanted to write a
method that didn't parse a `String` directly, but parsed a value from the
console, how would we do that? We have two options.

#### Re-Using Our StrParsers

The first option is writing a whole new set of `object`s
dedicated to parsing from the console:

```scala
trait ConsoleParser[T]{ def parse(): T }
object ConsoleParseInt extends ConsoleParser[Int]{
  def parse() = scala.Console.in.readLine().toInt
}
object ConsoleParseBoolean extends ConsoleParser[Boolean]{
  def parse() = scala.Console.in.readLine().toBoolean
}
object ConsoleParseDouble extends ConsoleParser[Double]{
  def parse() = scala.Console.in.readLine().toDouble
}

val myInt = ConsoleParseInt.parse()
val myBoolean = ConsoleParseBoolean.parse()
val myDouble = ConsoleParseDouble.parse()
```

The second option is defining a helper method that receives a `StrParser[T]` as
an argument, which we would need to pass in to tell it how to parse the type `T`

```scala
def parseFromConsole[T](parser: StrParser[T]) = parser.parse(scala.Console.in.readLine())

val myInt = parseFromConsole[Int](ParseInt)
val myBoolean = parseFromConsole[Boolean](ParseBoolean)
val myDouble = parseFromConsole[Double](ParseDouble)
```

Both of these solutions are clunky:

1. The first because we need to duplicate all the `Int`/`Boolean`/`Double`/etc.
   parsers. What if we need to parse input from the network? From files? We
   would need to duplicate every parser for each case.

2. The second because we need to pass these `ParseFoo` objects everywhere. Often
   there is only a single `StrParser[Int]` we can pass to
   `parseFromConsole[Int]`. Why can't the compiler infer it for us?

### Solution: Implicit StrParser

The solution to the problems above is to make the instances of `StrParser`
`implicit`:

```scala
trait StrParser[T]{ def parse(s: String): T }
object StrParser{
  implicit object ParseInt extends StrParser[Int]{
    def parse(s: String) = s.toInt
  }
  implicit object ParseBoolean extends StrParser[Boolean]{
    def parse(s: String) = s.toBoolean
  }
  implicit object ParseDouble extends StrParser[Double]{
    def parse(s: String) = s.toDouble
  }
}
```

We put the `implicit object ParseInt`, `ParseBoolean`, etc. in an `object
StrParser` with the same name as the `trait StrParser` next to it. An `object`
with the same name as a `class` that it is defined next to is called a
*companion object*. Companion objects are often used to group together
implicits, static methods, factory methods, and other functionality that is
related to a `trait` or `class` but does not belong to any specific instance.
Implicits in the companion object are also treated specially, and do not need to
be imported into scope in order to be used as an implicit parameter.

Note that if you are entering this into the Ammonite Scala REPL, you need to
surround both declarations with an extra pair of curly brackets `{...}` so that
both the `trait` and `object` are defined in the same REPL command.

Now, while we can still explicitly call `ParseInt.parse(args(0))` to parse
literal strings as before, we can now write a generic function that
automatically uses the correct instance of `StrParser` depending on what type we
asked it to parse:

```scala
def parseFromString[T](s: String)(implicit parser: StrParser[T]) = {
  parser.parse(s)
}

val args = Seq("123", "true", "7.5")
val myInt = parseFromString[Int](args(0))
val myBoolean = parseFromString[Boolean](args(1))
val myDouble = parseFromString[Double](args(2))
```

This looks similar to our initial sketch, except by taking an `(implicit parser:
StrParser[T])` parameter the function can now automatically infer the correct
`StrParser` for each type it is trying to parse.

#### Re-Using Our Implicit StrParsers

Making our `StrParser[T]`s implicit means we can re-use them without duplicating
our parsers or passing them around manually. For example, we can write a
function that parses strings from the console:

```scala
def parseFromConsole[T](implicit parser: StrParser[T]) = {
  parser.parse(scala.Console.in.readLine())
}

val myInt = parseFromConsole[Int]
```

The call to `parseFromConsole[Int]` automatically infers the
`StrParser.ParseInt` implicit in the `StrParser` companion object, without
needing to duplicate it or tediously pass it around. That makes it very easy to
write code that works with a generic type `T` as long as `T` has a suitable
`StrParser`.

#### Context-Bound Syntax

This technique of taking an implicit parameter with a generic type is common
enough that the Scala language provides dedicated syntax for it. The following
method signature:

```scala
def parseFromString[T](s: String)(implicit parser: StrParser[T]) = ...
```

Can be written more concisely as:

```scala
def parseFromString[T: StrParser](s: String) = ...
```

This syntax is referred to as a *context bound*, and it is semantically
equivalent to the `(implicit parser: StrParser[T])` syntax above. When using the
context bound syntax, the implicit parameter isn't given a name, and so we
cannot call `parser.parse` like we did earlier. Instead, we can resolve the
implicit values via the `implicitly` function, e.g.
`implicitly[StrParser[T]].parse`.

#### Compile-Time Implicit Safety

As Typeclass Inference uses the the same `implicit` language feature we saw
earlier, mistakes such as attempting to call `parseFromConsole` with an invalid
type produce a compile error:

```scala
@ val myDatagramSocket = parseFromConsole[java.net.DatagramSocket]
cmd19.sc:1: could not find implicit value for parameter parser:
            ammonite.$sess.cmd11.StrParser[java.net.DatagramSocket]
val myDatagramSocket = parseFromConsole[java.net.DatagramSocket]
                                       ^
Compilation Failed
```

Similarly, if you try to call a method taking an `(implicit parser:
StrParser[T])` from another method that does not have such an implicit
available, the compiler will also raise an error:

```scala
@ def genericMethodWithoutImplicit[T](s: String) = parseFromString[T](s)
cmd2.sc:1: could not find implicit value for parameter parser:
           ammonite.$sess.cmd0.StrParser[T]
def genericMethodWithoutImplicit[T](s: String) = parseFromString[T](s)
                                                                ^
Compilation Failed
```

Most of the things we have done with Typeclass Inference could also be achieved
using runtime reflection. However, relying on runtime reflection is fragile, and
it is very easy for mistakes, bugs, or mis-configurations to make it to
production before failing catastrophically. In contrast, Scala's `implicit`
feature lets you achieve the same outcome but in a safe fashion: mistakes are
caught early at compile-time, and you can fix them at your leisure rather than
under the pressure of a ongoing production outage.

### Recursive Typeclass Inference

We have already seen how we can use the typeclass technique to automatically
pick which `StrParser` to use based on the type we want to parse to. This can
also work for more complex types, where we tell the compiler we want a
`Seq[Int]`, `(Int, Boolean)`, or even nested types like `Seq[(Int, Boolean)]`,
and the compiler will automatically assemble the logic necessary to parse the
type we want.

#### Parsing Sequences

For example, the following `ParseSeq` function provides a `StrParser[Seq[T]]`
for any `T` which itself has an implicit `StrParser[T]` in scope:

```scala
implicit def ParseSeq[T](implicit p: StrParser[T]) = new StrParser[Seq[T]]{
  def parse(s: String) = s.split(',').toSeq.map(p.parse)
}
```

Note that unlike the `implicit object`s we defined earlier which are singletons,
here we have an `implicit def`. Depending on the type `T`, we would need a
different `StrParser[T]`, and thus need a different `StrParser[Seq[T]]`.
`implicit def ParseSeq` would thus return a different `StrParser` each time it
is called with a different type `T`.

From this one defintion, we can now parse `Seq[Boolean]`s, `Seq[Int]`s, etc.

```scala
@ parseFromString[Seq[Boolean]]("true,false,true")
res99: Seq[Boolean] = ArraySeq(true, false, true)

@ parseFromString[Seq[Int]]("1,2,3,4")
res100: Seq[Int] = ArraySeq(1, 2, 3, 4)
```

What we are effectively doing is teaching the compiler how to produce a
`StrParser[Seq[T]]` for any type `T` as long as it has an implicit
`StrParser[T]` available. Since we already have `StrParser[Int]`,
`StrParser[Boolean]`, and `StrParser[Double]` available, the `ParseSeq` method
gives `StrParser[Seq[Int]]`, `StrParser[Seq[Boolean]]`, and
`StrParser[Seq[Double]]` for free.

The `StrParser[Seq[T]]` we are instantiating has a parse method that receives a
parameter `s: String` and returns a `Seq[T]`. We just needed to implement the
logic necessary to do that transformation, which we have done in the code
snippet above.

#### Parsing Tuples

Similar to how we defined an `implicit def` to parse `Seq[T]`s, we could do the
same to parse tuples. We do so below by assuming that tuples are represented by
`key=value` pairs in the input string:

```scala
implicit def ParseTuple[T, V](implicit p1: StrParser[T], p2: StrParser[V]) =
  new StrParser[(T, V)]{
    def parse(s: String) = {
      val Array(left, right) = s.split('=')
      (p1.parse(left), p2.parse(right))
    }
  }
```

This definition produces a `StrParser[(T, V)]`, but only for a type `T` and type
`V` for which there are `StrParser`s available. Now we can parse tuples, as
`=`-separated pairs:

```scala
@ parseFromString[(Int, Boolean)]("123=true")
res102: (Int, Boolean) = (123, true)

@ parseFromString[(Boolean, Double)]("true=1.5")
res103: (Boolean, Double) = (true, 1.5)
```

#### Parsing Nested Structures

The two definitions above, `implicit def ParseSeq` and `implicit def
ParseTuple`, are enough to let us also parse sequences of tuples, or tuples of
sequences:

```scala
@ parseFromString[Seq[(Int, Boolean)]]("1=true,2=false,3=true,4=false")
res104: Seq[(Int, Boolean)] = ArraySeq((1, true), (2, false), (3, true), (4, false))

@ parseFromString[(Seq[Int], Seq[Boolean])]("1,2,3,4,5=true,false,true")
res105: (Seq[Int], Seq[Boolean]) = (ArraySeq(1, 2, 3, 4, 5), ArraySeq(true, false, true))
```

Note that in this case we cannot handle nested `Seq[Seq[T]]`s or nested tuples
due to how we're naively splitting the input string. A more structured parser
handles such cases without issues, allowing us to specify an arbitrarily complex
output type and automatically inferring the necessary parser. We will use a
serialization library that uses this technique in `%Chapter 8: JSON and Binary
Data Serialization`.

Most statically typed programming languages can infer types to some degree: even
if not every expression is annotated with an explicit type, the compiler can
still figure out the types based on the program structure. Typeclass derivation
is effectively the reverse: by providing an explicit type, the compiler can
infer the program structure necessary to provide a value of the type we are
looking for.

In the example above, we just need to define how to handle the basic types - how
to produce a `StrParser[Boolean]`, `StrParser[Int]`, `StrParser[Seq[T]]`,
`StrParser[(T, V)]` - and the compiler is able to figure out how to produce a
`StrParser[Seq[(Int, Boolean)]]` when we need it.

> %example 6 - TypeclassInference

## Conclusion

In this chapter, we have explored some of the more unique features of Scala.
Case Classes or Pattern Matching you will use on a daily basis, while By-Name
Parameters, Implicit Parameters, or Typeclass Inference are more advanced tools
that you might only use when dictated by a framework or library. Nevertheless,
these are the features that make the Scala language what it is, providing a way
to tackle difficult problems more elegantly than most mainstream languages
allow.

We have walked through the basic motivation and use cases for these features in
this chapter. You will get familiar with more use cases as we see the features
in action throughout the rest of this book.

This chapter will be the last in which we discuss the Scala programming language
in isolation: subsequent chapters will introduce you to much more complex topics
like working with your operating system, remote services, and  third-party
libraries. The Scala language fundamentals you have learned so far will serve
you well as you broaden your horizons, from learning about the Scala language
itself to using the Scala language to solve real-world problems.

> %exercise 7 - Simplify
>
> Define a function that uses pattern matching on the `Expr`s we saw earlier to
> perform simple algebraic simplifications:
>
> > %horizontal-50
> >
> > ```
> > (1 + 1)
> > ```
> > ----------------------------------------------------------------------------
> > ```
> > 2
> > ```
> > ----------------------------------------------------------------------------
> > ```
> > ((1 + 1) * x)
> > ```
> > ----------------------------------------------------------------------------
> > ```
> > (2 * x)
> > ```
> > ----------------------------------------------------------------------------
> > ```
> > ((2 - 1) * x)
> > ```
> > ----------------------------------------------------------------------------
> > ```
> > x
> > ```
> > ----------------------------------------------------------------------------
> > ```
> > (((1 + 1) * y) + ((1 - 1) * x))
> > ```
> > ----------------------------------------------------------------------------
> > ```
> > (2 * y)
> > ```

> %exercise 8 - Backoff
>
> Modify the `def retry` function earlier that takes a by-name parameter and
> make it perform an exponential backoff, sleeping between retries, with a
> configurable initial `delay` in milliseconds:
>
> ```scala
> retry(max = 50, delay = 100 /*milliseconds*/) {
>   requests.get(s"$httpbin/status/200,400,500")
> }
> ```


> %exercise 9 - Deserialize
>
> Modify the typeclass-based `parseFromString` method we saw earlier to take a
> JSON-like format, where lists are demarcated by square brackets with
> comma-separated elements. This should allow it to parse and construct
> arbitrarily deep nested data structures automatically via typeclass inference:
>
> ```scala
> @ parseFromString[Seq[Boolean]]("[true,false,true]") // 1 layer of nesting
> res1: Seq[Boolean] = List(true, false, true)
>
> @ parseFromString[Seq[(Seq[Int], Seq[Boolean])]]( // 3 layers of nesting
>     "[[[1],[true]],[[2,3],[false,true]],[[4,5,6],[false,true,false]]]"
>   )
> res2: Seq[(Seq[Int], Seq[Boolean])] = List(
>   (List(1), List(true)),
>   (List(2, 3), List(false, true)),
>   (List(4, 5, 6), List(false, true, false))
> )
> 
> @ parseFromString[Seq[(Seq[Int], Seq[(Boolean, Double)])]]( // 4 layers of nesting
>     "[[[1],[[true,0.5]]],[[2,3],[[false,1.5],[true,2.5]]]]"
>   )
> res3: Seq[(Seq[Int], Seq[(Boolean, Double)])] = List(
>   (List(1), List((true, 0.5))),
>   (List(2, 3), List((false, 1.5), (true, 2.5)))
> )
> ```
>
> A production-ready version of this `parseFromString` method exists in
> `upickle.default.read`, which we will see in `%Chapter 8: JSON and Binary Data
> Serialization`.

> %exercise 10 - Serialize
>
> How about using typeclasses to generate JSON, rather than parse it? Write a
> `writeToString` method that uses a `StrWriter` typeclass to take nested values
> parsed by `parseFromString`, and serialize them to the same strings they were
> parsed from.
>
> ```scala
> @ writeToString[Seq[Boolean]](Seq(true, false, true))
> res1: String = "[true,false,true]"
> 
> @ writeToString(Seq(true, false, true)) // type can be inferred
> res2: String = "[true,false,true]"
>
> @ writeToString[Seq[(Seq[Int], Seq[Boolean])]](
>     Seq(
>       (Seq(1), Seq(true)),
>       (Seq(2, 3), Seq(false, true)),
>       (Seq(4, 5, 6), Seq(false, true, false))
>     )
>   )
> res3: String = "[[[1],[true]],[[2,3],[false,true]],[[4,5,6],[false,true,false]]]"
> 
> @ writeToString(
>     Seq(
>       (Seq(1), Seq((true, 0.5))),
>       (Seq(2, 3), Seq((false, 1.5), (true, 2.5)))
>     )
>   )
> res4: String = "[[[1],[[true,0.5]]],[[2,3],[[false,1.5],[true,2.5]]]]"
> ```

