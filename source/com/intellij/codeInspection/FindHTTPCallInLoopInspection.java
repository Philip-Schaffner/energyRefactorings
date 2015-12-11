package com.intellij.codeInspection;

import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.lang.java.JavaFindUsagesProvider;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.LightClassReference;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.tree.java.PsiAnnotationImpl;
import com.intellij.psi.search.PsiSearchHelper;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.Query;
import com.intellij.util.QueryExecutor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.StringTokenizer;

/**
 * @author max
 */
public class FindHTTPCallInLoopInspection extends BaseJavaLocalInspectionTool {

    private final LocalQuickFix myQuickFix = new MyQuickFix();

    @SuppressWarnings({"WeakerAccess"})
    @NonNls
    public String CHECKED_CLASSES = "java.lang.String;java.util.Date";
    @NonNls
    private static final String DESCRIPTION_TEMPLATE = ("Finds HTTP calls in loops");

    @NotNull
    public String getDisplayName() {

        return "HTTP calls in loops";
    }

    @NotNull
    public String getGroupDisplayName() {
        return GroupNames.BUGS_GROUP_NAME;
    }

    @NotNull
    public String getShortName() {
        return "HTTP_calls_in_loops";
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
        return new JavaRecursiveElementVisitor() {

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
            public void visitAnnotation(PsiAnnotation annotation){
                if(annotation.getNameReferenceElement().getText().equalsIgnoreCase("POST")){
                    PsiElement annotatedElement = annotation.getParent().getParent();
                    if(annotatedElement instanceof PsiMethod){
                        visitSuspiciousElement(annotatedElement);
                    }
                }
            }

            @Nullable
            public void inspectMethodCallExpression(PsiMethodCallExpression expression){
                PsiExpression[] exList = expression.getArgumentList().getExpressions();
                for (PsiExpression ex : exList) {
                    for (PsiElement param : ex.getChildren()) {
//                        if (getVariable(param) != null) {
//                            PsiVariable var = getVariable(param);
//
                    }
                    if (ex.getType() instanceof PsiClassReferenceType) {
                        PsiClass aClass = ((PsiClassReferenceType) ex.getType()).resolve();
                        aClass.getContainingFile().getVirtualFile();
                        if ((aClass.getQualifiedName().equals("Dummy"))) {
                            //System.out.println("found a " + aClass.getName());
                        } else if (aClass.getQualifiedName().equals("android.location.LocationManager")) {
                            //System.out.println("found a " + aClass.getName());
                        }

                    }
                }
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

            private PsiVariable getVariable(PsiElement element) {
                if (!(element instanceof PsiJavaToken)) {
                    return null;
                }

                final PsiJavaToken token = (PsiJavaToken) element;
                final PsiElement parent = token.getParent();
                if (parent instanceof PsiVariable) {
                    return (PsiVariable) parent;
                }


                if (parent instanceof PsiReferenceExpression) {
                    final PsiReferenceExpression referenceExpression = (PsiReferenceExpression) parent;
                    final PsiElement resolvedReference = referenceExpression.resolve();
                    if (resolvedReference instanceof PsiVariable) {
                        return (PsiVariable) resolvedReference;
                    }
                }


                if (parent instanceof PsiJavaCodeReferenceElement) {
                    final PsiJavaCodeReferenceElement javaCodeReferenceElement = (PsiJavaCodeReferenceElement) parent;
                    return (PsiVariable) PsiTreeUtil.getParentOfType(javaCodeReferenceElement, PsiVariable.class);
                }
                return null;
            }

        };
    }

    private void visitSuspiciousElement(PsiElement annotatedElement) {
        Collection<PsiReference> usages = findUsages(annotatedElement);
        for (PsiReference ref : usages){
            if (ref instanceof PsiReferenceExpression) {
                PsiElement[] children = ((PsiReferenceExpression) ref).getChildren();
                for (PsiElement child : children) {
                    while (!(child instanceof PsiWhileStatement) && child.getParent() != null) {
                        if (child instanceof PsiMethod && ((PsiMethod) child).getName().equalsIgnoreCase("update")){
                            visitSuspiciousElement(child);
                        }
                        child = child.getParent();
                    }
                    if (child instanceof PsiWhileStatement) {
                        System.out.println("found GET in while loop");
                    }
                }
            }
        }
    }

    private Collection<PsiReference> findUsages(PsiElement element) {
        JavaFindUsagesProvider usagesProvider = new JavaFindUsagesProvider();
        usagesProvider.canFindUsagesFor(element);
        Query<PsiReference> query = ReferencesSearch.search(element);
        Collection<PsiReference> result = query.findAll();
        return result;
    }

    private static class MyQuickFix implements LocalQuickFix {
        @NotNull
        public String getName() {
            // The test (see the TestThisPlugin class) uses this string to identify the quick fix action.
            return ("Use energy aware refactoring");
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
