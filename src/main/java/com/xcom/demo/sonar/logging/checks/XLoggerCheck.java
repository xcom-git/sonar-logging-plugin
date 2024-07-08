package com.xcom.demo.sonar.logging.checks;



import com.xcom.demo.sonar.logging.utils.PrinterVisitor;
import com.xcom.demo.sonar.logging.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.check.Rule;
import org.sonar.java.ast.parser.ArgumentListTreeImpl;
import org.sonar.java.model.*;
import org.sonar.java.model.JavaTree.*;
import org.sonar.java.model.declaration.*;
import org.sonar.java.model.expression.*;
import org.sonar.java.model.statement.*;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plugin for check logging standard.
 * check necessary logger package, and sentive words in log content
 * TODO: trace id in log content
 *
 * 1. settings of packages of logger as 'packageMust'
 * 2. settings of illegal packages as 'packageIllegal'
 * 3. settings of sensitive keyword
 */
@Slf4j
@Rule(key = "XLoggerCheck")
public class XLoggerCheck extends BaseTreeVisitor implements JavaFileScanner {
  private static final Logger LOGGER = Loggers.get(XLoggerCheck.class);
  private JavaFileScannerContext context;

  // package necessary must be imported
  private Set<String> packageMust = new HashSet<String>(Arrays.asList(
    "com.google.cloud.logging.Logging",
    "com.google.devtools.cloudtrace.v1.Trace"
  ));
  // package illegal must NOT be imported
  private List<String> packageIllegal = new ArrayList<String>(Arrays.asList(
    "org.apache.commons.logging.Log",
    "com.google.api.gax.core.CredentialsProvider"
  ));
  // sensitive keyword
  private Set<String> secrecKeywordsSet = new HashSet<String>(Arrays.asList(
    "Credential",
    "Credentials",
    "secret",
    "secrets",
    "pwd",
    "password",
    "pass",
    "token",
    "credit",
    "mobile",
    "phone"
  ));
  // TODO: trace-id keyword

  // package imported and necessary
  private Set<String> packageChecked = new HashSet<String>();
  // simple type to full type
  private ConcurrentHashMap<String, String> types2Fullname = new ConcurrentHashMap<String, String>();
  // all variables
  private ConcurrentHashMap<String, String> variables2Fulltype = new ConcurrentHashMap<String, String>();
  // logger variables
  private ConcurrentHashMap<String, String> variablesLogger = new ConcurrentHashMap<String, String>();


  @Override
  public void scanFile(JavaFileScannerContext context) {
    this.context = context;
    scan(context.getTree());

    PrinterVisitor.print(context.getTree(), LOGGER::debug);

    log.info("");
  }

  @Override
  public void visitCompilationUnit(CompilationUnitTree tree) {
    PackageDeclarationTree packageDeclaration = tree.packageDeclaration();
    if (packageDeclaration != null) {
      printPackageName(packageDeclaration.packageName());
    }

    super.visitCompilationUnit(tree);
    // Check if all necessary packages are imported
    if (packageChecked.size() != packageMust.size()) {
      context.reportIssue(this, tree, "Not import necessary packages");
    }
  }

  @Override
  public void visitClass(ClassTree tree) {
    boolean implementsSpecificInterface = false;
    for (TypeTree typeTree : tree.superInterfaces()) {
      LOGGER.debug("implements Interface: {}", typeTree);
      if ("MySecurityInterface".equals(typeTree.toString())) {
        implementsSpecificInterface = true;
      }
    }

    super.visitClass(tree);
    log.info("");
  }

