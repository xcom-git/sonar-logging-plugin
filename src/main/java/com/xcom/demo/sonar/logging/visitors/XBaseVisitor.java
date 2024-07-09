package com.xcom.demo.sonar.logging.visitors;

import com.xcom.demo.sonar.logging.utils.StringUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.sonar.plugins.java.api.tree.*;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Base Visitor
 */
@Data
@Slf4j
public class XBaseVisitor extends BaseTreeVisitor {
    protected final int INDENT_SPACES = 2;
    protected int indentLevel;

    // AST content
    protected StringBuffer ast;
    // source code
    protected StringBuffer code;

    public XBaseVisitor() {
        ast = new StringBuffer();
        code = new StringBuffer();
    }

    public XBaseVisitor process(Tree tree) {
        clear();
        scan(tree);
        return this;
    }

    public XBaseVisitor clear() {
        ast.setLength(0);
        code.setLength(0);
        return this;
    }

    /**
     * indent space for displaying AST content
     * @return
     */
    protected StringBuffer indent() {
        return ast.append(StringUtils.spaces(INDENT_SPACES * indentLevel));
    }

    @Override
    protected void scan(List<? extends Tree> trees) {
        if (!trees.isEmpty()) {
            ast.deleteCharAt(ast.length() - 1);
            ast.append(" : [\n");
            super.scan(trees);
            indent().append("]\n");
        }
    }

    @Override
    protected void scan(@Nullable Tree tree) {
        indentLevel++;
        super.scan(tree);
        indentLevel--;
    }
}
