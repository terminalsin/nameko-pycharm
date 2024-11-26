package com.namecheap.nameko.reference;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.python.psi.PyStringLiteralExpression;
import com.namecheap.nameko.cache.ServiceCache;
import com.namecheap.nameko.model.ServiceInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NamekoServiceReference extends PsiReferenceBase<PyStringLiteralExpression> {
    
    public NamekoServiceReference(@NotNull PyStringLiteralExpression element) {
        super(element);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        String serviceName = getElement().getStringValue();
        ServiceInfo service = ServiceCache.getServices(
            getElement().getProject(), 
            getElement().getContainingFile().getVirtualFile()
        ).get(serviceName);

        return service != null ? service.getServiceClass() : null;
    }
}