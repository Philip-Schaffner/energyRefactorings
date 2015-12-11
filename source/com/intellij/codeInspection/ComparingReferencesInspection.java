package com.intellij.codeInspection;

import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.StringTokenizer;

/**
 * @author max
 */
public class ComparingReferencesInspection extends BaseJavaLocalInspectionTool {

    private final LocalQuickFix myQuickFix = new MyQuickFix();

    @SuppressWarnings({"WeakerAccess"})
    @NonNls
    public String CHECKED_CLASSES = "java.lang.String;java.util.Date";
    @NonNls
    private static final String DESCRIPTION_TEMPLATE = InspectionsBundle.message("inspection.comparing.references.problem.descriptor");

    @NotNull
    public String getDisplayName() {

        return "'==' or '!=' instead of 'equals()'";
    }

    @NotNull
    public String getGroupDisplayName() {
        return GroupNames.BUGS_GROUP_NAME;
    }

    @NotNull
    public String getShortName() {
        return "ComparingReferences";
    }

    private boolean isCheckedType(PsiType type) {
        if (!(type instanceof PsiClassType)) return false;

        StringTokenizer tokenizer = new StringTokenizer(CHECKED_CLASSES, ";");
        while (tokenizer.hasMoreTokens()) {
            String className = tokenizer.nextToken();
            if (type.equalsToText(className)) return true;
        }

        return false;
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitReferenceExpression(PsiReferenceExpression psiReferenceExpression) {
            }


            /*@Override
            public void visitBinaryExpression(PsiBinaryExpression expression) {
                super.visitBinaryExpression(expression);
                IElementType opSign = expression.getOperationTokenType();
                if (opSign == JavaTokenType.EQEQ || opSign == JavaTokenType.NE) {
                    PsiExpression lOperand = expression.getLOperand();
                    PsiExpression rOperand = expression.getROperand();
                    if (rOperand == null || isNullLiteral(lOperand) || isNullLiteral(rOperand)) return;

                    PsiType lType = lOperand.getType();
                    PsiType rType = rOperand.getType();

                    if (isCheckedType(lType) || isCheckedType(rType)) {
                        holder.registerProblem(expression,
                                DESCRIPTION_TEMPLATE, myQuickFix);
                    }
                }
            }*/

            @Override
            public void visitWhileStatement (PsiWhileStatement statement){
                super.visitWhileStatement(statement);
                PsiElement[] children = statement.getChildren();
                for (PsiElement child : children){
                    inspectElement(child);
                }
            }

            public void inspectElement(PsiElement element){
                if (element instanceof PsiBlockStatement) {
                    inspectCodeBlock(((PsiBlockStatement) element).getCodeBlock());
                }else {
                    //System.out.println(element.getText() + " is leaf");
                }
            }

            public void inspectCodeBlock(PsiCodeBlock element){
                PsiStatement statements[] = element.getStatements();
                for (PsiStatement statement : statements){
                    inspectExpressionStatmenent(statement);
                }
            }

            public void inspectExpressionStatmenent(PsiStatement statement){
                if (statement instanceof PsiExpressionStatement){
                    //statement
                }
//                System.out.println(statement.getText() + " is leaf");
            }

            private PsiExpression getExpression(PsiCodeBlock body) {
                final PsiStatement[] statements = body.getStatements();
                if (statements.length == 1) {
                    if (statements[0] instanceof PsiBlockStatement) {
                        return getExpression(((PsiBlockStatement)statements[0]).getCodeBlock());
                    }
                    if (statements[0] instanceof PsiReturnStatement || statements[0] instanceof PsiExpressionStatement) {
                        if (statements[0] instanceof PsiReturnStatement) {
                            final PsiReturnStatement returnStatement = (PsiReturnStatement)statements[0];
                            return returnStatement.getReturnValue();
                        }
                        else {
                            final PsiExpression expression = ((PsiExpressionStatement)statements[0]).getExpression();
                            final PsiType psiType = expression.getType();
                            if (psiType != PsiType.VOID) {
                                return null;
                            }
                            return expression;
                        }
                    }
                }
                return null;
            }

        };
    }

    private static boolean isNullLiteral(PsiExpression expr) {
        return expr instanceof PsiLiteralExpression && "null".equals(expr.getText());
    }

    private static class MyQuickFix implements LocalQuickFix {
        @NotNull
        public String getName() {
            // The test (see the TestThisPlugin class) uses this string to identify the quick fix action.
            return InspectionsBundle.message("inspection.comparing.references.use.quickfix");
        }


        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            try {
                PsiBinaryExpression binaryExpression = (PsiBinaryExpression) descriptor.getPsiElement();
                IElementType opSign = binaryExpression.getOperationTokenType();
                PsiExpression lExpr = binaryExpression.getLOperand();
                PsiExpression rExpr = binaryExpression.getROperand();
                if (rExpr == null)
                    return;

                PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
                PsiMethodCallExpression equalsCall = (PsiMethodCallExpression) factory.createExpressionFromText("a.equals(b)", null);

                equalsCall.getMethodExpression().getQualifierExpression().replace(lExpr);
                equalsCall.getArgumentList().getExpressions()[0].replace(rExpr);

                PsiExpression result = (PsiExpression) binaryExpression.replace(equalsCall);

                if (opSign == JavaTokenType.NE) {
                    PsiPrefixExpression negation = (PsiPrefixExpression) factory.createExpressionFromText("!a", null);
                    negation.getOperand().replace(result);
                    result.replace(negation);
                }
            } catch (IncorrectOperationException e) {
                System.out.println(e);
            }
        }

        @NotNull
        public String getFamilyName() {
            return getName();
        }
    }

    public JComponent createOptionsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JTextField checkedClasses = new JTextField(CHECKED_CLASSES);
        checkedClasses.getDocument().addDocumentListener(new DocumentAdapter() {
            public void textChanged(DocumentEvent event) {
                CHECKED_CLASSES = checkedClasses.getText();
            }
        });

        panel.add(checkedClasses);
        return panel;
    }

    public boolean isEnabledByDefault() {
        return true;
    }
}
