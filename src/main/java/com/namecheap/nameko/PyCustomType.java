package com.namecheap.nameko;

rivate abstract class PyCustomType implements PyType {
    protected final PsiElement anchor;
    protected final String serviceName;
    protected final ServiceInfo serviceInfo;

    protected PyCustomType(PsiElement anchor, String serviceName, ServiceInfo serviceInfo) {
        this.anchor = anchor;
        this.serviceName = serviceName;
        this.serviceInfo = serviceInfo;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isBuiltin() {
        return false;
    }

    @Override
    public @Nullable List<? extends RatedResolveResult> resolveMember(
            @NotNull String name,
            @Nullable PyExpression location,
            @NotNull AccessDirection direction,
            @NotNull PyResolveContext resolveContext,
            boolean inherited) {
        ServiceMethod method = serviceInfo.findMethod(name);
        if (method == null) return null;

        return Collections.singletonList(
            new RatedResolveResult(RatedResolveResult.RATE_NORMAL,
                createMethodElement(method)));
    }

    private PsiElement createMethodElement(ServiceMethod method) {
        // Create a fake Python method element for inspection
        StringBuilder methodText = new StringBuilder();
        methodText.append("def ").append(method.name).append("(self");
        
        for (ParameterInfo param : method.parameters) {
            methodText.append(", ").append(param.name);
            if (param.type != null) {
                methodText.append(": ").append(param.type);
            }
            if (param.defaultValue != null) {
                methodText.append(" = ").append(param.defaultValue);
            }
        }
        
        methodText.append(") -> ").append(method.returnType).append(":\n    pass");

        return PyElementGenerator.getInstance(anchor.getProject())
            .createFromText(LanguageLevel.getDefault(), PyFunction.class, methodText.toString());
    }

    @Override
    public String getName() {
        return "RpcProxy(" + serviceName + ")";
    }
}