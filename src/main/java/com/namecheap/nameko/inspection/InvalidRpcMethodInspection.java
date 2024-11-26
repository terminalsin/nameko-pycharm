package com.namecheap.nameko.inspection;

import com.intellij.codeInspection.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.PyExpression;
import com.jetbrains.python.psi.PyReferenceExpression;
import com.namecheap.nameko.cache.ServiceCache;
import com.namecheap.nameko.model.ServiceInfo;
import com.namecheap.nameko.util.RpcProxyUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class InvalidRpcMethodInspection extends LocalInspectionTool {
    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new PsiElementVisitor() {
            @Override
            public void visitElement(@NotNull PsiElement element) {
                if (!(element instanceof PyReferenceExpression)) return;

                //System.out.println("Ref Element inspection: " + element.getParent().getText());
                //System.out.println("Parent of type " + element.getParent().getClass());
                PyReferenceExpression refExpr = (PyReferenceExpression) element;
                PyReferenceExpression reference = PsiTreeUtil.getParentOfType(
                        refExpr,
                        PyReferenceExpression.class,
                        true
                );

                if (reference == null) {
                    //System.out.println("No reference expression found");
                    return;
                }
                //System.out.println("Found reference: " + reference.getText());

                // Get the qualifier (the part before the dot)
                PyExpression qualifier = reference.getQualifier();
                if (qualifier == null || !RpcProxyUtil.isRpcProxyReference(qualifier)) {
                    //System.out.println("Not an RPC proxy reference");
                    return;
                }

                //System.out.println("Ref Element: " + reference + " with parent " + reference.getParent() + " qualifier " + qualifier);
                String serviceName = RpcProxyUtil.getServiceNameFromProxy(qualifier);
                if (serviceName == null) return;

                String methodName = reference.getName();
                final Map<String, ServiceInfo> services = ServiceCache.getServices(element.getProject(), element.getContainingFile().getVirtualFile());

                if (!services.containsKey(serviceName)) {
                    holder.registerProblem(refExpr,
                        "Service '" + serviceName + "' not found",
                        ProblemHighlightType.WARNING);
                    return;
                }

                if (!services
                        .get(serviceName)
                        .hasMethod(methodName)) {
                    holder.registerProblem(refExpr,
                        "Method '" + methodName + "' not found in service '" + serviceName + "'",
                        ProblemHighlightType.WARNING);
                }
            }
        };
    }
} 