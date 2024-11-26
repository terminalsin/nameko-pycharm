package com.namecheap.nameko.reference;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.jetbrains.python.psi.PyCallExpression;
import com.jetbrains.python.psi.PyExpression;
import com.jetbrains.python.psi.PyStringLiteralExpression;
import com.jetbrains.python.psi.PyTargetExpression;
import com.namecheap.nameko.cache.ServiceCache;
import com.namecheap.nameko.model.ServiceInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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