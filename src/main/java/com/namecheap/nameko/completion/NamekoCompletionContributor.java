package com.namecheap.nameko.completion;

public class NamekoCompletionContributor extends CompletionContributor {
    @Override
    protected void addCompletions(@NotNull CompletionParameters parameters,
                                @NotNull ProcessingContext context,
                                @NotNull CompletionResultSet result) {
        PsiElement element = parameters.getPosition();
        Project project = element.getProject();
        
        // Check if we're in an RPC proxy declaration
        if (isRpcProxyDeclaration(element)) {
            String serviceName = getServiceNameFromProxy(element);
            if (serviceName != null) {
                ServiceInfo service = ServiceCache.getServices(project).get(serviceName);
                if (service != null) {
                    for (ServiceMethod method : service.getMethods()) {
                        result.addElement(createMethodLookupElement(service, method));
                    }
                }
            }
        }
    }

    private boolean isRpcProxyDeclaration(PsiElement element) {
        PyAssignmentStatement assignment = PsiTreeUtil.getParentOfType(element, PyAssignmentStatement.class);
        if (assignment == null) return false;

        PyExpression value = assignment.getAssignedValue();
        return value instanceof PyCallExpression && 
               ((PyCallExpression) value).getCallee() != null &&
               "RpcProxy".equals(((PyCallExpression) value).getCallee().getName());
    }

    private String getServiceNameFromProxy(PsiElement element) {
        PyAssignmentStatement assignment = PsiTreeUtil.getParentOfType(element, PyAssignmentStatement.class);
        if (assignment == null) return null;

        PyCallExpression call = (PyCallExpression) assignment.getAssignedValue();
        PyStringLiteralExpression serviceArg = PsiTreeUtil.getChildOfType(call.getArgumentList(), PyStringLiteralExpression.class);
        return serviceArg != null ? serviceArg.getStringValue() : null;
    }
}