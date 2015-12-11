package com.intellij.codeInspection;

import com.intellij.codeInsight.daemon.GroupNames;
import com.intellij.ide.DataManager;
import com.intellij.lang.java.JavaFindUsagesProvider;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Query;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Created by pip on 10.12.2015.
 */
public class FindGPSUsageInspection extends BaseJavaLocalInspectionTool {

    @NonNls
    private static final String DESCRIPTION_TEMPLATE = ("Finds usages of GPS");

    @NotNull
    public String getDisplayName() {

        return "Finds usages of GPS";
    }

    @NotNull
    public String getGroupDisplayName() {
        return GroupNames.BUGS_GROUP_NAME;
    }

    @NotNull
    public String getShortName() {
        return "GPS_usage";
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaRecursiveElementVisitor() {
            @Nullable
            public void inspectMethodCallExpression(PsiMethodCallExpression expression) {
                DataContext dataContext = DataManager.getInstance().getDataContext();
                Project project = DataKeys.PROJECT.getData(dataContext);
                PsiManager psiManager = PsiManager.getInstance(project);
                String classQName = "android.location.LocationManager";
                PsiClass locationClass = JavaPsiFacade.getInstance(psiManager.getProject()).findClass(classQName, GlobalSearchScope.allScope(project));
                Collection<PsiReference> locationUsages = findUsages(locationClass);
//                PsiManager psiManager = PsiManager.getInstance();
//                PsiExpression[] exList = expression.getArgumentList().getExpressions();
//                for (PsiExpression ex : exList) {
//                    if (ex.getType() instanceof PsiClassReferenceType) {
//                        PsiClass aClass = ((PsiClassReferenceType) ex.getType()).resolve();
//                        aClass.getContainingFile().getVirtualFile();
//                        if (aClass.getQualifiedName().equals("android.location.LocationManager")) {
//                            System.out.println("found a " + aClass.getName());
//                        }
//                    }
//                }
            }
        };
    }

    private Collection<PsiReference> findUsages(PsiElement element) {
        JavaFindUsagesProvider usagesProvider = new JavaFindUsagesProvider();
        usagesProvider.canFindUsagesFor(element);
        Query<PsiReference> query = ReferencesSearch.search(element);
        Collection<PsiReference> result = query.findAll();
        return result;

    }
}