# Snips Compiler Gen.2 v.3.4.7-RELEASE
## Some words in advance
 This project was started and still is for educational purposes. The programming language Snips, the Compiler and all included modules are not following any standards and are built to function well only for this project. Results procuded by the compiler and included modules may contain errors and are not thought for any production environment. The project and all its included modules are still under development and are subject to change.
 
## What is Snips?
 Snips is a lightweight C/Java oriented programming language. This brings familiar programming concepts to 
 the table, like functions, conditionals, loops, arrays, pointers, global variables and a wide roster of built in operators, as well as support for functionality like recursion. Also, more advanced features like imports, structs, templating, heap functionality, namespaces, exception handling, predicates and inline assembly are supported.
 
 Currently supported data types are Integers and Booleans, Chars and Strings, Enums, Predicates as well as multi-dimensional arrays of said types. Provisos act as a special, dynamic type that can take the shape of any other type. The can f.E. be used to re-use the same struct with different field types. Also, functions can pass and receive proviso types, allowing them to handle various types.
 
 Currently supported statement structures are if, if-else, else, while, do-while, for, switch, break, continue, try/watch, signal and return.
 
 You can find more information on the language and the libraries in the [Official Documentation](https://github.com/PhilipJonasFranz/SnipsCompilerGen2/blob/develop/doc/Snips%20Documentation.pdf).
### The compiler
 The compiler pipeline consists out of various stages:
 
 - Reading the code to compile
 - Pre Processing and resolving static imports
 - Scanning the code and converting it into a token stream
 - Parsing the token stream, creating an AST
 - Processing dynamic imports
 - Context checking and creating the DAST
 - Code Generation, create list of Assembly instructions
 - Assembly Optimizer, local changes while keeping original functionality

 The compiler uses a built-in system libary, located at release/lib. 
 
 The compiler will output ARM Assembly. See https://iitd-plos.github.io/col718/ref/arm-instructionset.pdf for more information. 
 
## Usage & Setup
### Running the executable
 If you just want to use the compiler, you can use the compiled and wrapped .exe in release/. The lib/ folder includes some libary functions that the compiler uses. To compile enter in the console "snips (Full Path to file to compile)". With "snips -help" you can get more information on the arguments.

### Running the code
If you want to run the code, you can run either the CompilerDriver.java with the same arguments as up below, or you can run the TestDriver.java. This will run all the tests and verify the correct functionality of the compiler. The Arguments here are either a path to a file name, f.E. "res/Test/Arith/Static/test_00.txt" or a list of directories, f.E. "res/Test/Arith/ res/Test/Stack/".
### Code Examples
 Code examples and testcases can be found under res/Test/ and under release/examples/.
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
 
All instructions do support the condition field. If you compile your assembly code with the Assembler mentioned up below you can be sure for it to work since the Assembler roughly implements the feature set of the Processor.
### XML-Parser
 Under src/REv/Modules/Tools/ you can also find a basic implementation of a XML-Parser, that converts a given .xml file to a tree structure. 
### Utility
 Under src/REv/Modules/Tools/Util.java you can find some utility functions for binary arithmetic, as well as File-I/O and a method that sets up the Processor with a provided configuration file. This is used by the TestDriver.java to set up the runtime environment. 
## Feature Roadmap
### v.4.0.0-RELEASE: Function Pointers/Lambdas, Parser/Attachment rework, Struct Type Vendor
 - Rework Parser to use Vendor System, implement improved comment and directive attatching to syntax elements
 - Rework Struct Type System to use SSOT Vendor
 
### v.5.0.0-RELEASE: Optimization, extended compilation control
 - Implement AST Optimizer
 - Implement Optimizer Annotations

## License & Copyright
 © Philip Jonas Franz
 
 Licensed under [Apache License 2.0](LICENSE).
