import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
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

    // handle special first number such as -5, +3
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

      if ((nextChar == '+' || nextChar == '-')
          && (temp.operator == '*' || temp.operator == '/')) {
        temp.right = node;
        node.parent = temp;
        while (temp.parent != null) {
          temp = (OperatorNode) temp.parent;
          if (temp.operator == '+' || temp.operator == '-') {
            break;
          }
        }

        opNode.left = temp;
        if (temp.parent != null) {
          OperatorNode tempParent = (OperatorNode) temp.parent;
          if (tempParent.left == temp) {
            tempParent.left = opNode;
          } else {
            tempParent.right = opNode;
          }
          opNode.parent = tempParent;
        }
        temp.parent = opNode;

      } else {
        opNode.parent = temp;
        temp.right = opNode;
        opNode.left = node;
        node.parent = opNode;

      }
      currentParent = opNode;
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

      return leftFuture.thenCombine(rightFuture, new BiFunction<Double, Double, Double>() {
        @Override
        public Double apply(Double l, Double r) {
          switch (operator) {
            case '+':
              return l + r;
            case '-':
              return l - r;
            case '*':
              return l * r;
            case '/':
              return l / r;
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
}