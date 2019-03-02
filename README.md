# Internal Rate of Return

The internal rate of return of an investment made at irregular intervals is the constant rate of return which would yield the equivalent value.

For example, suppose you purchased $1000 in a stock on January 15, 2016 then $2500 on February 8, 2016 and finally another $1000 on April 17, 2016.  The value of the stock on August 24, 2016 is $5050.  The internal rate of return could be computed by:

```` java
double rate = new Xirr(
        new Transaction(-1000, "2016-01-15"),
        new Transaction(-2500, "2016-02-08"),
        new Transaction(-1000, "2016-04-17"),
        new Transaction( 5050, "2016-08-24")
    ).xirr();
System.out.println(rate); // Prints 0.2504234710540838
````

This means annualized rate of return is 25.04%.  That is, an equivalent investment would be a savings account of with an interest rate of 25.04%.  (And if you happen to know of such a savings account, please let the author know immediately.)

This calculation can be used to compare disparate investments.  See javadoc for more details.

# Newton-Raphson method

For this implementation of xirr I decided to implement the Newton-Raphson method myself within the library.  It was written in a general purpose manner, so if you were just looking for that feel free to use it.

# Implementation Details

To compute the irregular rate of return, you must find the constant rate of return which yields a present value of zero over the set of transactions.  The present value of a transaction is determined by the formula <code>A(1+r)<sup>Y</sup></code>, where `A` is the `amount`, `Y` is the duration of the investment represented by the transaction in years and  `r` is the rate to solve for.  The sum of the present values is the function for which we need to find the zero.

To find the zero of a function, we use the Newton-Raphson method as implemented in the NewtonRaphson class.  To use Newton's method, we need the derivative of the present value with respect to `r`. Fortunately this is easily determined using the power rule.  The derivative is the sum of the terms <code>AY(1+r)<sup>Y-1</sup></code> for which `Y` is not zero.

I had a very elegant proof of the above but unfortunately the margin is too small to contain it.

# Maven

The library is available in the Maven Central repository, use the following dependency in your pom.xml:

```
<dependency>
  <groupId>org.decampo</groupId>
  <artifactId>xirr</artifactId>
  <version>0.1</version>
</dependency>
```

You may want to verify that the version is the latest by checking https://search.maven.org/artifact/org.decampo/xirr/.
