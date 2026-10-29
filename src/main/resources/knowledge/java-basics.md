# Java fundamentals and collections

Java is statically typed. A class combines state and behavior. An interface describes a contract; implementations supply behavior. Prefer composition when behavior can be delegated to another object. Records provide concise immutable data carriers, with generated accessors, equality, hash code, and string representation. A record's referenced objects can still be mutable.

Use List for an ordered sequence, Set for uniqueness, and Map for key-value lookup. ArrayList offers fast indexed access. HashMap uses hashCode and equals; equal keys must have equal hash codes. Use generics to catch type mismatches at compile time. Streams transform collections with operations such as filter, map, and collect. Avoid mutating shared state inside stream operations.

Practice: create a Student record, store students in a List, filter by score, and build a Map keyed by student ID. Test empty inputs and duplicate IDs. Use JUnit assertions to check behavior and expected exceptions.
