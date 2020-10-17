# Introduction to the Snips language

 This document aims to give a brief introduction to the language and its basic mechanisms. If you want a more in-depth guide and information about included libraries, see the [Official Documentation](https://github.com/PhilipJonasFranz/SnipsCompilerGen2/blob/develop/doc/Snips%20Documentation.pdf).

## Contents

- [Type System](#type-system)
- [Expressions](#expressions)
  - [Operators](#operators)
  - [Examples](#examples)
- [Statements](#statements)
  - [Data Statements](#data-statements)
  - [Flow Statements](#flow-statements)
- [Program Elements](#program-elements)
  - [Function](#function)
  - [Interface Typedef](#interface-typedef)
  - [Struct Typedef](#struct-typedef)
  - [Enum Typedef](#enum-typedef)
- [Advanced Features](#advanced-features)
   - [Exceptions](#exceptions)

## Type System

The type-system is a big part of the foundation of the language. While being strict in some areas, the type system still leaves space for more free usage of types.

### Built-in types

Types are seperated in three classes: Primitives, Composit and Special Types. Primitive types are:

 |         Type         |                  Description                  |
 | -------------------- | --------------------------------------------- |
 | `INT` (Integer)      | A whole number ranging from `2^-32 to 2^31`   |
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

 |          Type        |                         Description                         |
 | -------------------- | ----------------------------------------------------------- |
 | `PROVISO`            | A shapeshifter type, used for templating and re-usability   |


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
 | Logical and, or        | `a && b`, `a \|\| b`        | The logical and, xor and or operation of two operands            |
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
 | `&&=`, `\|\|=`, `^=` | Bitwise and, or, xor with new value and current value                  |
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

The for-each loop can, like the for loop, iterate over a code section, but has automatic iterator wiring. The loop can iterate over an array with fixed length, or over a pointer. In this case, the loop requires a second range parameter, that defines the length of the array. In each iteration, the current value is loaded into the iterator. The iterator value is not written back to the source, so modifications to the iterator do not change the data source.

```c
  for (int i : array) ...
  
  for (int i : p, 10) ...
  
  for (int i : s->arr) ...
```

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
  interface [Name]<[Proviso]> {
  
    [Function Signature];
  
    ...
  
  }
```

As we can see, an interface typedef starts with the keyword `interface`, followed by the interface name, and a list of optional provisos. The body of the typedef contains multiple function signatures with semicolons at the end. The head provisos of the typedef are passed to the contained function signatures.

For example, the `Iterable<T>` interface from the included library:

```c
  interface Iterable<T> {
  
    int size<T>();
    
    T get<T>(int index);
    
    void set<T>(int index, T val);
  
  }
```

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

An enum typedef defines a new enum and provides an anchor for a new enum type based on this typedef. The syntax of an enum typedef is:

```c
  enum [Name] {
  
    [FIELDS]
    
  }
```

The enum typedef contains field, whiches names are unique within the enum. For example:

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
