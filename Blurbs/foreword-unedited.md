Scala as a language delegates much to libraries. Instead of many primitive
concepts and types it offers a few powerful abstractions that lets libraries
define flexible interfaces that are natural to use. A particular goal is that
code using a library-provided construct should not look different from code that
uses a built-in language construct. Haoyi Li's libraries and tools are a
beautiful example of what can be built on these foundations. There's a whole
universe of them: from interacting with the operating system (os-lib), testing
(utest), serialization (upickle), parsing (fastparse), web-services (cask) to a
full-featured REPL (ammonite) and build tool (mill). A common thread of all
these libraries and tools is that they are simple and user friendly. Hands-On
Scala is a great resource for learning how to use Scala and these libraries and
tools to build practical applications in a straightforward way. It covers a lot
of ground with over a hundred worked out mini-applications. Its code-first
philosophy gets to the point quickly and with a minimum of fuss. This works
well, since the code is simple and therefore easy to understand by itself with
only a few explanations needed.

Making things simple is not easy. It requires restraint and thought and
expertise. Haoyi has laid out his approach in an illuminating blog post titled
Strategic Scala Style: The Principle of Least Power. It argues that one should
always reach for the least powerful language feature and architecture that
achieves the requirements. Less power means more predictability what the code
does which translates to faster understanding and easier maintenance. I see
Hands-On Scala as the Principle of Least Power in action - in fact, that would
have been a nice alternative title! It shows that one can build powerful
applications without having to buy into complex frameworks. Scala's abstraction
features do allow one to formulate advanced design elements, such as typeclass
derivations, free monads, monad transformers, tagless-final architectures or
recursion schemes, to name just a few. They certainly have their place in some
scenarios. Some of them even show up in this book. But for many tasks this
generality and abstraction is simply not needed and the Principle of Least Power
states that they should then not be used. Following this principle is a big
factor in what makes Haoyi's code so easy to understand and his libraries so
easy to use. Hands-On Scala is the best way to learn about this way of doing
things, and a great resource to get stuff done using Scala.

