package com.namecheap.nameko.reference;

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