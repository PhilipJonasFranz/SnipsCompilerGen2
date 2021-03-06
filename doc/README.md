# Snips Language Guide

 This document aims to give an overview of the Snips programming language and it's mechanisms. If you want a more in-depth guide and information about included libraries, see the [Full Documentation](https://github.com/PhilipJonasFranz/SnipsCompilerGen2/blob/develop/doc/Snips%20Documentation.pdf).

## Contents

- [Type System](#type-system)
- [Expressions](#expressions)
  - [Operators](#operators)
  - [Examples](#examples)
- [Statements](#statements)
  - [Data Statements](#data-statements)
  - [Flow Statements](#flow-statements)
- [Program Elements](#program-elements)
  - [Global Variables](#global-variables)
  - [Function](#function)
  - [Interface Typedef](#interface-typedef)
  - [Struct Typedef](#struct-typedef)
  - [Enum Typedef](#enum-typedef)
- [Advanced Features](#advanced-features)
   - [Exceptions](#exceptions)
   - [Direct ASM](#direct-asm)
   - [Modules](#modules)
   - [Directives](#directives)
   - [Header Files](#header-files)
   - [Heap Functionality](#heap-functionality)
   - [Namespaces](#namespaces)
   - [Visibility Modifiers](#visibility-modifiers)
   - [Parameter Covering](#parameter-covering)
   - [Struct Nesting](#struct-nesting)
- [Further Reading](#further-reading)
   - [Function Overloading](#function-overloading)
   - [Auto Proviso](#auto-proviso)
   - [Placeholder Atoms](#placeholder-atoms)
   - [Register Atoms](#register-atoms)
   - [Predicates and Inline Functions](#predicates-and-inline-functions)
   - [Variable Shadowing](#variable-shadowing)
   - [Dont care arrays](#dont-care-arrays)
   - [Operator Overloading](#operator-overloading)

## Type System

The type-system is a big part of the foundation of the language. While being strict in some areas, the type system still leaves space for more free usage of types.

### Built-in types

Types are seperated in three classes: Primitives, Composit and Special Types. Primitive types are:

 |         Type         |                  Description                  |
 | -------------------- | --------------------------------------------- |
 | `INT` (Integer)      | A number ranging from `2^-32 to (2^32)-1`     |
 | `BOOL` (Boolean)     | A logic type with two states, `true or false` |
 | `CHAR` (Character)   | A single character encoded in UTF-8           |
 | `FUNC` (Function)    | Holds the address of a function, is callable  |
 | `ENUM` (Enumeration) | Holds a value from an enumeration             |
 | `VOID` (Void)        | Universal type that matches any other type    |
 | `NULL` (Null)        | Type that holds reference to null-pointer     |
 
 Composit types are built up from other types and are combined into a new type. Composit types are:
 
 |          Type        |                         Description                         |
 | -------------------- | ----------------------------------------------------------- |
 | `STRUCT`             | A collection of types and functions                         |
 | `ARRAY`              | A collection of equal types                                 |
 | `INTERFACE`          | Provides abstraction by defining functions to be implemented|
 | `POINTER`            | Holds address reference to other target type                |

Special types are managed during compile time, and are fixed during runtime. Special types are:

 |          Type        |                         Description                                 |
 | -------------------- | ------------------------------------------------------------------- |
 | `PROVISO`            | A shapeshifter type, used for templating and re-usability           |
 | `AUTO`               | Used as a shortcut. Will be replaced with acutal type automatically. Only available for declarations with initial value. The type of the value will be used as the actual type. |


## Expressions

Expressions are inductiveley defined. From a wide range of operators, expressions can be built up into a complex term. At any point, parentheses can be inserted between sub-expressions. Evaluating expressions will always yield a return value. This means that expressions must have a target like a declaration or assignment. During compilation, operator precedence is taken into account. In the table below, the operators are listed in the correct precedence, starting with the highest precedence.

### Operators

 |         Operator       |       Code Example        |                               Description                          |
 | ---------------------- | ------------------------- | ------------------------------------------------------------------ |
 | Atom, Enum Select, Call| `a`, `State.Ok`, `f()`    | Atoms can be a variable reference, direct value or a function call |
 | Array Selection        | `a [0]`, `m [0] [1]`      | Select from multi-dimensional array. Results in value or array     |
 | Structure Selection    | `st.v`, `st->v`           | Select a field from a struct                                       |
 | Increment, Decrement   | `i++`, `i--`              | Get the value from the variable and increment/decrement the origin |
 | Unary Minus            | `-val`                    | Creates the numeric negation of the base value                     |
 | Negation               | `!b`, `~i`                | Creates the boolean or bitwise negation of the base value          |
 | Type Cast              | `(int) true`              | Set the type of the base value to the given type in compile time   |
 | Dereference            | `*p`                      | Loads the value from the given pointer address                     |
 | Address Of             | `&val`                    | Creates a pointer address reference to the base variable           |
 | Size Of                | `sizeof(v)`, `sizeof(int)`| Evaluates the wordsize of the type of the variable or given type   |
 | Mul, Div, Mod          | `a * b`, `a / b`, `a % b` | The multiplication, divison or modulus operation of two operands   |
 | Add, Sub               | `a + b`, `a - b`          | The addition or subtraction operation of two operands              |
 | Shift left, Shift right| `a << b`, `a >> b`        | Logical shift operation of the first operand by the second operand |
 | Comparison             | `a <= b`, `a == b`        | Compares two operands based on operator and returns boolean result |
 | Bitwise and, xor, or   | `a & b`, `a ^ b`, `a \| b`| The bitwise and, xor and or operation of two operands              |
 | Logical and, or        | `a && b`, `a \|\| b`      | The logical and, xor and or operation of two operands            |
 | Ternary                | `(c)? a : b`              | Selects one of two operands based on condition                     |
 | Array Initialization   | `{1, 2, a, b + 1}`        | Creates a new array from the given values                          |
 | Struct Initialization  | `Struct::(1, c, a + 3)`   | Creates a new struct instance from the given values                |
 
### Examples

Now, with the defined operators, we can inductiveley define expressions. For example:

- `a * (4 + b)`
- `*(p + 4) % 5`
- `(int) ((b)? true : eval(a, c))`
- `Data::(5, true, 'c');`

## Statements

Statements make up the context around expressions and provide an infrastructure for data management and flow. Using statements in a clever and effective way is critical to creating efficient and well-defined programs. 


### Data Statements

Data statements provide the infrastructure for data in the program. These statements are:

#### Declaration

A declaration creates a new variable in the current scope and assigns a value to it. The data type of the declaration and value has to match.

```c
  int i = 10;
```

#### Assignment

An assignment re-assigns a value to an existing variable. The data type of the value and the variable has to match.

```c
  i = eval(i + 4);
```

The data target from an assignment can vary, as well as the assign arithmetic. Assign arithmetic can be used to alter the value of the variable not only using the new value, but the old value of the variable as well:

 |   Assign Operator    |                         Description                                    |
 | -------------------- | ---------------------------------------------------------------------- |
 | `=`                  | Simple assignment to the variable                                      |
 | `+=`, `-=`           | Adds or subtracts new value from current value                         |
 | `*=`, `/=`, `%=`     | Multiplis, divides or modulus operation on current value with new vaue |
 | `&=`, `\|=`, `^=`    | Bitwise and, or, xor with new value and current value                  |
 | `&&=`, `\|\|=`       | Logical and, or with new value and current value                       |
 | `<<=`, `>>=`         | Shift current value by new value                                       |

The data target can be direct, when the value is assigned to the variable name directley. But data targets hidden behind a struct or array select, or even behind a pointer can be used:

 |         Operator       |       Code Example        |                               Description                          |
 | ---------------------- | ------------------------- | ------------------------------------------------------------------ |
 | Direct                 | `a = 25;`                 | Assigns the value to the variable directley                        |
 | Array Selection        | `a [i] = c * d;`          | Assigns the value to the element at the selected index in the array|
 | Pointer                | `*(p + 2) = get();`       | Assigns to the address the expression in the dereference defines   |
 | Struct Select          | `a->b = x->v * 3;`        | Assigns to the selected field in the struct                        |


### Flow Statements

Flow statements alter the flow of the program. This is done by creating branches in the code flow, either through a function call or a conditional statement. Flow statements are:

#### Function call

Calls the given function with given parameters and discards the return value.

```c
  incr(p);
```

#### Return Statement

Returns from the current function with or without a return value. The type of the returned value must match witht the function return type.

```c
  return;
  
  return a + 4;
```

#### If Statement

Creates a branch in the program based on the evaluated value of a condition. The statements in the if-block are executed if the condition evaluates to true. An optional `else if`/`else` statement can be attatched. An `if`-statement can have multiple `else if` statements, but only one else statement. The if statement whiches condition evaluates to true is executed, all other statements attatched are skipped. If an else statement is present, and all conditions are false, the else statemnet block is executed.

```c
  if (a < 10) {
    ...
  }
  else if (b && c) {
    ...
  }
  else {
    ...
  }
```

#### Switch Statement

The switch statement takes a variable and compares in multiple cases its value. Once one case matches, the corresponding statements are executed and the program jumps to the end of the switch statement. If no case matches, the default case is executed. This implies that for each switch statement, exactly one default case is present.

```c
  switch (var) {
    case (10) : {
      ...
    }
    case (15) : {
      ...
    }
    default : {
      ...
    }
  }
```

#### For Loop

The for loop can repeats the same code section multiple times, based on the iterator and the condition. Upon entering, an iterator is declared. Then for every loop execution, the condition is checked. If the condition is false, the loop terminates. If not, the code section is executed and the increment operation is executed.

```c
  for (int i = 0; i < 10; i++) ...
  
  for (ListNode* n = head; n != null; n = n->next) ...
```

#### For-Each Loop

The for-each loop can, like the for loop, iterate over a code section, but has automatic iterator wiring. The loop can iterate over an array with fixed length or over a pointer. In this case, the loop requires a second range parameter, that defines the length of the array. In each iteration, the current value is loaded into the iterator. For example:

```c
  // Iterate over fixed size array
  for (int i : array) ...
  
  // Iterate over array via pointer with range
  for (int i : p, 10) ...
  
  // Iterate over array within struct with fixed size
  for (int i : s->arr) ...
```

In all of these examples, changes made to the iterator `i` within the loops body are not written back into the data source. This can be enabled by using brackets instead of parenthesis:

```c
  for [int i : array] ...
  
  for [int i : p, 10] ...
  
  for [int i : s->arr] ...
```

Changes made to the iterator will result in the data source be modified. For example:

```c
  int [5] arr = {1, 5, 2, 1, 3};
  for [int i : arr] i += 10;
```

The code will increment each value in the array by `10`, resulting in the array `{11, 15, 12, 11, 13}`. If no changes were made to the iterator or the changes made are not supposed to be written back, it is recommended to use the basic version of the for-Each loop, since it can be more performant.

#### While Loop

The while loop executes a code section as long as a condition is true. The loop checks the condition at the start of each iteration.

```c
  while (a < b) {
    ...
  }
```

#### Do-While Loop

The do-While loop executes a code section as long as a condition is true, but checks the condition after each iteration.

```c
  do {
    ...
  } while (a < b);
```

#### Continue and Break Statement

The continue statement causes the program to jump to the end of the current loop iteration and immediately starts with the execution of the next loop iteration. The break statement jumps to the end of the current loop iteration and quits the loop execution and continues with the next statement.

```c
  continue;
  
  break;
```


## Program Elements

### Global Variables

Global variables can be created by writing a declaration outside of a function, struct or interface scope. Global variables can be capsuled within namespaces, and can be accessed from everywhere with the corresponding namespace path. Global Variables can have visibility modifiers. The variable can have an initial value as well, like so:

```c
  restricted int globalVar = 10;
```

The value is evaluated and set during the programs startup, in the order of the apearance of the global variables. This is true for included variables as well. The only exception to this is that functions that may be called in the expression of the initial value may not signal any exceptions.

### Function

A function is the largest building block of a program. It consists out of a function signature and a body. The signature looks like this:

```c
  [Type] [Name]<[Provisos]>([Parameters]) signals [Exceptions]
```

Note that provisos and the signals attatchment is optional. The body of the function is simply a list of statements, which are wrapped in braces. A function finally looks like this:

```c
  T getValue<T>(A<T>* a) {
    return (T) a->value;
  }
```

#### Function Provisos

Function signatures can have proviso heads, which mean they can provide provisos for the return type, parameters and code section. In the example, the function `getValue` provides the proviso `T`. Every time the function is called, the callee has to define types to act as provisos. These types are then substituted into the return type, parameters and code. For example, if we call the function with the call `getValue<int>(a)`, behind the scenes, the function now will look like this:

```c
  int getValue(A<int>* a) {
    return (int) a->value;
  }
```

### Interface Typedef

An interface typedef defines a new interface and provides an anchor for a new interface type based on this typedef. The syntax of an interface typedef is:

```c
  interface [Name]<[Proviso]> : [Interfaces] {
  
    [Function Signature];
  
    ...
  
  }
```

As we can see, an interface typedef starts with the keyword `interface`, followed by the interface name, and a list of optional provisos. The interface typedef can implement multiple interfaces as well, which will simply cause the functions of the other interfaces to be copied to this interface typedef. The body of the typedef contains multiple function signatures with semicolons at the end. The head provisos of the typedef are passed to the contained function signatures.

For example, the `Iterable<T>` interface from the included library:

```c
  interface Iterable<T> {
  
    int size<T>();
    
    T get<T>(int index);
    
    void set<T>(int index, T val);
  
  }
```

#### Instantiating an Interface

Instantiating an interface has some different rules that common instantiations. In order to create a new interface, it is required to assign a pointer to a struct to it that implements this interface. For example:

```c
  LinkedList<int>* list = LinkedList::create<int>(0);
  Collection<int> c = list;
  Collection<int>* c0 = &list;
```

In the example, a new instance of a linked list is created. The type of the list implements the interface Collection. So, by creating a pointer to the list, we can use it as a new interface instance.

#### Calling an Interface

Calling a function of the interface acts like calling a [nested Function](#struct-nesting). Lets add to the example:

```c
  Collection<int> c = list;
  
  // Call a method in the interface
  c.add(14);
  
  // Creating a pointer to the interface
  Collection<int>* c0 = &c;
  
  // Call a method in the interface and deref c0.
  return c0->get(0);
```

In the example we call a method in the interface directley, as well via a pointer to an interface. Note the use of `.` and `->` in the call syntax: Using `.` will cause the value the expression `c` evaluates to to be used directley. Using `->` will cause the value to be treated as a pointer, so after loading `c0`, this value will be derefed, and the resulting value will be used. Using `.` and `->` incorrectly can cause the program to behave unexpectedly.

### Struct Typedef

A struct typedef defines a new struct union and provides an anchor for a new struct type based on this typedef. The syntax of a struct typedef is:

```c
  struct [Name]<[Proviso]> : [Extension], [Interfaces] {
  
    [Type] [Fieldname];
    
    ...
    
    [Function Signature]
  
    ...
  
  }
```

As we can see, a struct typedef starts with the keyword `struct`, followed by the struct name, and a list of optional provisos. A struct can extend from another struct. In this case, we add `: [Extension]`. Also, the struct can implement multiple interfaces. In this case, we write the interface names seperated by commas.

Example:

```c
  struct A<T> : B<T>, Iterable<T>, Serializable {
  
    T value;
    
    void getValue<T>() {
      return self->value;
    }
  
    ...
  }
```

In the example, the struct typedef extends from the struct type `B`, and implements the interfaces `Iterable` and `Serializable`. Note that the implementation of the functions are left out to keep the example short. 

When extending and implementing in the struct typedef, provisos can be passed as well. In the example, we pass the proviso `T` to the extension `B` and to the interface `Iterable`.

### Enum Typedef

An enum typedef defines a new enum and provides an anchor for a new enum type based on this typedef. The enum typedef contains field, whiches names are unique within the enum. For example:

```c
  enum Status {
  
    NORMAL, EXCEPTIONAL;
    
  }
```


## Advanced Features

### Exceptions

Exception handling in Snips consists out of two parts: Signaling the exception, and watching it. An exception can be any struct instance. When the exception is thrown, the current program execution ends and the program jumps to the nearest watchpoint. A watchpoint is either a watch-statement, or the function itself. If it is the function itself, the program returns from the function and the cycle repeats. Lets have a look at how to signal exceptions.

#### Signaling Exceptions

Signaling an exception is as easy as using the `signal` keyword, followed by an expression that evaluates to a stuct. This struct then acts as the thrown exception.

```c
  signal e;
  signal Exc::(...);
```

#### Watching Exceptions

Watching exceptions can happen in one of two ways. Either the function itself is signaling exceptions, or the function watches the exception with a watchpoint. Lets have a look at the latter option:

```c
  ...
  
  try {
    ...
    // The exception e of type Exc is thrown somewhere here
    ...
  } watch (Exc e) {
    ...
  }
  
  ...
```

In the example, we watch over a piece of code that may signal an exception of the type `Exc`. We try to execute the piece of code. It may signal an exception, but may not. In case that it signals an exception, the code of the watchpoint is executed. Example:

```c
  int status = 0;
  try {
    signal Exc::(12);
  } watch (Exc e) {
    status = e.status;
  }
```

In the example, an exception is signaled and watched. The watchpoint retrieves the status from the exception and wires it to a status flag. Watching multiple exception types is possible as well:

```c
  int status = 0;
  try {
    maySignalException();
    signal Exc::(12);
  } watch (Exc e) {
    status = e.status;
  } watch (Exc2 e) {
    status = e.status;
  }
```

In this example we watch two exception types, `Exc` and `Exc2`. Depending on which exception type is signaled, the corresponding watchpoint executes its code. 

Functions may signal exceptions as well:

```c
  void maySignalException() signals Exc2 {
    signal Exc2::(25);
  }
```

The function signals the exception of type `Exc2`. Note that all exception types that can be signaled in the function body, must either be covered by a watchpoint or by the `signals` option at the function head.


### Direct ASM

When programming highly important and performance-ciritcal routines, it may sometimes be required to directley write asm within Snips. This can be done with the direct ASM statement:

```asm
asm(a : r0, b : r1) {
  /* Directley inject assembly */
  lsl r0, #1 :
  lsr r1, #1 :
  and r0, r0, r1
} (r0 : r);
 ```
 
Using the `asm` keyword, we can define a new assembly section. In parentheses, we can define values to be copied before starting to execute the assembly. In the example, we copy the variable `a` into the register `R0`. After execution, we copy the register `R0` into the variable `r`. Note that when writing custom assembly, you have full control over the entire assembly execution. Your assembly code will not be optimized by the optimizer. Be aware to reset the stack and any registers you use - or you may break the program execution. With great power comes great responsibility!

### Modules

A module is a translation unit that is stored in a single file. A module can contain all kinds of program elements, as well as imports. When a module is compiled, a module dump is generated. The generated file will have the same filename as the module and is a `.s` file. Each generated module dump has a version number, that is generated by hashing the contents of the source file. If the source file changed, the module changed and the existing module dump is discarded. If the version has not changed, the dump does not have to be updated and can be used to link other programs that depend on this module. If a module contains a program element with proviso types, and if a new proviso mapping of a program element is generated upon a compilation, the existing module dump is amended. 

### Directives

#### Include Directive

Using the `#include<[Path]>` directive, code from other files can be included in the current module. Imports are transitive, which means when a module is imported into a module, and this module has imports itself, these imports will be resolved into the current file as well. When multiple modules import the same module, the module is only included once. The order of the imports, so that all dependencies are in total order, is determined automatically. All include directives should be stated at the file start, but its not required. Import paths can be of one of three types:

- Absolute filepath
- Relative filepath to the main file the compiler was called with
- Alias Names, which is only available for included libraries

Example:

```c
  // Absolute import
  #include<C:\Users\...\file.sn>
  
  // Relative import
  #include<...\file.sn>
  
  // Alias Import
  #include<linked_list.sn>
```

Warning: A limitation of the system are cyclic imports. This means `A` imports `B` and the other way round. These imports cannot be resolved.

#### Define Directive

Using the `#define [Value] [Replace With]` directive, an alias for a value can be created. All instances of this value will be replaced with the second argument. This can be used to dynamically create code, and ties in with the ifdef-directive. 

Example:

```c
  #define dimension 3
  int [dimension] [dimension] matrix;
```

will become after the Pre-Processor:


```c
  int [3] [3] matrix;
```

#### If-Define Directive

Using the `#ifdef [Condition]` directive, code can be included or excluded based on the condition. A block is defined by a leading `#ifdef` and closed by a `#end` directive. These blocks may be capsuled. If a block's condition is evaluated to false, all of the block's contents, including other directives, will be removed. The condition may be be equal to `true`, `false`, `[FLAG]`, where the flag was passed from the command line. Note that using `#define`, flags can be created in the code itself, which are then substituted into the condition by the Pre-Processor.

Example:

```c
  #define includeFunction true
  ...
  #ifdef includeFunction
  int foo() {
    ...
  }
  #end
```

will become after the Pre-Processor:


```c
  int foo() {
    ...
  }
```

#### Passing Pre-Processor Flags from the Command Line

When starting the compiler, using the `-F [Args]` argument, multiple flags can be passed to the Pre-Processor. These flags can then be used within `#ifdef` conditions. The passed flags are globally visible, meaning they extend over all translation units, including transitive imports.

#### AST Optimizer Directives

Next to the Pre-Processor directives, AST directives will be attatched to the AST during the parsing stage. These directives can have arguments and are read-out during the AST-Optimizer stage. Using these directives, the AST-Optimizer can be guided to achieve improved optimization results. A directive can be attatched to a statement or function by writing it one line above the target, like so:

```c
  #inline
  int foo() {
    #unroll depth = 10
    for (int i = 0; i < 10; i++) ...
  }
```

A directive can have no arguments or multiple of them. In case of having multiple arguments, they are seperated by a comma:

```c
  #somedirective foo = abc, bar = baz
  int foo() {
    ...
  }
```

Directive arguments do not have to have a value, for example the `#strategy` directive:

```c
  #strategy always
  int foo() {
    ...
  }
```

Currently available AST-Optimizer directives are:

 |    Directive Name      |         Arguments              |                                        Description                                          |
 | ---------------------- | ------------------------------ | ------------------------------------------------------------------------------------------- |
 | unroll                 | depth &lt;n&gt;                | Attatch to a loop to indicate the Optimizer &lt;n&gt; loop-unrolls should bee made.         |
 | inline                 | -                              | Attatch to a function to indicate that this function should be inlined when possible.       |
 | unsafe                 | -                              | Attatch to function to exclude from optimizer, indicates it is performing unsafe operations.|
 | strategy               | &lt;always, on_improvement&gt; | Attatch to function to overwrite default optimization strategy for function scope.          |
 | predicate              | -                              | If set to a function, it is considered non-state changing. Allows for further optimizations. A state is considered as the values of the Stack, Global Variables and Heap. Ideally, the function should only depend on the given parameters. |
 | operator               | &lt;symbol&gt;                 | If set to a function, the function is used as operator for the specified symbol. Everytime when this operator is used with the function parameters as operands, a call to this function will be made instead. |

### Header Files

Header files are a way to define the contents of a module without including the contents itself. For example, method signatures can be declared in a header file, without including their bodies. Using header files and including header files instead of a single source file can speed up the compilation process when the `-r` or `-R` argument flag is not set. In this case, the compiler only reads in the included imports. If the imports are header files, the compiler will only create signatures and type definitions based on the contents of the header. Later, during the linking-stage, the compiler will read-in existing module translations and copy the required assembly into the main translation unit.

If a source file is directly included, the compiler will search for a header file with the same name in the same directory. If such a file exists, the file is loaded as well. Including a source file directly eliminates the possibility to use exising module translations, and thus reduces compatibility. It is recommended to use header files whenever possible to be able to make use of the feature.

Examples for ressources in header files:

```c
  #include<import.hn>

  // Function signature
  T foo<T>(T value);
  
  // Struct Typedefinition
  struct X<T> {
    T value;
    
    T get();
  }
```

The following program elements MUST be present in a header file, if they are present in a source file:

- Imports

The following program elements MUST be present in a source file, if they are present in a header file:

- Imports
- Function implementations that are defined in a header file

The following program elements MUST NOT be present in a source file, if they are present in a header file:

- Struct Field Types in a struct type-definition that was defined in the header file

For an example, have a look at the  [Linked List Implementation](https://github.com/PhilipJonasFranz/SnipsCompilerGen2/tree/develop/release/lib/std/data).

### Heap functionality

Using the heap can be crititcal to achieve more complex and functioncal programs. In short, the Heap is like an external database, that can be accessed from anyone at any point, given they have the correct address for what they are looking for.

#### Using the Heap

Allthough the heap can be used manually, it is highly suggested to use the built-in dynamic libraries for memory operations:

 |         Name           |         Signature         |                                        Description                                     |
 | ---------------------- | ------------------------- | -------------------------------------------------------------------------------------- |
 | Allocate Memory        | `void* resv(int size)`    | Allocates a new memory section on the heap with the given size in datawords            |
 | Free Memory            | `void free(void* p)`      | De-allocates the memory section corresponding to the given pointer                     |
 | Initialize in memory   | `void* init<T>(T value)`  | Allocates a new memory section, copies the given value to it and returns pointer to it |
 | Memory heap size       | `int hsize(void* p)`      | Loads the size of the memory section corresponding to the given pointer in datawords   |

Example:

```c
  // Allocate memory for number
  void* p0 = resv(sizeof(int));

  // Initialize number on heap
  int* p = init<>(14); 
  
  // Set value to 20
  *p = 20;
  
  // s = 1
  int s = hsize(p);
  
  // Free memory
  free(p);
```

### Namespaces

Namespaces allow the programmer to hierarchically structure their program. Also, they allow ressources with the same name in different namespaces.

Example:

```c
  namespace N {
    struct X {
      int value;
    }
  }
  
  int main() {
    X x0 = X::(10);
    N::X x = N::X::(12);
    return x.value + x0.value;
  }
```

In the example we declare a namespace `N` and capsule a struct in it. We can access the nested ressource either by navigating to it with the namespace path or by accessing it directly. This works as long as the ressource name is unique:

```c
  namespace N {
    struct X {
      int value;
    }
  }
  
  namespace M {
    struct X {
      bool value;
    }
  }
```

Now there are two ressources with the same name, but capsuled in different namespaces. Now we are forced to use full namespace paths:

```c
  int main() {
    N::X x0 = N::X::(10);
    M::X x1 = M::X::(true);
    return x0.value + (int) x1.value;
  }
```

Namespaces can be stacked within each other as well:

```c
  namespace N {
    namespace M {
      struct X {
        int value;
      }
    }
  }
```

### Visibility Modifiers

Visibility modifiers, or modifiers for short, are a way for the programmer to restrict the access to a function or struct typedef. Modifiers are optional, by default, all functions and struct typedefs are shared. Modifiers can even be overridden with the argument `-rov` when compiling the program. Modifiers are meant as a way to give a hint of direction when working with an external library. For example you may want to restrict the access to a struct typedef, so it is not possible to initialize an instance of this struct directley, but only by the constructor.

Modifiers are:

 |         Name           |                                                      Description                                                |
 | ---------------------- | --------------------------------------------------------------------------------------------------------------- |
 | `static`               | Most permissive, allows access from anywhere, even allows struct nested function access without struct instance |
 | `shared`               | Same as `static`, but does not allow struct nested function access without struct instance                      |
 | `restricted`           | Allows only access to the ressource if the current scope is the same or child of the resource scope             |
 | `exclusive`            | Allows only access if the current scope is the same as the scope of the ressource                               |

Modifiers can be placed in front of the following program elements:

 |     Ressource          |                Example               |
 | ---------------------- | ------------------------------------ |
 | Function               |  `shared T get<T>(int index) ...`    |
 | Interface              |  `shared interface Serializable ...` |
 | Struct                 |  `shared struct X<T> ...`            |
 | Enum                   |  `shared enum State ...`             |

### Parameter Covering

Paramter covering simplifies the process of creating a new instance of a struct that extends another struct. Lets look at an example:

```c
  struct Point2D {
    int x;
    int y;
  }
  
  struct Point3D : Point2D {
    int z;
  }
```

The struct `Point3D` extends the struct `Point2D`, and now has per definition three fields: `x, y, z`. If we want to create a new instance of this struct, we could use the expression:

```c
  Point3D p = Point3D::(5, 2, 6);
```

In this case, we manually enter every parameter in the structure initialize, even the values for fields that are inherited. Parameter covering allows us to re-write this expression to:

```c
  Point3D p = Point3D::(Point2D::(5, 2), 6);
```

Now, we use parameter covering to 'cover' the first two parameters from the original expression with the first parameter from the second expression. The instance of `Point2D` that is created provides us the values for these parameters. In this form, the example is a bit pointless, since it makes the code unnessesarily more complex, if not a bit more readable. But lets now think of an example where the two structs have a `create()` method that performs computations on the parameters before setting them. In this case, an expression like this makes a lot more sense:

```c
  Point3D p = Point3D::create(Point2D::create(5, 2), 6);
```

Because now we use the computataion logic of the both `create()` methods to create the new instance. This way we can be sure our structs are correctly initialized.

Restrictions of this feature are:

- Paramter Covering only works for the first parameter
- Works only when initiating a new struct instance
- Works only if the struct extends from a struct and the parameters are covered by an expression that returns an instance of the extended struct

### Struct Nesting

Struct nesting is a tool that allows the programmer to write code that associates functions more with the struct they work with. This brings multiple benefits:

- Implicit `self` reference
- Constructors and implicit `super()` constructor

Lets first look at an example:

```c
  struct S {
    int value;
    
    int get() {
      return self->value;
    }
    
    S* id() {
      return self;
    }
  }
  
  int main() {
    S* s = init<>(S::(12));
    return s->get();
  }
```

In the example we create a new struct type with a struct typedef, give it a value, and a function `get`, which is nested in the struct typedef. In the following `main` method, we create a new struct instance on the heap, and recieve a pointer to it. Finally, we call the function `get` in `S` via the pointer `s`. Lets have a look behind the scenes what is going on:

When nesting a struct function, it is associated with this struct. This means it can only be called via an instance of the struct or a pointer to the struct. An exception to this is the `static` modifier: A nested method annotated with static can be accessed with the name of the struct:

```c
  struct S {
    int value;
    
    static int getNum() {
      return 10;
    }
  }
  
  int main() {
    return S::getNum();
  }
```

Note that if the function is static, no implicit `self` is added, so the function call will not be associated with a specific struct instance.

When we call the function using a pointer reference, the pointer is added as an implicit first parameter to the call. On the other end, a new argument is added to the function signature of `get`: a pointer to an instance of `S` named `self`. So, behind the scenes, the example looks like this:

```c
  struct S {
    int value;
    
    int get(S* self) {
      return self->value;
    }
    
    S* id(S* self) {
      return self;
    }
  }
  
  int main() {
    S* s = init<>(S::(12));
    return get(s);
  }
```

When using a struct instance directley to access the function, an address reference is injected, the rest stays the same:

```c
  int main() {
    S s = S::(12);
    return get(&s);
  }
```

Struct nested calls can even be chained, for example:

```c
  int main() {
    S* s = init<>(S::(12));
    return s->id()->get();
  }
```

Again, behind the scenes this chain is transformed into:

```c
  int main() {
    S* s = init<>(S::(12));
    return get(id(s));
  }
```

Now that we have an understanding on how struct nesting works behind the scenes, lets have a look at the implicit `super()` constructor and constructors in general. A method is identified as a 'constructor' iff:

- The method signature is `static`
- The method name is `create`
- The method returns an instance of the struct its nested in

Example:

```c
  struct X {
    int value;
    
    static X create(int value) {
      return X::(value);
    }
  }
```

If we re-visit parameter covering, we now can take advantage of one small shortcut. Lets look at this example:

```c
  struct Point2D {
    int x;
    int y;
    
    static Point2D(int x, int y) {
      return Point2D::(x, y);
    }
  }
  
  struct Point3D : Point2D {
    int z;
    
    static Point3D(int x, int y, int z) {
      return Point3D::(Point2D::create(5, 2), 6);
    }
  }
```

In the constructor of the struct `Point3D` we call explicitly to the constructor of the extended struct, making use of paramter covering. We now can use the `super()` shortcut to implicitly reference this constructor, and we can re-write the constructor as:

```c
  static Point3D(int x, int y, int z) {
    return Point3D::(super(5, 2), 6);
  }
```

During compile time, the constructor of the extended struct is searched and replaces the `super()` construct.

## Further Reading

In this section, some more language features are discussed, which mostly are used to take shortcuts during development and make code more readable. The following features can be replaced with other language constructs, if not as efficiently.

### Function Overloading

Functions can be overloaded, or in other words, can have the same name, while having different parameter types. For Example:

```c
  int get() {
    ...
  }
  
  int get(bool b) {
    ...
  }
```

In the example, the function name `get` is overloaded by two functions. The function signatures differ, meaning either the return type or parameter types differ. It is required that the function can be clearly identified by the given name, which is the name and the parameters. Function overloading is available within struct typedefs, interface typedefs and the regular program space.

### Auto Proviso

Auto proviso is a feature that attempts to automatically map missing proviso types by mapping the types of parameters to the actual proviso types. For example:

```c
  T get<T>(T v) {
    ...
  }
  
  int main() {
    return get<>(10);
  }
```

In the example we have a function with one type parameter `T`. In the `main` function, we call the function with the argument `10`, which is obviously of the type `INT`. We leave out the required type parameters in the call syntax. Now, behind the scenes, it is attempted to create an auto-mapping: Looking through the arguments, it is clear that the parameter in the function head `v` with type `T` is mapped to the first call argument type `INT`. So, we can reverse map the missing proviso with `INT`. 

```c
  T* get<T>(T* v) {
    ...
  }
  
  int main() {
    return get<>((int*) 10);
  }
```

The mapping even works when the type parameter is capsuled within other types, like in the pointer in the example above. Note the nessesity of casting the value `10` to an `INT*` in the call. This is needed so the mapper recognizes the type `INT` in the type `INT*` compared to the target type `T*`. Not casting to the pointer will result in an error message: `Cannot auto-map proviso 'PROVISO<T>', not used by parameter`. 

Additionally, a few requirements have to be met in order that a valid auto-mapping can be determined:

- The mapping of the type parameters has to be non-ambigous, one type parameter may not share multiple mapped types
- All required parameter types have to be used by the arguments. If one type parameter cannot be mapped, the mapping fails

With the auto-proviso functionality, some interesting conclusions can be made:

- When calling a struct nested function that has no additional provisos except the struct provisos, auto proviso will always succeed, since the implicit `self` reference always carries all required provisos
- In the call syntax, `<>` can be left out

### Placeholder Atoms

Placeholder atoms are a way to generate default values of variable size. For example:

```c
  struct X {
    int [2] arr;
    int x0;
  }
  
  int main() {
    X x = X::(...);
  }
```

In the example, we use the `...` syntax as a placeholder to fill in all struct field values automatically. This special version simply skips the amount of words the covered fields are large, in this case 3. The struct initialization in assembly then boils down to subtracting the amount of words times four and pushing the SID if enabled. This is a very efficient and quick method to create a struct if no concrete values are available at the moment.

Placeholder atoms can also be used to initialize fields with a starting value:

```c
  int main() {
    int a = 2;
    X x = X::((a++)...);
  }
```

By providing a value before the placeholder, we effectiveley 'fill' the covered memory space with this value. So in this example, we fill three memory words with the value `3`. The expression is evaluated once at the start, and the value is used repeadetley. Placeholder atoms are available for declarations and structure initializations.

We can take this functionality even further, by using multi-word expressions to use to fill the memory:

```c
  struct X {
    int [5] arr;
  }
  
  int main() {
    X x = X::(({1, 2, 3})...);
  }
```

This will result in the base being repeatedley loaded. The initialized struct will look like this:

 | SID |
 | --- |
 |  1  |
 |  2  |
 |  3  |
 |  1  |
 |  2  |

 As we can see, the array expression was loaded behind the struct id, and then repeated until the bounds of the memory section from the struct were reached. This can be used to initialize structs with a pattern.
 
### Register Atoms

Register atoms are a way to directley read-out the value of a register at this moment. This can be done with the following syntax:

```c
  int val = #r3;
```

With this, we read out R3 at the current execution. The following registers can be read out:

- `r0` to `r15`
- `fp`, `sp`, `lr`, `pc`

Reading out registers requires a vast amount of delicacy and a deep understanding of the underlying mechanics of the translation of the compiler. Most generally, this feature can be used as a semi-random number generator or to read out function parameters directly.

### Predicates and Inline Functions

#### Introduction to Predicates

Predicates and inline functions are a way to pass a function as a parameter or variable to another part of the code, and execute it there. It allows for great flexebility and code re-usability. Lets have a look at this example:

```c
  bool isEqual(int x, int y) {
    return x == y;
  }
  
  int process(func pred, int x, int y) {
    return (int) pred(x, y);
  }

  int main() {
    func pred = isEqual;
    return process(pred, 10, 12);
  }
```

In this example, we create a predicate with the function address of `isEqual`. Then we pass this function to the function `process` with parameters, and in this function, call the predicate and return its return value. This is one of the most basic usages of predicates.

When we compile this code however, we get two warnings:

- `Unsafe operation, predicate 'pred' is anonymous`
- `Using implicit anonymous type INT`

Lets look at the first one. The predicate is anonymous. This means that we did not provide the compiler information about the function signature of the passed predicate. This means the compiler assumes we know what we are doing and blindly loads the parameters as we list them. This is in no way problematic, but this way does not guarantee us type-safety and error reports when something goes wrong. We can de-anonymize the predicate by adding in a signature for it:

```c
  int process(func (int, int) -> bool pred, int x, int y) {
    return (int) pred(x, y);
  }
```

With this we simply tell the compiler that our predicate will take two `INT` parameters, and will return a `BOOL` value. This way the compiler can check if we actually provide the correct parameters to the call and handle the return type correctly. Also, this way we minimize the possibility of stack shifts. Lets think about a predicate that returns an array. This array would be loaded on the stack and returned over the stack. If the predicate was anonymous, the compiler assumes a `VOID` return type into `R0`. And just like that, we have a 'ghosted' array on our stack. This may cause addressing issues, and may even crash the entire program. So - its is always best practice - to pass a function signature to the compiler. Also, we notice our second warning is gone. What is this all about?

Like discussed previously, the compiler makes the assumption about an anonymous predicate, that it returns a `VOID` type into `R0`. The implicit anonymous type comes about the casting expression that casts the call return type to an `INT`. During context checking, the compiler uses this casting to set the return type of the funtion to the casted type. So, we could cast to an `INT`-Array to prevent the issue we had earlier. This provides type safety on the output, but not on the inputted arguments. For full type safety, a predicate function signature is needed.

Also, it is important to note is that predicates may not signal exceptions.

Predicates can be compared like regular variables, returning `true` if they point to the same function.

#### Inline Functions

Usual predicates require a function defined somewhere in the code. We can prevent this by using an inline function:

```c
  int main() {
    func pred = (int x, int y -> bool) : { return x == y; };
    return process(pred, 10, 12);
  }
```

With this syntax we create an anonymous inline function. This function is casted like a regular function outside of the function where it is defined, but only access to this function is only given by the expression itself. These function have names like `ANON2`. In the syntax, we provide function parameters, return type and body.

Like normal predicates, these functions may not signal exceptions.

### Variable Shadowing

Variable names can be shadowed when a variable with the same name lies in a different scope. For example:

```c
  int x = 10;
  
  struct S {
    int x;
  }
  
  int get(S x) {
    if (true) {
      S* s = &x;
      int x = 2;
      s->x <<= 1;
    }
    return x.x;
  }
```

In the example, the name `x` is shadowed multiple times. In the top scope, a global variable is defined. Then, a function parameter is defined, and finally a variable inside the function. When using the variable name `x` in the program, the variable with the name `x` in the highest scope is used. Highest in the sense of 'most capsuled' scope. Shadowing will produce warnings, but is safe to use.

The only restriction to shadowing obviously is having two variables with the same name in the same scope.

### Don't care arrays

It is possible to create arrays and disable the type-checking of the array elements during the array's creation by using the syntax `[]` instead of `{}`:

```c
  int [5] arr = [X::(10, 12), true, 'c'];
```

The only limitation is that the combined data-word size of the array elements have to be equal to the specified array size.

### Operator Overloading

Operators can be overloaded using the `#operator [Symbol]` directive. Let's look at an example:

```c
  #operator +
  static int add(int a, int b) {
    return a + b + 5;
  }
  
  int main(int x) {
    return x + 5;
  }
```

We specified that the operator `+` should be overloaded for the operand types `INT, INT` by the function `add`. In the function main, we use this operator with the specified types. So, instead of adding the numbers, behind the scenes the AST is transformed to:

```c
  ...
  
  int main(int x) {
    return add(x, 5);
  }
```

Overloaded operators inherit the precedence and associativity from the original operator. Operators can be overloaded multiple times, the only restriction is that every function signature that overloads a single symbol has to differ from one another, so the correct function can be uniquely identified. Two functions can be differentiated if their arguments types are not equal or if they do not have the same amount of arguments.

It is also worth noting that unbound expressions are possible via operator overloading:

```c
  #include <linked_list.hn>
  
  int main(int x) {
    LinkedList<int>* list = LinkedList::create<int>(0);
    list << x;
    ...
  }
```

The `<<` operator is overloaded to add elements to the list. In this scenario, an expression can be written without having a data target. Since the expression is transformed to a function call, this is not an issue. But keep in mind that if the operator that overloads `<<` is not found, the translated code potentially has an unbound expression casted.

Note that functions that have operator directives still can be called explicitly like normal functions.

