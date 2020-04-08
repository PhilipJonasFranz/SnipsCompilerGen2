# Snips Compiler Gen.2 v.1.0.1-RELEASE
## What is Snips?
 Snips is a lightweight C/Java oriented proramming language. This brings familiar programming concepts to 
 the table, like functions, conditionals, loops, arrays, global variables and a wide roster of built in 
 operators, as well as support for functionality like recursion. 
 Currently supported data types are Integers and Booleans, as well as multi-dimensional arrays of said 
 types.
 Currently supported statement structures are if, if-else, else, while, do-while, for, switch, break, 
 continue and return.
## Usage
### How to use
 If you just want to use the compiler, you can use the compiled and wrapped .exe in release/. The lib/ folder includes some libary functions that the compiler uses. To compile enter in the console "snips [Full Path to your File]". With "snips -help" you can get more information on the arguments.

If you want to run the code instead, you can run the either the CompilerDriver.java with the same arguments as up below, or you can run the TestDriver.java. This will run all the tests and verify the correct functionality of the compiler. The Arguments here are either a path to a file name, f.E. "res\Test\Arith\Static\test_00.txt" or a list of directories, f.E. "res\Test\Arith\ res\Test\Stack\".
### Code Examples
 Code examples and testcases can be found under res/Test/.
## Feature Roadmap
### v.2.0.0-RELEASE: Extended type system, extended functionality
 - Implement Arrays WIP: Global Arrays
 - Implement Pointers
 - Implement Heap System
 - Implement Addressof and Dereference Operators
 - Implement Pass-by-value and pass-by-reference for non-primitive types
 
### v.3.0.0-RELEASE: Complex type system, advanced operations
 - Implement increment and decrement
 - Implement Arithmetic Operators
 - Implement Direct ASM injection
 - Implement Structs

### v.4.0.0-RELEASE: Optimization, extended compilation control
 - Implement AST Optimizer
 - Implement Optimizer Annotations
