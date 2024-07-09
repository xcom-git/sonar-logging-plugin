package com.xcom.demo.sonar.logging.visitors;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.sonar.plugins.java.api.tree.IdentifierTree;
import org.sonar.plugins.java.api.tree.ImportTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;

import javax.annotation.Nullable;


/**
 * Visitor for Import to process ImportTree
 */
@Data
@Slf4j
public class XImportVisitor extends XBaseVisitor {

    // class name of package
    // eg. Tracer for import com.google.cloud.trace.Tracer
    protected String className;

    @Override
    protected void scan(@Nullable Tree tree) {
        if (tree != null) {
            Class<?>[] interfaces = tree.getClass().getInterfaces();
            if (interfaces.length > 0) {
                indent().append(interfaces[0].getSimpleName());
                /*switch (tree.kind()) {
                    case IMPORT:
                        ImportTree treeCon = (ImportTree) tree;
                        log.info("");
                        break;
                    case IDENTIFIER:
                        IdentifierTree treeCon = (IdentifierTree) tree;
                        ast.append(" ");
                        ast.append(treeCon.name());
                        if (code.length() > 0) code.append(".");
                        code.append(treeCon.name());
                        break;
                    default:
                        log.info("");
                        break;
                }*/
                if (tree.is(Kind.IMPORT)) {
                    ImportTree treeCon = (ImportTree) tree;
                    // import class name
                    className = ((MemberSelectExpressionTree) treeCon.qualifiedIdentifier()).identifier().name();
                } else if (tree.is(Kind.IDENTIFIER)) {
                    IdentifierTree treeCon = (IdentifierTree) tree;
                    ast.append(" ");
                    ast.append(treeCon.name());
                    if (code.length() > 0) code.append(".");
                    code.append(treeCon.name());
                } else {
                    log.info("");
                }
                ast.append("\n");
            }
        }
        super.scan(tree);
    }

}
