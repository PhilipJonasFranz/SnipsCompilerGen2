# The Snips Language

 This document aims to give a brief introduction to the language and its basic mechanisms. If you want a more in-depth guide, see the [Official Documentation](https://github.com/PhilipJonasFranz/SnipsCompilerGen2/blob/develop/doc/Snips%20Documentation.pdf).

## Type System

The type-system is a big part of the foundation of the language. While being strict in some areas, the type system still leaves space for more free usage of types.

### Built-in types

Types are seperated in three classes: Primitives, Composit and Special Types. Primitive types are:

 |         Type       |                  Description                  |
 | ------------------ | --------------------------------------------- |
 | INT (Integer)      | A whole number ranging from `2^-32 to 2^31`   |
 | BOOL (Boolean)     | A logic type with two states, `true or false` |
 | CHAR (Character)   | A single character encoded in UTF-8           |
 | FUNC (Function)    | Holds the address of a function, is callable  |
 | ENUM (Enumeration) | Holds a value from an enumeration             |
 | VOID (Void)        | Universal type that matches any other type    |
 | NULL (Null)        | Type that holds reference to null-pointer     |
 
 Composit types are built up from other types and are combined into a new type. Composit types are:
 
 |         Type       |                         Description                         |
 | ------------------ | ----------------------------------------------------------- |
 | STRUCT             | A collection of types and functions                         |
 | ARRAY              | A collection of equal types                                 |
 | INTERFACE          | Provides abstraction by defining functions to be implemented|
 | POINTER            | Holds address reference to other target type                |

Special types are managed during compile time, and are fixed during runtime. Special types are:

 |         Type       |                         Description                         |
 | ------------------ | ----------------------------------------------------------- |
 | PROVISO            | A shapeshifter type, used for templating and re-usability   |

## Expressions

Expressions are inductiveley defined. From a wide range of operators, expressions can be built up into a complex term. During compilation, operator precedence is taken into account. In the table below, the operators are listed in the correct precedence, starting with the highest precedence.

 |         Operator       |       Code Example        |                               Description                          |
 | ---------------------- | ------------------------- | ------------------------------------------------------------------ |
 | Atom, Enum Selection   | `a`, `10`, `State.Ok`     | Atoms can be a variable reference or direct value.                 |
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
 | Bitwise and, xor, or   | `a & b`, `a ^ b`, `a | b` | The bitwise and, xor and or operation of two operands              |
 | Logical and, or        | `a && b`, `a || b`        | The logical and, xor and or operation of two operands              |
 | Ternary                | `(c)? a : b`              | Selects one of two operands based on condition                     |
 | Array Initialization   | `{1, 2, a, b + 1}`        | Creates a new array from the given values                          |
 | Struct Initialization  | `Struct::(1, c, a + 3)`   | Creates a new struct instance from the given values                |
 
