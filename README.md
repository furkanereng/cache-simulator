# Cache Simulator
This is a simple program that simulates caches for the given RAM and trace files. This is the third project assignment for Systems Programming course.

- There are 3 caches: `L1I` which is for instruction operations, `L1D` which is for data operations and `L2` which is for both data and instruction operations and also bigger than L1 caches.
- There are 4 different operations: Instruction Load, Data Load, Data Store & Data Modify.
- It is assumed that a single memory access never crosses block boundaries and it will be always aligned.
- The evidences are handled with FIFO policy.
- Write-through and no-write allocate mechanisms are used.

# How to Run?
- This program requires a file named as `RAM.dat` which represents content of the initial RAM. Each bytes are separated by a blank and it is in hexadecimal format.
- This program requires a `.trace` file that includes the operations each in a separate line. The format of each operation must be as below:
```
Instruction Load :   I <Operation Address: Hexadecimal>, <Size: Positive Integer>
Data Load        :   L <Operation Address: Hexadecimal>, <Size: Positive Integer>
Data Store       :   S <Operation Address: Hexadecimal>, <Size: Positive Integer>, <Data: Hexadecimal>
Data Modify      :   M <Operation Address: Hexadecimal>, <Size: Positive Integer>, <Data: Hexadecimal>
```
- This program can be runned after it is compiled, from command-line with the following format:
```
java CacheSimulator -L1s <L1s> -L1E <L1E> -L1b <L1b> -L2s <L2s> -L2E <L2E> -L2b <L2b> -t <tracefile>
```
where 

- `L1s, L2s` are the number of set bits of L1 and L2, respectively.
- `L1E, L2E` are the number of lines inside a set of L1 and L2, respectively.
- `L1b, L2b` are the number of block bits of a line of L1 and L2, respectively.
- `tracefile` is the name of the trace file.

**Example run:** `java CacheSimulator -L1s 0 -L1E 2 -L1b 3 -L2s 1 -L2E 2 -L2b 3 -t test.trace`

# Outputs
- This program displays number of hits, misses and evictions for each of caches.
- This program records the accessed/updated caches or RAM after each operation, in the `output.txt` file.
- This program records the final contents of each caches in separate text files.
