# Snips Compiler Gen.2 v.1.2.3-RELEASE
## What is Snips?
 Snips is a lightweight C/Java oriented proramming language. This brings familiar programming concepts to 
 the table, like functions, conditionals, loops, arrays, pointers, global variables and a wide roster of built in 
 operators, as well as support for functionality like recursion. 
 Currently supported data types are Integers and Booleans, as well as multi-dimensional arrays of said 
 types.
 Currently supported statement structures are if, if-else, else, while, do-while, for, switch, break, 
 continue and return.
### The compiler
 The compiler outputs ARM Assembly. 
## Usage
### How to use
 If you just want to use the compiler, you can use the compiled and wrapped .exe in release/. The lib/ folder includes some libary functions that the compiler uses. To compile enter in the console "snips [Full Path to file to compile]". With "snips -help" you can get more information on the arguments.

If you want to run the code instead, you can run either the CompilerDriver.java with the same arguments as up below, or you can run the TestDriver.java. This will run all the tests and verify the correct functionality of the compiler. The Arguments here are either a path to a file name, f.E. "res/Test/Arith/Static/test_00.txt" or a list of directories, f.E. "res/Test/Arith/ res/Test/Stack/".
### Code Examples
 Code examples and testcases can be found under res/Test/.
## Included Modules
### Assembler
 Under src/REv/Modules/RAsm/ you can find an assembler that is used by the test driver to assemble the outputted program and run it on the SWARM32Pc. The Assembler will work with the code that the Compiler outputs, but it does not fully support all instructions and does not follow any compilation conventions. I do not recommend to use it anywhere else than in combination with this project.
### SWARM32Pc
 Under src/REv/CPU/ you can find a virtual machine, which implements a subset of the ARM Instruction Set. Again, this is used to test the output of the Compiler. Since the processor does not support all instructions i would not recommend to use it somewhere else. Supported instructions are: 
 - b, bl, bx
 - All data processing operations
 - mrs, msr
 - mul, mla
 - ldr, str
 
All instructions do support the condition field. See https://iitd-plos.github.io/col718/ref/arm-instructionset.pdf for more information. If you compile your assembly code with the Assembler mentioned up below you can be sure for it to work since the Assembler roughly implements the feature set of the Processor.
### XML-Parser
 Under src/REv/Modules/Tools/ you can also find a basic implementation of a XML-Parser, that converts a given .xml file to a tree structure. 
### Utility
 Under src/REv/Modules/Tools/Util.java you can find some utility functions for binary arithmetic, as well as File-I/O and a method that sets up the Processor with a provided configuration file. This is used by the TestDriver.java to set up the runtime environment. 
## Feature Roadmap
### v.2.0.0-RELEASE: Extended type system, extended functionality
 - Implement Direct ASM injection
 
### v.3.0.0-RELEASE: Complex type system, advanced operations
 - Implement increment and decrement
 - Implement Arithmetic Operators
 - Implement Heap System
 - Implement Structs

### v.4.0.0-RELEASE: Optimization, extended compilation control
 - Implement AST Optimizer
 - Implement Optimizer Annotations
