<p align="center">
  <img width="115" height="75" src="https://github.com/PhilipJonasFranz/SnipsCompilerGen2/blob/develop/res/sn-logo.png?raw=true">
</p>

# Snips Compiler Gen.2 [![version](https://img.shields.io/badge/version-4.6.0-green.svg)](https://semver.org) [![status](https://img.shields.io/badge/status-development-red.svg)](https://semver.org)

![size](https://img.shields.io/github/repo-size/PhilipJonasFranz/SnipsCompilerGen2) ![size](https://img.shields.io/github/languages/code-size/PhilipJonasFranz/SnipsCompilerGen2)

## Some words in advance
 This project was started for educational purposes. The programming language Snips, the Compiler and all included modules are not following any standards and are built to function well only for this project. Results produced by the compiler and included modules may contain errors and are not thought for any production environment. The project and all its included modules are still under development and are subject to change.
 
## Contents

- [What is Snips?](#what-is-snips)
- [The Compiler](#the-compiler)
- [Usage & Setup](#usage-and-setup)
- [Included Modules](#included-modules)
- [License & Copyright](#license-and-copyright)

## What is Snips?
### Short introduction
 Snips is a lightweight C/Java oriented programming language. This brings familiar programming concepts to 
 the table, like functions, conditionals, loops, arrays, references, global variables and a wide roster of built in operators. Also, more advanced features like imports, interfaces, structs, polymorphism, templating, heap functionality, namespaces, exception handling, predicates and inline assembly are supported.
 
 Snips is mainly a procedural language, but through interfaces, struct polymorphism and struct nesting, an object-oriented programming style can be achieved.

Currently supported data types are:

- Primitive types : `int`, `bool`, `char`
- Custom types    : `interface`, `struct`, `enum`
- Special types   : `func`, `proviso`, `void`

as well as pointers and arrays of said types. Structs can be created capsuling fields of any type. The Void Type can be used as a type wildcard. Provisos act as a special, dynamic type that can take the shape of any other type. The can f.E. be used to re-use the same struct with different field types. Also, functions can pass and receive proviso types, allowing them to handle various types.

 Some of the core features of Snips are shown in this little code example below:
 
```c
struct X<T> {
    T val;
  
    T get() {
        return self->val;
    }
  
    /* Construct a new struct instance */
    static X<T> create<T>(T val) {
        return X<T>::(val);
    }
}
  
int main() {
    // Push the struct on the heap and create a pointer
    X<int>* x = init<>(X::create<int>(12));
    return (x->get<>() == 12)? 25 : 5;
}
```
 
 You can find more information about the language in the [Quick-start Documentation](doc/README.md). For a more in-depth guide and information about the included libraries, see [Official Documentation](https://github.com/PhilipJonasFranz/SnipsCompilerGen2/blob/develop/doc/Snips%20Documentation.pdf).
 
## The compiler
 The compiler pipeline consists out of these stages:
 
 - Reading the code to compile
 - Pre Processing and resolving static imports
 - Scanning the code and converting it into a token stream
 - Parsing the token stream, creating an AST
 - Processing dynamic imports
 - Context checking and creating the DAST
 - Code Generation, create list of Assembly instructions
 - Assembly Optimizer, rule-based optimizations
 - Linker, resolves assembly imports of output

 The compiler uses a built-in [System Library](release/lib "release/lib"), located at `release/lib`. 
 
 The compiler will output ARM Assembly. See [ARM Instruction Set](https://iitd-plos.github.io/col718/ref/arm-instructionset.pdf) for more information. 
 
## Usage and Setup
### Running the compiler executable
 The compiler executable can be run with `snips [Path to file] ARGS`. The possible arguments can be found below:

 |     Argument     |        Functionality                                      |   Default Value    |
 | ---------------- | --------------------------------------------------------- | -----------------  |
 | `-help`          | Print argument list                                       | `false`            |
 | `-info`          | Print compiler version                                    | `false`            |
 | `-log`           | Print log in console                                      | `false`            |
 | `-com`           | Remove comments from output                               | `false`            |
 | `-warn`          | Disable warnings in console                               | `false`            |
 | `-imp`           | Print out imported libraries                              | `false`            |
 | `-opt`           | Disable Assembly optimizer                                | `false`            |
 | `-ofs`           | Optimize for filesize                                     | `false`            |
 | `-rov`           | Ignore errors from modifiers                              | `false`            |
 | `-sid`           | Disable Struct IDs                                        | `false`            |
 | `-imm`           | Print out immediate data                                  | `false`            |
 | `-viz`           | Disable ANSI colors in log                                | `false`            |
 | `-o`             | Build the object file only, do not link output            | `false`            |
 | `-r`             | Recursively re-compile all included modules               | `false`            |
 | `-R`             | Same as `-r`, but prune all existing module dumps         | `false`            |
 | `-L`             | Link given program, requires input to be .s file          | `false`            |
 | `-o [Path]`      | Specify output path                                       | Directory of input |
 
### Running the code
If you want to run the code, you can run either the CompilerDriver.java with the same arguments as up below, or you can run the TestDriver.java. This will run all the tests and verify the correct functionality of the compiler. The Arguments here are either a path to a file name, f.E.`res/Test/Arith/Static/test_00.txt` or a list of directories, f.E. `res/Test/Arith/ res/Test/Stack/`.

### Code Examples, Tests
 [Code Examples](release/examples/) can be found under `release/examples`, [Testcases](res/Test/) can be found under `res/Test/`.
 
## Included Modules
### Assembler
 Under `src/REv/Modules/RAsm/` you can find an [Assembler](src/REv/Modules/RAsm/) that is used by the test driver to assemble the outputted program and run it on the SWARM32Pc. The Assembler will work with the code that the Compiler outputs, but it does not fully support all instructions and does not follow any compilation conventions. I do not recommend to use it anywhere else than in combination with this project.
 
### SWARM32Pc
 Under `src/REv/CPU/` you can find a [Virtual Machine](src/REv/CPU/), which implements a subset of the ARM Instruction Set. Again, this is used to test the output of the Compiler. Since the processor does not support all instructions i would not recommend to use it somewhere else. Supported instructions are: 
 - `b`, `bl`, `bx`
 - All data processing operations
 - `mrs`, `msr`
 - `mul`, `mla`
 - `ldr`, `str`
 - `ldm`, `stm`
 
All instructions do support the condition field. If you compile your assembly code with the Assembler mentioned up below you can be sure for it to work since the Assembler roughly implements the feature set of the Processor.

### XML-Parser
 Under `src/Util/XMLParser.java` you can also find a basic implementation of an [XML-Parser](src/Util/XMLParser.java), that converts a given .xml file to a tree structure. 
 
### Utility
 Under `src/REv/Modules/Tools/Util.java` you can find some [Utility Functions](src/REv/Modules/Tools/Util.java) for binary arithmetic, as well as File-I/O and a method that sets up the Processor with a provided configuration file. This is used by the TestDriver.java to set up the runtime environment. 
 
## License and Copyright
 © Philip Jonas Franz
 
 Licensed under [Apache License 2.0](LICENSE). 
