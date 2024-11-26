package com.namecheap.nameko.reference;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.python.psi.PyExpression;
import com.jetbrains.python.psi.PyReferenceExpression;
import com.namecheap.nameko.cache.ServiceCache;
import com.namecheap.nameko.model.ServiceInfo;
import com.namecheap.nameko.model.ServiceMethod;
import com.namecheap.nameko.util.RpcProxyUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NamekoMethodReference extends PsiReferenceBase<PyReferenceExpression> {
    
    public NamekoMethodReference(@NotNull PyReferenceExpression element) {
        super(element);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        PyReferenceExpression element = getElement();
        PyExpression qualifier = element.getQualifier();
        if (qualifier == null) return null;

        String serviceName = RpcProxyUtil.getServiceNameFromProxy(qualifier);
        if (serviceName == null) return null;

        ServiceInfo service = ServiceCache.getServices(
            element.getProject(), 
            element.getContainingFile().getVirtualFile()
        ).get(serviceName);
        
        if (service == null) return null;

        String methodName = element.getName();
        ServiceMethod method = service.findMethod(methodName);
        return method != null ? method.getElement() : null;
    }
} 