# Elementary arithmetic

## Description
Given an iterator over a sequence of characters that represent a mathematical expression, your task is to write Java code that evaluates the expression. The following must be noted:

  1. An expression will only contain integers, and the following non-integer characters: ‘(‘, ‘)’, ‘+’, ‘-‘, ‘*’, ‘/’.
  2. Parentheses enclose work units that must be evaluated in separate thread(s).
  3. Expressions are of arbitrary length.
  4. Expressions can have arbitrary levels of nesting.

## Example:

* Input: An iterator over => (1 + 2) / 5 + (7 * 2 - 5)
* It is expected that (1 + 2), (7 * 2 - 5) are evaluated by different threads.
* Output: 9.6

## Deliverables:

  1. A high level specification describing your ideas on how to solve the problem. Flowcharts, algorithms, pseudo-code, sequence diagrams, etc. are all acceptable.
  2. Source code that exposes and implements the following method: public double evaluate(Iterator<Character> expressionItr);
  3. Junit4 unit tests that cover the functionality appropriately.

## Bonus:
Suppose that the operators (addition, subtraction, multiplication, division) are divided into two groups:

* Group 1: +, - [addition and subtraction]
* Group 2: *, / [multiplication and division]

Within each group, only one operation can be performed at any given instant; operations across groups can occur simultaneously. So, if we had:
* Input: An iterator over => (5 + 2) * (3 – 1) + (10 / 2) The program should ensure that 5+2 and 3-1 are NOT evaluated concurrently.
