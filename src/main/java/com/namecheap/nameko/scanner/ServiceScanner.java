package com.namecheap.nameko.scanner;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.*;
import com.namecheap.nameko.model.ParameterInfo;
import com.namecheap.nameko.model.ServiceInfo;
import com.namecheap.nameko.model.ServiceMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ServiceScanner {
    private static final Logger LOG = Logger.getInstance(ServiceScanner.class);

    @NotNull
    public Map<String, ServiceInfo> scanFile(@NotNull PyFile pyFile, @NotNull ProgressIndicator progress) {
        progress.setText2("Scanning " + pyFile.getName());

        Map<String, ServiceInfo> services = new HashMap<>();
        Collection<PyClass> classes = PsiTreeUtil.findChildrenOfType(pyFile, PyClass.class);
        
        for (PyClass pyClass : classes) {
            progress.checkCanceled(); // Allow cancellation
            String serviceName = findServiceName(pyClass);
            if (serviceName == null) continue;

            progress.setText2("Found service: " + serviceName + " in " + pyFile.getName());
            List<ServiceMethod> methods = findServiceMethods(pyClass);
            if (!methods.isEmpty()) {
                services.put(serviceName, new ServiceInfo(serviceName, methods, pyClass));
            }
        }
        
        return services;
    }

    @Nullable
    private String findServiceName(@NotNull PyClass pyClass) {
        // Try class attribute 'name'
        for (PyTargetExpression expr : pyClass.getClassAttributes()) {
            if ("name".equals(expr.getName())) {
                PyExpression value = expr.findAssignedValue();
                if (value instanceof PyStringLiteralExpression) {
                    return ((PyStringLiteralExpression) value).getStringValue();
                }
            }
        }
        return null;
    }

    @NotNull
    private List<ServiceMethod> findServiceMethods(@NotNull PyClass pyClass) {
        return Arrays.stream(pyClass.getMethods())
                .filter(method -> !method.getName().startsWith("_"))
                .filter(this::hasRpcDecorator)
                .map(this::createServiceMethod)
                .collect(Collectors.toList());
    }

    private boolean hasRpcDecorator(@NotNull PyFunction function) {
        PyDecoratorList decoratorList = function.getDecoratorList();
        if (decoratorList == null) return false;

        return Arrays.stream(decoratorList.getDecorators())
                .map(PyDecorator::getName)
                .filter(Objects::nonNull)
                .anyMatch(name -> name.equals("rpc") || 
                                name.equals("expose_rpc") || 
                                name.contains("method"));
    }

    @NotNull
    private ServiceMethod createServiceMethod(@NotNull PyFunction function) {
        return new ServiceMethod(
                function.getName(),
                getReturnType(function),
                getParameters(function),
                function.getDocStringValue(),
                function
        );
    }

    @NotNull
    private List<ParameterInfo> getParameters(@NotNull PyFunction function) {
        return Arrays.stream(function.getParameterList().getParameters())
                .skip(1) // Skip 'self'
                .map(this::createParameterInfo)
                .collect(Collectors.toList());
    }

    @NotNull
    private ParameterInfo createParameterInfo(@NotNull PyParameter param) {
        if (!(param instanceof PyNamedParameter)) {
            return new ParameterInfo(param.getName(), "Any", null);
        }

        PyNamedParameter namedParam = (PyNamedParameter) param;
        String type = Optional.ofNullable(namedParam.getAnnotation())
                .map(PyAnnotation::getText)
                .map(text -> text.replaceFirst("^\\s*:\\s*", ""))
                .orElse("Any");
        String defaultValue = Optional.ofNullable(namedParam.getDefaultValue())
                .map(PyExpression::getText)
                .map(text -> text.replaceFirst("^\\s*=\\s*", ""))
                .orElse(null);

        return new ParameterInfo(namedParam.getName(), type, defaultValue);
    }

    @NotNull
    private String getReturnType(@NotNull PyFunction function) {
        return Optional.ofNullable(function.getAnnotation())
                .map(PyAnnotation::getText)
                .map(text -> text.replaceFirst("^\\s*->\\s*", ""))
                .orElse("Any");
    }
} 