  /**
   * visit and analyze each import statement
   * @param tree
   */
  @Override
  public void visitImport(ImportTree tree) {
    StringBuffer sb = new StringBuffer();
    ConcurrentHashMap<String, Object> features = new ConcurrentHashMap<String, Object>();
    printExpress(tree, sb, features);
    // remove word 'import'
    String line = sb.substring(6, sb.length() - 1);

    // package imported and necessary
    if (packageMust.contains(line)) {
      packageChecked.add(line);
    }
    // package imported and illegal
    if (packageIllegal.contains(line)) {
      // ISSUE
      context.reportIssue(this, tree, "Illegal pakcage");
    }
    // simple type with full type
    int posBeforeName = line.lastIndexOf(".");
    String name = line.substring(posBeforeName+1);
    if (StringUtils.isNotEmpty(name)) types2Fullname.putIfAbsent(name, line);

    log.info("==== IMPORT {}", line);
    super.visitImport(tree);
  }

  /**
   * visit and analyze expression statement
   * @param tree
   */
  @Override
  public void visitExpressionStatement(ExpressionStatementTree tree) {
    StringBuffer sb = new StringBuffer();
    ConcurrentHashMap<String, Object> features = new ConcurrentHashMap<String, Object>();
    printExpress(tree, sb, features);
    log.info("==== LINE {}", sb.toString());

    // check if caller is logger
    if (features.size() > 0 && features.containsKey("callers")) {
      Set<String> callers = (Set<String>) features.get("callers");
      boolean isLoggerCaller = false;
      for (String caller : callers) {
        if (variablesLogger.containsKey(caller)) {
          isLoggerCaller = true;
          break;
        }
      }
      // check if the statement contain sensitive word
      boolean isContainSerect = false;
      if (isLoggerCaller) {
        String statement = sb.toString().toLowerCase();
        // check statement if contains secret
        for (String word : secrecKeywordsSet) {
          int posWord = statement.indexOf(word.toLowerCase());
          if (posWord > -1) {
            isContainSerect = true;
            break;
          }
        }
      }
      // create Issue if the statement contain sensitive word
      if (isContainSerect) {
        context.reportIssue(this, tree, "Log content contains secret keyword");
      }
    }

    super.visitExpressionStatement(tree);
  }

  /**
   * visit and cache variable with its full-type
   * @param tree
   */
  @Override
  public void visitVariable(VariableTree tree) {
    StringBuffer sb = new StringBuffer();
    ConcurrentHashMap<String, Object> features = new ConcurrentHashMap<String, Object>();
    printExpress(tree, sb, features);
    log.info("==== LINE {}", sb.toString());

    // each variables with its full-tpye
    String varName = tree.simpleName().toString();
    String varType = tree.type().toString();
    String varFulltype = types2Fullname.get(varType);
    if (varFulltype == null) varFulltype = varType;
    variables2Fulltype.putIfAbsent(varName, varFulltype);

    // if it is logger variables
    if (packageMust.contains(varFulltype)) {
      variablesLogger.putIfAbsent(varName, varFulltype);
    }

    super.visitVariable(tree);
  }


  /**
   * print PackageName
   * @param packageName
   */
  private static void printPackageName(ExpressionTree packageName) {
    StringBuilder sb = new StringBuilder();
    ExpressionTree expr = packageName;
    while (expr.is(Tree.Kind.MEMBER_SELECT)) {
      MemberSelectExpressionTree mse = (MemberSelectExpressionTree) expr;
      sb.insert(0, mse.identifier().name());
      sb.insert(0, mse.operatorToken().text());
      expr = mse.expression();
    }
    IdentifierTree idt = (IdentifierTree) expr;
    sb.insert(0, idt.name());

    LOGGER.debug("PackageName: {}", sb);
  }


  public void printExpress(Tree tree, StringBuffer sb, ConcurrentHashMap<String,Object> features) {
    printExpress(tree, sb, "", features);
  }

