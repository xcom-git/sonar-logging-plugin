package com.xcom.demo.sonar.logging.checks;


import com.xcom.demo.sonar.logging.utils.StringUtils;
import com.xcom.demo.sonar.logging.visitors.XBaseVisitor;
import com.xcom.demo.sonar.logging.visitors.XExpressionStatementVisitor;
import com.xcom.demo.sonar.logging.visitors.XImportVisitor;
import com.xcom.demo.sonar.logging.visitors.XVariableVisitor;
import lombok.extern.slf4j.Slf4j;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.JavaFileScanner;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.tree.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Plugin for check logging standard.
 * check necessary logger package, and sentive words in log content
 * TODO: trace id in log content
 * <p>
 * 1. settings of packages of logger as 'packageMust'
 * 2. settings of illegal packages as 'packageIllegal'
 * 3. settings of sensitive keyword
 */
@Slf4j
@Rule(key = "XLoggerCheck")
public class XLoggerCheck extends BaseTreeVisitor implements JavaFileScanner {
    private static final Logger LOGGER = Loggers.get(XLoggerCheck.class);
    private JavaFileScannerContext context;
    private XBaseVisitor xBaseVisitor = new XBaseVisitor();
    private XImportVisitor xImportVisitor = new XImportVisitor();
    private XExpressionStatementVisitor xExpressionStatementVisitor = new XExpressionStatementVisitor();
    private XVariableVisitor xVariableVisitor = new XVariableVisitor();

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
     *
     * @param tree
     */
    @Override
    public void visitImport(ImportTree tree) {
        String line = xImportVisitor.process(tree).getCode().toString();

        // package imported and necessary
        if (packageMust.contains(line)) {
            packageChecked.add(line);
        }
        // package imported and illegal
        if (packageIllegal.contains(line)) {
            // ISSUE
            context.reportIssue(this, tree, "Illegal pakcage");
        }
        // class name to full type
        String name = xImportVisitor.getClassName();
        if (StringUtils.isNotEmpty(name)) types2Fullname.putIfAbsent(name, line);

        log.info("==== IMPORT {}", line);
        super.visitImport(tree);
    }

    /**
     * visit and analyze expression statement
     *
     * @param tree
     */
    @Override
    public void visitExpressionStatement(ExpressionStatementTree tree) {
        String line = xExpressionStatementVisitor.process(tree).getCode().toString();

        // check if caller is logger
        Set<String> callers = xExpressionStatementVisitor.getCaller();
        if (callers != null && callers.size() > 0) {
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
                String statement = line.toLowerCase();
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
     *
     * @param tree
     */
    @Override
    public void visitVariable(VariableTree tree) {
        String line = xVariableVisitor.process(tree).getCode().toString();

        // each variables with its full-tpye
        String varName = tree.simpleName().toString();
        String varType = tree.type().toString();
        String varFulltype = types2Fullname.get(varType);
        // canbe null
        String varValue = String.valueOf(xVariableVisitor.getValue());
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
     *
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

}
