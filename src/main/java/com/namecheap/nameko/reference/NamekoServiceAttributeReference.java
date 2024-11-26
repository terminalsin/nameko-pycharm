package com.namecheap.nameko.reference;

public class NamekoServiceAttributeReference extends PsiReferenceBase<PyTargetExpression> {
    
    public NamekoServiceAttributeReference(@NotNull PyTargetExpression element) {
        super(element);
    }

    @Nullable
    @Override
    public PsiElement resolve() {
        PyExpression value = getElement().findAssignedValue();
        if (!(value instanceof PyCallExpression)) return null;
        
        PyCallExpression call = (PyCallExpression) value;
        PyExpression[] args = call.getArguments();
        if (args.length == 0 || !(args[0] instanceof PyStringLiteralExpression)) return null;
        
        String serviceName = ((PyStringLiteralExpression) args[0]).getStringValue();
        ServiceInfo service = ServiceCache.getServices(
            getElement().getProject(), 
            getElement().getContainingFile().getVirtualFile()
        ).get(serviceName);
        
        return service != null ? service.getServiceClass() : null;
    }
}