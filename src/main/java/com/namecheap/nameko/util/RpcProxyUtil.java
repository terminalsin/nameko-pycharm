package com.namecheap.nameko.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.impl.references.PyQualifiedReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RpcProxyUtil {
    
    @Nullable
    public static String getServiceNameFromProxyOld(@NotNull PsiElement element) {
        PyReferenceExpression refExpr = (PyReferenceExpression) element;
        PsiPolyVariantReference reference = refExpr.getReference();
        //System.out.println("Reference: " + reference);
        PsiElement target = reference.resolve();

        //System.out.println("Target: " + target);
        if (target == null || !(target.getParent() instanceof PyAssignmentStatement)) return null;
        
        PyAssignmentStatement assignment = (PyAssignmentStatement) target.getParent();

        //System.out.println("Assignment: " + assignment);
        if (assignment == null) return null;
        
        PyExpression value = assignment.getAssignedValue();

        //System.out.println("Value: " + value.getText());
        if (!(value instanceof PyCallExpression)) return null;
        
        PyCallExpression call = (PyCallExpression) value;

        //System.out.println("Call: " + call.getCallee().getName());
        if (!"RpcProxy".equals(call.getCallee().getName())) return null;

        //System.out.println("Service arg: " + call.getArguments()[0]);
        PyStringLiteralExpression serviceArg = (PyStringLiteralExpression) call.getArguments()[0];
        return serviceArg.getStringValue();
    }

    public static boolean isRpcProxyServiceName(PyStringLiteralExpression element) {
        PsiElement parent = element.getParent();
        if (!(parent instanceof PyArgumentList)) return false;

        PsiElement call = parent.getParent();
        if (!(call instanceof PyCallExpression)) return false;

        PyExpression callee = ((PyCallExpression) call).getCallee();
        return callee != null && "RpcProxy".equals(callee.getName());
    }
    
    public static boolean isRpcProxyReference(@NotNull PsiElement element) {
        return element instanceof PyReferenceExpression && 
               getServiceNameFromProxy(element) != null;
    }

    @Nullable
    public static String getServiceNameFromProxy(@NotNull PsiElement element) {
        if (element instanceof PyReferenceExpression) {
            PyReferenceExpression refExpr = (PyReferenceExpression) element;

            if (refExpr.getQualifier() == null) return null;
            // Handle self.service_name case
            if ("self".equals(refExpr.getQualifier().getName())) {
                PyTargetExpression target = findServiceAttribute(refExpr);
                if (target != null) {
                    return getServiceNameFromRpcProxy(target);
                }
            }

            // Handle existing RpcProxy case
            PsiElement target = refExpr.getReference().resolve();
            if (target instanceof PyTargetExpression) {
                return getServiceNameFromRpcProxy((PyTargetExpression) target);
            }
        }
        return null;
    }

    private static PyTargetExpression findServiceAttribute(PyReferenceExpression refExpr) {
        String attrName = refExpr.getName();
        PyClass containingClass = PsiTreeUtil.getParentOfType(refExpr, PyClass.class);
        if (containingClass != null) {
            return containingClass.findClassAttribute(attrName, false, null);
        }
        return null;
    }

    private static String getServiceNameFromRpcProxy(PyTargetExpression target) {
        PyExpression value = target.findAssignedValue();
        if (value instanceof PyCallExpression) {
            PyCallExpression call = (PyCallExpression) value;
            if ("RpcProxy".equals(call.getCallee().getName())) {
                PyExpression[] args = call.getArguments();
                if (args.length > 0 && args[0] instanceof PyStringLiteralExpression) {
                    return ((PyStringLiteralExpression) args[0]).getStringValue();
                }
            }
        }
        return null;
    }
} 