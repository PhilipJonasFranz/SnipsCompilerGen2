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
  - [Global Variables](#global-variables)
  - [Function](#function)
  - [Interface Typedef](#interface-typedef)
  - [Struct Typedef](#struct-typedef)
  - [Enum Typedef](#enum-typedef)
- [Advanced Features](#advanced-features)
   - [Exceptions](#exceptions)
   - [Direct ASM](#direct-asm)
   - [Imports](#imports)
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
  Collection<int>* c = &list;
```

In the example, a new instance of a linked list is created. The type of the list implements the interface Collection. So, by creating a pointer to the list, we can use it as a new interface instance.

#### Calling an Interface

Calling a function of the interface acts like calling a [nested Function](#struct-nesting). Lets add to the example:

```c
  Collection<int> c = &list;
  
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

### Imports

Using the `#include<[Path]>` directive, code from other files can be included in the current file. Imports are transitive, which means when a file is imported into a file, and this file has imports itself, these imports will be resolved into the current file as well. When multiple files import the same file, the file is only included once. The order of the imports, so that all dependencies are in total order, is determined automatically. All include directives should be stated at the file start, but its not required. Import paths can be of one of three types:

- Absolute filepath
- Relative filepath to the main file the compiler was called with
- Alias Names, which is only available for included libraries

Example

```c
  // Absolute import
  #include<C:\Users\...\file.sn>
  
  // Relative import
  #include<...\file.sn>
  
  // Alias Import
  #include<linked_list.sn>
```

Warning: A limitation of the system are cyclic imports. This means `A` imports `B` and the other way round. These imports cannot be resolved.

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

### Function Overloading

### Auto Proviso

### Placeholder Atoms

### Register Atoms

### Predicates and Inline Functions
