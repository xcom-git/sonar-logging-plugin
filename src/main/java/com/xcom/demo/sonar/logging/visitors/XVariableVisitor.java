package com.xcom.demo.sonar.logging.visitors;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.sonar.plugins.java.api.tree.*;

import javax.annotation.Nullable;


/**
 * Visitor for Variable to process VariableTree
 */
@Data
@Slf4j
public class XVariableVisitor extends XBaseVisitor {

    // name of variable
    protected String name;
    // type of variable
    protected String type;
    // value of variable
    protected Object value;

    @Override
    protected void scan(@Nullable Tree tree) {
        if (tree != null) {
            Class<?>[] interfaces = tree.getClass().getInterfaces();
            if (interfaces.length > 0) {
                indent().append(interfaces[0].getSimpleName());
                if (tree.is(Tree.Kind.VARIABLE)) {
                    VariableTree treeCon = (VariableTree) tree;
                    name = treeCon.simpleName().name();
                    type = treeCon.type().toString();
                    // san sub
                    if (treeCon.initializer() != null) {
                        StringBuffer oldAst = ast;
                        StringBuffer oldCode = code;
                        ast = new StringBuffer();
                        code = new StringBuffer();
                        this.scan(treeCon.initializer());
                        value = this.getCode();
                        ast = oldAst;
                        code = oldCode;
                        //return;
                    }
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
                } else {
                    log.info("");
                }
                ast.append("\n");
            }
        }
        super.scan(tree);
    }

}
