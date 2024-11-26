package com.namecheap.nameko.reference;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.python.PyTokenTypes;
import com.jetbrains.python.psi.*;
import com.namecheap.nameko.util.RpcProxyUtil;
import org.jetbrains.annotations.NotNull;

public class NamekoReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        // Register for method references and service references
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PyReferenceExpression.class),
            new PsiReferenceProvider() {
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                                     @NotNull ProcessingContext context) {
                    if (!(element instanceof PyReferenceExpression)) {
                        return PsiReference.EMPTY_ARRAY;
                    }

                    PyReferenceExpression refExpr = (PyReferenceExpression) element;
                    PyExpression qualifier = refExpr.getQualifier();
                    
                    // Handle service attribute references (self.sq_website_service)
                    if (qualifier != null && qualifier instanceof PyReferenceExpression) {
                        PyReferenceExpression qualifierRef = (PyReferenceExpression) qualifier;
                        if ("self".equals(qualifierRef.getName())) {
                            // Find the corresponding class attribute
                            PyClass containingClass = PsiTreeUtil.getParentOfType(refExpr, PyClass.class);
                            if (containingClass != null) {
                                PyTargetExpression target = containingClass.findClassAttribute(refExpr.getName(), false, null);
                                if (target != null) {
                                    return new PsiReference[]{new NamekoServiceAttributeReference(target)};
                                }
                            }
                        }
                    }
                    
                    // Handle method references (self.sq_website_service.get)
                    if (qualifier != null && RpcProxyUtil.isRpcProxyReference(qualifier)) {
                        return new PsiReference[]{new NamekoMethodReference(refExpr)};
                    }

                    return PsiReference.EMPTY_ARRAY;
                }
            }
        );

        // Register for service name string literals
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PyStringLiteralExpression.class),
            new PsiReferenceProvider() {
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                                     @NotNull ProcessingContext context) {
                    if (!(element instanceof PyStringLiteralExpression)) {
                        return PsiReference.EMPTY_ARRAY;
                    }

                    PyStringLiteralExpression stringLiteral = (PyStringLiteralExpression) element;
                    if (RpcProxyUtil.isRpcProxyServiceName(stringLiteral)) {
                        return new PsiReference[]{new NamekoServiceReference(stringLiteral)};
                    }

                    return PsiReference.EMPTY_ARRAY;
                }
            }
        );
    }
}