  /**
   * parse recursively syntax-tree to string,
   * get features of some syntax, eg. callers of a statement
   *
   * @param tree
   * @param sb  [output] StringBuffer
   * @param sep Separator between item
   */
  public void printExpress(Tree tree, StringBuffer sb, String sep, ConcurrentHashMap<String,Object> features) {
    String sepParent = sep;
    if (tree instanceof ExpressionStatementTree) {
      log.info("==== printExpress ExpressionStatementTreeImpl");
      ExpressionStatementTreeImpl treeImpl = (ExpressionStatementTreeImpl) tree;
      List<Tree> children = treeImpl.children();
      if (children != null) {
        for (Tree sub : children) {
          printExpress(sub, sb, sep, features);
        }
      }
    } else if (tree instanceof MethodInvocationTree) {
      log.info("==== printExpress MethodInvocationTreeImpl");
      MethodInvocationTreeImpl treeImpl = (MethodInvocationTreeImpl) tree;
      List<Tree> children = treeImpl.children();
      if (children != null) {
        for (Tree sub : children) {
          printExpress(sub, sb, "", features);
          sep = sepParent;
        }
      }
      // feature: method caller, maybe many
      Set<String> callers = null;
      if (features.containsKey("callers")) {
        callers = (HashSet<String>) features.get("callers");
      } else {
        callers = new HashSet<String>();
      }
      callers.add(treeImpl.firstToken().text());
      features.put("callers", callers);
    } else if (tree instanceof MemberSelectExpressionTree) {
      log.info("==== printExpress MemberSelectExpressionTreeImpl");
      MemberSelectExpressionTreeImpl treeImpl = (MemberSelectExpressionTreeImpl) tree;
      List<Tree> children = treeImpl.children();
      if (children != null) {
        for (Tree sub : children) {
          printExpress(sub, sb, sep, features);
        }
        //features.putIfAbsent("target", )
      }
    } else if (tree instanceof Arguments) {
      log.info("==== printExpress ArgumentListTreeImpl");
      ArgumentListTreeImpl treeImpl = (ArgumentListTreeImpl) tree;
      List<Tree> children = treeImpl.children();
      if (children != null) {
        for (Tree sub : children) {
          printExpress(sub, sb, sep, features);
        }
      }
    } else if (tree instanceof ImportTree) {
      log.info("==== printExpress JavaTree.ImportTreeImpl");
      ImportTreeImpl treeImpl = (ImportTreeImpl) tree;
      List<Tree> children = treeImpl.children();
      if (children != null) {
        for (Tree sub : children) {
          printExpress(sub, sb, sep, features);
        }
      }
    } else if (tree instanceof IdentifierTree) {
      log.info("==== printExpress IdentifierTreeImpl");
      IdentifierTreeImpl treeImpl = (IdentifierTreeImpl) tree;
      List<Tree> children = treeImpl.children();
      if (children != null) {
        for (Tree sub : children) {
          if(sb.length() > 0) sb.append(sep);
          printExpress(sub, sb, sep, features);
        }
      } else {
        //sb.append(treeImpl.value);
        log.info("");
      }
    } else if (tree instanceof VariableTree) {
      log.info("==== printExpress VariableTreeImpl");
      VariableTreeImpl treeImpl = (VariableTreeImpl) tree;
      List<Tree> children = treeImpl.children();
      if (children != null) {
        for (Tree sub : children) {
          printExpress(sub, sb, " ", features);
          sep = sepParent;
        }
      } else {
        //sb.append(treeImpl.value);
        log.info("");
      }
    } else if (tree instanceof ModifiersTree) {
      log.info("==== printExpress ModifiersTreeImpl");
      ModifiersTreeImpl treeImpl = (ModifiersTreeImpl) tree;
      List<Tree> children = treeImpl.children();
      if (children != null) {
        for (Tree sub : children) {
          printExpress(sub, sb, sep, features);
        }
      } else {
        //sb.append(treeImpl.value);
        log.info("");
      }
    } else if (tree instanceof ModifierKeywordTree) {
      log.info("==== printExpress ModifierKeywordTreeImpl");
      ModifierKeywordTreeImpl treeImpl = (ModifierKeywordTreeImpl) tree;
      if(sb.length() > 0) sb.append(sep);
      sb.append(treeImpl.text());
    } else if (tree instanceof AnnotationTree) {
      log.info("==== printExpress AnnotationTreeImpl");
      AnnotationTreeImpl treeImpl = (AnnotationTreeImpl) tree;
      List<Tree> children = treeImpl.children();
      if (children != null) {
        for (Tree sub : children) {
          printExpress(sub, sb, "", features);
          sep = sepParent;
        }
      } else {
        //sb.append(treeImpl.value);
        log.info("");
      }
    } else if (tree instanceof BinaryExpressionTree) {
      log.info("==== printExpress BinaryExpressionTreeImpl");
      BinaryExpressionTreeImpl treeImpl = (BinaryExpressionTreeImpl) tree;
      List<Tree> children = treeImpl.children();
      if (children != null) {
        for (Tree sub : children) {
          printExpress(sub, sb, sep, features);
        }
      } else {
        //sb.append(treeImpl.value);
        log.info("");
      }
    } else if (tree instanceof AssignmentExpressionTree) {
      log.info("==== printExpress AssignmentExpressionTreeImpl");
      AssignmentExpressionTreeImpl treeImpl = (AssignmentExpressionTreeImpl) tree;
      List<Tree> children = treeImpl.children();
      if (children != null) {
        for (Tree sub : children) {
          printExpress(sub, sb, sep, features);
        }
      } else {
        //sb.append(treeImpl.value);
        log.info("");
      }
    } else if (tree instanceof NewClassTree) {
      log.info("==== printExpress NewClassTreeImpl");
      NewClassTreeImpl treeImpl = (NewClassTreeImpl) tree;
      List<Tree> children = treeImpl.children();
      if (children != null) {
        for (Tree sub : children) {
          printExpress(sub, sb, sep, features);
        }
      } else {
        //sb.append(treeImpl.value);
        log.info("");
      }
    } else if (tree instanceof MethodTree) {
      log.info("==== printExpress MethodTreeImpl");
      MethodTreeImpl treeImpl = (MethodTreeImpl) tree;
      List<Tree> children = treeImpl.children();
      if (children != null) {
        for (Tree sub : children) {
          printExpress(sub, sb, sep, features);
        }
      } else {
        //sb.append(treeImpl.value);
        log.info("");
      }
    } else if (tree instanceof LambdaExpressionTree) {
      log.info("==== printExpress LambdaExpressionTreeImpl");
      LambdaExpressionTreeImpl treeImpl = (LambdaExpressionTreeImpl) tree;
      List<Tree> children = treeImpl.children();
      if (children != null) {
        for (Tree sub : children) {
          printExpress(sub, sb, sep, features);
        }
      } else {
        //sb.append(treeImpl.value);
        log.info("");
      }
    } else if (tree instanceof LiteralTree) {
      log.info("==== printExpress LiteralTreeImpl");
      LiteralTreeImpl treeImpl = (LiteralTreeImpl) tree;
      if(sb.length() > 0) sb.append(sep);
      sb.append(treeImpl.value());
    } else if (tree instanceof InternalSyntaxToken) {
      log.info("==== printExpress InternalSyntaxToken");
      InternalSyntaxToken treeImpl = (InternalSyntaxToken) tree;
      if(sb.length() > 0) sb.append(sep);
      sb.append(treeImpl.text());
    } else if (tree instanceof InferedTypeTree) {
      log.info("==== printExpress InternalSyntaxToken");
      if(sb.length() > 0) sb.append(sep);
      //sb.append(treeImpl..text());
    } else {
      log.info("==== printExpress Class {}", tree.getClass());
      JavaTree treeImpl = (JavaTree) tree;
      List<Tree> children = treeImpl.getChildren();
      if (children != null) {
        for (Tree sub : children) {
          printExpress(sub, sb, sep, features);
        }
      } else {
        //sb.append(treeImpl.value);
        log.info("");
      }
    }
  }

}
