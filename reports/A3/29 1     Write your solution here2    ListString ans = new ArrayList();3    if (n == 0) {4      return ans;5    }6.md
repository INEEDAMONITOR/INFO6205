```java
    // Write your solution here
    List<String> ans = new ArrayList();
    if (n == 0) {
      return ans;
    }
    
    validParentheses(n, 0, 0, new StringBuilder(""), ans);
    return ans;
  }

  public void validParentheses(int pairs, int leftAddedSoFar, 
  int rightAddedSoFar, StringBuilder solutionPrefix,List<String> ans) {
    if (leftAddedSoFar == pairs && rightAddedSoFar == pairs) {
      ans.add(solutionPrefix.toString());
      return;
    }

    if (leftAddedSoFar < pairs) {
      solutionPrefix.append("(");
      validParentheses(pairs, leftAddedSoFar+1, rightAddedSoFar, solutionPrefix, ans);
      solutionPrefix.deleteCharAt(solutionPrefix.length() - 1);
    }

    if (rightAddedSoFar < leftAddedSoFar && rightAddedSoFar < pairs) {
      solutionPrefix.append(")");
      validParentheses(pairs, leftAddedSoFar, rightAddedSoFar+1, solutionPrefix, ans);
      solutionPrefix.deleteCharAt(solutionPrefix.length() - 1);
    }
  }
```

