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

Returns from the current function with or without a return value.

```c
  return;
  
  return a + 4;
```
