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

 |         Operator       |      Code Example      |                               Description                          |
 | ---------------------- | ---------------------- | ------------------------------------------------------------------ |
 | Atom, Enum Selection   | `a`, `10`, `State.Ok`  | Atoms can be a variable reference or direct value.                 |
 | Array Selection        | `a [0]`, `m [0] [1]`   | Select from multi-dimensional array. Results in value or array     |
 | Structure Selection    | `st.v`, `st->v`        | Select a field from a struct                                       |
 | Increment, Decrement   | `i++`, `i--`           | Get the value from the variable and increment/decrement the origin |
 | Unary Minus            | `-val`                 | Creates the numeric negation of the base value                     |
 | Negation               | `!b`, `~i`             | Creates the boolean or bitwise negation of the base value          |
 
