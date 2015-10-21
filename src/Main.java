import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

public class Main {
  public static void main(String[] args) throws Exception {
    new Main().evaluate();
  }

  public double evaluate(Iterable<Character> expression) throws Exception {
    assert expression != null;

    // construct huffman tree
    Node root = generateHuffmanTree(expression.iterator());

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
    Node firstLeftNode = null;
    if (nextChar == '+' || nextChar == '-') {
      StringBuilder firstNumber = new StringBuilder();
      firstNumber.append(nextChar);
      while (expression.hasNext() && Character.isDigit(nextChar = expression.next())) {
        firstNumber.append(nextChar);
      }

      firstLeftNode = new NumberNode(Double.parseDouble(firstNumber.toString()));
      if (!expression.hasNext()) {
        return firstLeftNode;
      }

    } else if (nextChar == '(') {
      firstLeftNode = generateHuffmanTree(expression);
      if (!expression.hasNext()) {
        return firstLeftNode;
      }

      nextChar = expression.next();
    }

    if (firstLeftNode != null) {
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
      Node rightNode;
      if (nextChar == '(') {
        rightNode = generateHuffmanTree(expression);
        if (!expression.hasNext()) {
          temp.right = rightNode;
          rightNode.parent = temp;
          continue;
        }
        nextChar = expression.next();
        opNode = generateOpNode(nextChar);
      } else if (Character.isDigit(nextChar)) {
        StringBuilder number = new StringBuilder();
        number.append(nextChar);
        while (expression.hasNext() && Character.isDigit(nextChar = expression.next())) {
          number.append(nextChar);
        }

        rightNode = new NumberNode(Double.parseDouble(number.toString()));
        if (!expression.hasNext()) {
          if (currentParent == null) {
            currentParent = rightNode;
          } else {
            temp.right = rightNode;
            rightNode.parent = temp;
          }
          continue;
        }

        nextChar = expression.next();
        opNode = generateOpNode(nextChar);

        if (currentParent == null) {
          currentParent = opNode;
          opNode.left = rightNode;
          rightNode.parent = opNode;
          continue;
        }


      } else {
        throw new RuntimeException("Unexpect symbol: " + nextChar);
      }

      if ((nextChar == '+' || nextChar == '-')
          && (temp.operator == '*' || temp.operator == '/')) {
        temp.right = rightNode;
        rightNode.parent = temp;
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
        opNode.left = rightNode;
        rightNode.parent = opNode;

      }
      currentParent = opNode;

    } while (expression.hasNext() && (nextChar = expression.next()) != ')');

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