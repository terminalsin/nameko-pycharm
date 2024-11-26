package com.namecheap.nameko;

import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.types.PyType;
import com.jetbrains.python.psi.types.PyTypeProviderBase;
import com.jetbrains.python.psi.types.TypeEvalContext;
import com.namecheap.nameko.cache.ServiceCache;
import com.namecheap.nameko.model.ServiceInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RpcProxyTypeProvider extends PyTypeProviderBase {
    @Nullable
    @Override
    public Ref<PyType> getCallType(@NotNull PyFunction function,
                                   @NotNull PyCallSiteExpression callSite,
                                   @NotNull TypeEvalContext context) {
        if (!"RpcProxy".equals(function.getName())) {
            return null;
        }

        List<PyExpression> arguments = callSite.getArguments(function);
        if (arguments.isEmpty() || !(arguments.iterator().next() instanceof PyStringLiteralExpression)) {
            return null;
        }

        String serviceName = ((PyStringLiteralExpression) arguments.iterator().next()).getStringValue();
        return createTypeFromServiceName(serviceName, callSite);
    }

    @Override
    public @Nullable Ref<PyType> getReferenceType(@NotNull PsiElement referenceTarget,
                                                 @NotNull TypeEvalContext context, 
                                                 @Nullable PsiElement anchor) {
        // Handle class attribute references
        if (referenceTarget instanceof PyTargetExpression) {
            PyTargetExpression target = (PyTargetExpression) referenceTarget;
            PyExpression value = target.findAssignedValue();
            
            if (value instanceof PyCallExpression) {
                PyCallExpression call = (PyCallExpression) value;
                if ("RpcProxy".equals(call.getCallee().getName())) {
                    PyExpression[] args = call.getArguments();
                    if (args.length > 0 && args[0] instanceof PyStringLiteralExpression) {
                        String serviceName = ((PyStringLiteralExpression) args[0]).getStringValue();
                        return createTypeFromServiceName(serviceName, target);
                    }
                }
            }
        }
        
        return null;
    }

    private Ref<PyType> createTypeFromServiceName(String serviceName, PsiElement anchor) {
        ServiceInfo serviceInfo = ServiceCache.getServices(
            anchor.getProject(),
            anchor.getContainingFile().getVirtualFile()
        ).get(serviceName);

        if (serviceInfo == null) {
            return null;
        }

        return Ref.create(new PyCustomType(serviceInfo, serviceName, anchor));
    }
} 