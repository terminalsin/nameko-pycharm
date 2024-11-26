package com.namecheap.nameko;

import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.resolve.PyResolveContext;
import com.jetbrains.python.psi.resolve.RatedResolveResult;
import com.jetbrains.python.psi.types.PyType;
import com.namecheap.nameko.model.ParameterInfo;
import com.namecheap.nameko.model.ServiceInfo;
import com.namecheap.nameko.model.ServiceMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class PyCustomType implements PyType {
    private final ServiceInfo serviceInfo;
    private final String serviceName;
    private final PsiElement anchor;

    public PyCustomType(ServiceInfo serviceInfo, String serviceName, PsiElement anchor) {
        this.serviceInfo = serviceInfo;
        this.serviceName = serviceName;
        this.anchor = anchor;
    }

    @Override
    public void assertValid(String message) {

    }

    @Override
    public boolean isBuiltin() {
        return false;
    }

    @Override
    public @Nullable List<? extends RatedResolveResult> resolveMember(@NotNull String name,
                                                                      @Nullable PyExpression location,
                                                                      @NotNull AccessDirection direction,
                                                                      @NotNull PyResolveContext resolveContext) {
        ServiceMethod method = serviceInfo.findMethod(name);
        if (method == null) return null;

        return Collections.singletonList(new RatedResolveResult(RatedResolveResult.RATE_HIGH, createMethodElement(method)));
    }

    @Override
    public Object[] getCompletionVariants(String completionPrefix, PsiElement location, ProcessingContext context) {
        return serviceInfo.getMethods().stream()
                .map(this::createMethodElement)
                .toArray();
    }

    private PsiElement createMethodElement(ServiceMethod method) {
        return method.getElement();
    }

    @Override
    public String getName() {
        return "RpcProxy(" + serviceName + ")";
    }
}