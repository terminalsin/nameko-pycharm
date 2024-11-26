package com.namecheap.nameko.model;

import com.intellij.psi.PsiElement;
import com.jetbrains.python.psi.PyFunction;

import java.util.List;

public class ServiceMethod {
    public final String name;
    public final String returnType;
    public final List<ParameterInfo> parameters;
    public final String documentation;
    public final PyFunction element;

    public ServiceMethod(String name, String returnType, List<ParameterInfo> parameters, String documentation, PyFunction element) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
        this.documentation = documentation;
        this.element = element;
    }

    public String getName() {
        return name;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<ParameterInfo> getParameters() {
        return parameters;
    }

    public String getDocumentation() {
        return documentation;
    }

    public PyFunction getElement() {
        return element;
    }
}