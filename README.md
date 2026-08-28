# polyN-K

polyN-K is the latest language from the N series of programming languages ([polyN](https://github.com/kerlix123/polyN), [miniN-2.0](https://github.com/kerlix123/miniN-2.0) and [miniN](https://github.com/kerlix123/miniN)).

polyN-k is a multi-target transpiled programming language (Python, Kotlin (work in progress), C++ (work in progress), etc.).

# Syntax

Every statement in polyN-K starts with a line name followed by a function and its parameters and ends with a semicolon.

    line_name function;

## Line naming

In polyN-K every line of code can have its own name which is later used to implement it into a scope of a block statement like loops, conditionals and so on.

Every line needs to have its specific name. If there are more lines with the same name, the name goes to the one last declared.

If you want the function to be included into the main scope of the program, its name should be '-'. There can be more lines with this name and all of them are included into the main scope following the order they are written in.

## Comments

Comments are line which start and end with ?.

    ? this is a comment in polyN-K ?

## Data types

In polyN-K there are four main scalar types:

| Data type | Description                       | Example                        |
|-----------|-----------------------------------|--------------------------------|
| int       | Positive or negative whole number | -100, 0, 15, 1000, ...         |
| float     | Positive or negative real number  | -1.5, 4.5, 1000.05, ...        |
| string    | Sequence of characters            | "these", "are", "strings", ... |
| bool      | Logical value                     | true, false                    |

## Variables

Variables are containers for storing data values.

Variable syntax:

    - var variableName = value;

Variable examples:

    - var a = 1;
    - var b = -1.4;
    - var c = "cat";
    - var d = false;

### Lists

Lists are used to store multiple items in a single variable.

List syntax:

    - var listName = list[dataType](element);

List examples:

    - var a = list[int](1, 2, 3);
    - var b = list[float](-1.2, 3.2, 4.5);
    - var c = list[string]("meow", "roar", "moo");
    - var d = list[bool](true, false, true);

We can also have lists of lists and other complex data types:

    - var listOfLists = list[list[int]](list[int][1, 2], list[int](3, 4));

#### List items

List items are ordered, changeable and allow duplicate values.

List items are indexed starting at 0 and we can access them using square brackets:

    - var a = list[int](1, 2, 3);
    - var firstElement = a[0];
    - var secondElement = a[1];
    - var thirdElement = a[2];

#### List length

To get the length of a list we use its **.size** property:

    - var a = list[int](1, 2, 3);
    - var listLength = a.size;

#### List methods

| Method                 | Description                                                      | Example          |
|------------------------|------------------------------------------------------------------|------------------|
| .add(element)          | Adds an element at the end of the list                           | - a.add(1);      |
| .addAt(index, element) | Adds an element at the specified index                           | - a.addAt(1, 2); |
| .addAll(list)          | Adds all the elements of the list to the end of the current list | - a.addAll(b);   |
| .remove(element)       | Removes the first element with the specified value               | - a.remove(3);   |
| .removeAt(index)       | Removes the element at the specified index                       | - a.removeAt(0); |
| .clear()               | Removes all the elements from the list                           | - a.clear();     |
| .count(element)        | Returns the number of elements with the specified value          | - a.count(2);    |

### Sets

Sets are used to store multiple items in a single variable.

Set syntax:

    - var setName = set[setType](elements);

#### Set items

Set items are unordered, unchangeable, and do not allow duplicate values.

#### Set length

To get the length of a set we use its **.size** property:

    - var a = set[int](1, 2, 3);
    - var setLength = a.size;

#### Set methods

| Method           | Description                                | Example        |
|------------------|--------------------------------------------|----------------|
| .add(element)    | adds an element to the set                 | - a.add(1);    |
| .remove(element) | removes the specified element from the set | - a.remove(2); |
| .clear()         | removes all the element from the set       | - a.clear();   |


### Maps

Maps are used to store data values in key-value pairs.

Maps are changeable and don't allow duplicates.

Map syntax:

    - var mapName = map[keyType, valueType](key1 to value1, ..., keyN to valueN);

Keyword to connects a key-value pair.

Map examples:
    
    - var a = map[string, int]("one" to 1, "two" to 2);
    - var b = map[int, bool](1 to true, 2 to false);
    - var c = map[string, list[int]]("list1" to list[int](1, 2, 3));

#### Map length

To get the length of a map we use its **.size** property:

    - var a = map[string, int]("one" to 1, "two" to 2);
    - var mapLength = a.size;

#### Map keys

To get all the keys of a map in a list we us its **.keys** property:
    
    - var a = map[string, int]("one" to 1, "two" to 2);
    - var mapKeys = a.keys;

#### Map values

To get all the values of s map in a list we us its **.values** property:

    - var a = map[string, int]("one" to 1, "two" to 2);
    - var mapValues = a.values;

#### Map methods

| Method             | Description                                       | Example              |
|--------------------|---------------------------------------------------|----------------------|
| .clear()           | removes all the elements from the map             | - a.clear();         |
| .get(key, default) | returns the value of the specified key or default | - a.get("three", 3); |
| .remove(key)       | removes the element with specified key            | - a.remove("two");   |

## Input

Input syntax:

    - input dataType variableName;

The variable doesn't have to be declared beforehand.

## Output

For output in polyN-K we can use **print** and **println** functions. **println** adds a new line at the end of the output and **print** doesn't.

Output text:

    - println("This is polyN-K.");

Output expression:

    - println(a);
    - println(a + 4 * 3);

Output expression inside text (using ${expression}):

    - println("a: ${a}");
    - println("expression: ${a + 4 * 3}");

## Executing lines

In polyN-K every line can have its own name. If it has a name it is not executed immediately but waits on being called from exe function or one of the block statements.

### exe function

Using **exe** function we can run selected lines using easy regular expressions:

| Regular expression | Description                                  |
|--------------------|----------------------------------------------|
| A                  | Line A                                       |
| A.                 | All lines whose name starts with A           |
| .A                 | All lines whose name ends with A             |
| A & B. & ...       | Lines A, all lines starting with B and so on |

**exe** syntax:
    
    - exe (linesToExecute);

**exe** example:

    A1 print("This is ");
    A2 println("exe function");
    - exe(A.);

## Branching

For branching in polyN-K we use **if**, **elif** and **else** statements.

if, elif and else syntax:

    - if (condition1) [linesToExecute];
    - elif (condition2) [linesToExecute];
    ...
    - else [linesToExecute];

## While loop

While loop runs the set of statements while the condition is true.

while loop syntax:

    - while (condition) [linesToExecute];

## For loop

In polyN-K we have two types of for loop: range loop and element loop.

### Range loop

Range loop syntax:

    - for (variableName from start to end) [linesToExecute];

Variable doesn't have to be declared beforehand and **end** is inclusive.
    
If we want the loop to go backwards we use **downTo** instead of **to**.

### Element loop

Element loop is used to loop through elements of a sequence.

Element loop syntax:

    - for (element in sequence) [linesToExecute];

## Functions

Function syntax:

    - fun functionName(dataType parameterName, ...): returnDataType [linesToExecute];

Return with return:
    
    - return expression;

## Writing in target language

Using native keyword we can write code in our target language:

    - native[targetLanguageSpecifier]{targetLanguageCode};

| Target language | Specifier |
|-----------------|-----------|
| Python          | python    |
| Kotlin          | kotlin    |
| C++             | cpp       |

In the translation only the statements with current target language will be included/

If we want to write a native statement with a line name, we can write it for every target language and only the one with the corresponding target language will be saved and used in the program translation.

**native statements in polyN-K can be dangerous and should only be used if necessary!** Here are a few examples:

#### Example 1.

    - for (i from 1 to 5) [A];
    A native[python]{print(i)};

In this example we have native statement only for Python, that is okay if we only want to transpile to Python. if we want our code to work for all targets, we need to add code for line A in every target language.

#### Example 2.

    - native[python]{for i in range(5):};
    - println(i);

Transpiler doesn't analyze native code and will not create a scope for native functions so the code would look like this transpiled to Python:

```python
for i in range(5):
print(i)
```
and this is not valid in Python as we are missing a indent.

#### Example 3.

A similar thing happens in C++ and other languages using {} for scope.

    - native[cpp]{for (int i = 0; i < 5; i++) { };
    - println(i);

Transpiled code would look like this:

```cpp
for (int i = 0; i < 5; i++) {
    std::cout << i << '\n';
```

It is missing a closing curly bracket which we need to add using native statement:

    - native[cpp]{} };