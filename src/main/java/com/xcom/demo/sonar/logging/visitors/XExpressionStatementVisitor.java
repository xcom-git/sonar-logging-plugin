package com.xcom.demo.sonar.logging.visitors;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.sonar.plugins.java.api.tree.*;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * Visitor for ExpressionStatement to process ExpressionStatementTree
 */
@Data
@Slf4j
public class XExpressionStatementVisitor extends XBaseVisitor {

    // callers are classes
    protected Set<String> caller;
    // methods are functions
    protected Set<String> method;   // TODO

    public XExpressionStatementVisitor() {
        caller = new HashSet<String>();
        method = new HashSet<String>();
    }

    public XExpressionStatementVisitor process(Tree tree) {
        clear();
        scan(tree);
        return this;
    }

    public XExpressionStatementVisitor clear() {
        caller.clear();
        method.clear();
        super.clear();
        return this;
    }

    @Override
    protected void scan(@Nullable Tree tree) {
        if (tree != null) {
            Class<?>[] interfaces = tree.getClass().getInterfaces();
            if (interfaces.length > 0) {
                indent().append(interfaces[0].getSimpleName());
                if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
                    MethodInvocationTree treeCon = (MethodInvocationTree) tree;
                }
                if (tree.is(Tree.Kind.MEMBER_SELECT)) {
                    MemberSelectExpressionTree treeCon = (MemberSelectExpressionTree) tree;
                    // first IDENTIFIER of MemberSelectExpression
                    if (treeCon.expression().is(Tree.Kind.IDENTIFIER)) {
                        IdentifierTree treeCaller = (IdentifierTree) treeCon.expression();
                        caller.add(treeCaller.name());
                    }

                    if (treeCon.parent().is(Tree.Kind.METHOD_INVOCATION)) {
                        StringBuffer oldAst = ast;
                        StringBuffer oldCode = code;
                        ast = new StringBuffer();
                        code = new StringBuffer();
                        super.scan(treeCon);
                        String snippet = String.valueOf(this.getCode());
                        method.add(snippet);
                        ast = oldAst;
                        code = oldCode;
                        //return;
                    }
                }
                if (tree.is(Tree.Kind.ARGUMENTS)) {
                    Arguments treeCon = (Arguments) tree;
                    for (ExpressionTree argTree : treeCon) {
                        this.scan(argTree);
                        log.info("");
                    }
                    return;
                } else if (tree.is(Tree.Kind.IDENTIFIER)) {
                    IdentifierTree treeCon = (IdentifierTree) tree;
                    ast.append(" ");
                    ast.append(treeCon.name());
                    if (code.length() > 0) code.append(".");
                    code.append(treeCon.name());
                } else if (tree.is(
                        Tree.Kind.INT_LITERAL,
                        Tree.Kind.LONG_LITERAL,
                        Tree.Kind.FLOAT_LITERAL,
                        Tree.Kind.DOUBLE_LITERAL,
                        Tree.Kind.BOOLEAN_LITERAL,
                        Tree.Kind.CHAR_LITERAL,
                        Tree.Kind.STRING_LITERAL,
                        Tree.Kind.TEXT_BLOCK,
                        Tree.Kind.NULL_LITERAL)) {
                    LiteralTree treeCon = (LiteralTree) tree;
                    ast.append(" ");
                    ast.append(treeCon.value());
                    code.append(treeCon.value());
                } else if (tree.is(
                        // TODO Kind for all BinaryExpressionTree.class
                        Tree.Kind.PLUS)) {
                    BinaryExpressionTree treeCon = (BinaryExpressionTree) tree;
                    ast.append(" ");
                    code.append("(");
                    this.scan(treeCon.leftOperand());
                    this.scan(treeCon.operatorToken());
                    this.scan(treeCon.rightOperand());
                    code.append(")");
                    return;
                } else {
                    log.info("");
                }
                ast.append("\n");
            }
        }
        super.scan(tree);
    }

}
