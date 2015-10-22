import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

public class Main {
  public static void main(String[] args) throws Exception {
    String expression = "(1+2*(3+4))";
    char[] chars = expression.toCharArray();
    Character[] characters = new Character[chars.length];
    int count = 0;
    for (char c : chars) {
      characters[count++] = c;
    }

    System.out.println(new Main().evaluate(Arrays.asList(characters).iterator()));
  }

  final Lock addLock = new ReentrantLock();
  final Lock divideLock = new ReentrantLock();

  public double evaluate(Iterator<Character> expression) throws Exception {
    assert expression != null;

    // construct huffman tree
    Node root = generateHuffmanTree(expression);

    // traverse the tree and calculate
    return calculate(root).get();
  }

  Node generateHuffmanTree(Iterator<Character> expression) {
    Node currentParent = null;

    if (!expression.hasNext()) {
      throw new RuntimeException("No expression!");
    }

    // handle special first number with sign such as -5, +3
    char nextChar = expression.next();
    if (nextChar == '+' || nextChar == '-') {
      StringBuilder firstNumber = new StringBuilder();
      firstNumber.append(nextChar);
      while (expression.hasNext() && Character.isDigit(nextChar = expression.next())) {
        firstNumber.append(nextChar);
      }

      Node firstLeftNode = new NumberNode(Double.parseDouble(firstNumber.toString()));
      if (!expression.hasNext() || nextChar == ')') {
        return firstLeftNode;
      }

      OperatorNode operatorNode = generateOpNode(nextChar);
      operatorNode.left = firstLeftNode;
      firstLeftNode.parent = operatorNode;
      currentParent = operatorNode;
      if (!expression.hasNext()) {
        throw new RuntimeException("Lack an expression after " + nextChar);
      }
      nextChar = expression.next();
    }

    do {
      OperatorNode temp = (OperatorNode) currentParent;
      OperatorNode opNode;
      Node node;
      if (nextChar == '(') {
        node = generateHuffmanTree(expression);
        if (!expression.hasNext() || (nextChar = expression.next()) == ')') {
          if (temp == null) {
            currentParent = node;
          } else {
            temp.right = node;
            node.parent = temp;
          }
          continue;
        }

      } else if (Character.isDigit(nextChar)) {
        StringBuilder number = new StringBuilder();
        number.append(nextChar);
        while (expression.hasNext() && Character.isDigit(nextChar = expression.next())) {
          number.append(nextChar);
        }

        node = new NumberNode(Double.parseDouble(number.toString()));
        if (!expression.hasNext() || nextChar == ')') {
          if (temp == null) {
            currentParent = node;
          } else {
            temp.right = node;
            node.parent = temp;
          }
          continue;
        }

      } else {
        throw new RuntimeException("Unexpect symbol: " + nextChar);
      }

      opNode = generateOpNode(nextChar);

      if (temp == null) {
        currentParent = opNode;
        opNode.left = node;
        node.parent = opNode;
        nextChar = expression.next();
        continue;
      }

      if (Operator.compare(nextChar, temp.operator) < 0) {
        temp.right = node;
        node.parent = temp;
        while (temp.parent != null) {
          temp = (OperatorNode) temp.parent;
          if (Operator.compare(nextChar, temp.operator) >= 0) {
            break;
          }
        }

        if (temp.parent == null) {
          opNode.left = temp;
          temp.parent = opNode;
        } else {
          Node child;
          if (temp.right instanceof OperatorNode) {
            child = (OperatorNode) temp.right;
            opNode.left = child;
            child.parent = opNode;

            temp.right = opNode;
            opNode.parent = temp;
          } else if (temp.left instanceof OperatorNode) {
            child = (OperatorNode) temp.left;
            opNode.left = child;
            child.parent = opNode;

            temp.left = opNode;
            opNode.parent = temp;
          } else {
            // normally, it will never reach here.
            throw new RuntimeException("internal error");
          }
        }

//        opNode.left = temp;
//        if (temp.parent != null) {
//          OperatorNode tempParent = (OperatorNode) temp.parent;
//          if (tempParent.left == temp) {
//            tempParent.left = opNode;
//          } else {
//            tempParent.right = opNode;
//          }
//          opNode.parent = tempParent;
//        }
//        temp.parent = opNode;

      } else {
        opNode.parent = temp;
        temp.right = opNode;
        opNode.left = node;
        node.parent = opNode;

      }
      currentParent = opNode;

      if (!expression.hasNext()) {
        throw new RuntimeException("Lack an expression after " + nextChar);
      }
      nextChar = expression.next();
    } while (expression.hasNext() && nextChar != ')');

    while (currentParent.parent != null) {
      currentParent = currentParent.parent;
    }

    return currentParent;
  }

  private OperatorNode generateOpNode(char operator) {
    OperatorNode operatorNode = null;
    switch (operator) {
      case '+':
      case '-':
      case '*':
      case '/':
        operatorNode = new OperatorNode(operator);
        break;
      default:
        throw new RuntimeException("Unsupported operator: " + operator);
    }

    return operatorNode;
  }

  CompletableFuture<Double> calculate(Node root) {
    if (root instanceof NumberNode) {
      NumberNode numberNode = (NumberNode) root;
      return CompletableFuture.completedFuture(numberNode.number);
    } else if (root instanceof OperatorNode) {
      OperatorNode operatorNode = (OperatorNode) root;
      final char operator = operatorNode.operator;
      CompletableFuture<Double> leftFuture = calculate(operatorNode.left);
      CompletableFuture<Double> rightFuture = calculate(operatorNode.right);

      return leftFuture.thenCombineAsync(rightFuture, new BiFunction<Double, Double, Double>() {
        @Override
        public Double apply(Double l, Double r) {
          switch (operator) {
            case '+':
              addLock.lock();
              try {
                return l + r;
              } finally {
                addLock.unlock();
              }
            case '-':
              addLock.lock();
              try {
                return l - r;
              } finally {
                addLock.unlock();
              }
            case '*':
              divideLock.lock();
              try {
                return l * r;
              } finally {
                divideLock.unlock();
              }
            case '/':
              divideLock.lock();
              try {
                return l / r;
              } finally {
                divideLock.unlock();
              }
            default:
              throw new RuntimeException("Unsupported operator: " + operator);
          }
        }
      });
    } else {
      // todo
      return CompletableFuture.completedFuture(0.0);
    }
  }

  static class Node {
    Node parent;
  }

  static class OperatorNode extends Node {
    Node left;
    Node right;
    char operator;

    public OperatorNode(char operator) {
      this.operator = operator;
    }
  }

  static class NumberNode extends Node {
    double number;

    public NumberNode(double number) {
      this.number = number;
    }
  }

  static enum Operator {
    ADD('+', 1),
    MINUS('-', 1),
    MULTIPLY('*', 2),
    DIVIDE('/', 2);

    private char sign;
    private int priority;

    private Operator(char sign, int priority) {
      this.sign = sign;
      this.priority = priority;
    }

    public static Operator fromChar(char sign) {
      switch (sign) {
        case '+':
          return ADD;
        case '-':
          return MINUS;
        case '*':
          return MULTIPLY;
        case '/':
          return DIVIDE;
        default:
          throw new RuntimeException("Unsupported sign: " + sign);
      }
    }

    public static int compare(char sign1, char sign2) {
      Operator op1 = fromChar(sign1);
      Operator op2 = fromChar(sign2);

      return op1.priority - op2.priority;
    }
  }
}