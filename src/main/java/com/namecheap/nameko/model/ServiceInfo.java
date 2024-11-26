package com.namecheap.nameko.model;

import com.jetbrains.python.psi.PyClass;

import java.util.List;

public class ServiceInfo {
    private final String serviceName;
    public final List<ServiceMethod> methods;
    private final PyClass serviceClass;

    public ServiceInfo(String serviceName, List<ServiceMethod> methods, PyClass serviceClass) {
        this.serviceName = serviceName;
        this.methods = methods;
        this.serviceClass = serviceClass;
    }

    /**
     * Finds a method by its name
     * @param name The name of the method to find
     * @return The ServiceMethod if found, null otherwise
     */
    public ServiceMethod findMethod(String name) {
        if (name == null || methods == null) {
            return null;
        }
        
        return methods.stream()
            .filter(method -> name.equals(method.getName()))
            .findFirst()
            .orElse(null);
    }

    public boolean hasMethod(String name) {
        return findMethod(name) != null;
    }

    public String getServiceName() {
        return serviceName;
    }

    public PyClass getServiceClass() {
        return serviceClass;
    }

    public List<ServiceMethod> getMethods() {
        return methods;
    }

    @Override
    public String toString() {
        return "ServiceInfo{" +
                "serviceName='" + serviceName + '\'' +
                ", methods=" + methods.stream().map(ServiceMethod::getName).toList() +
                '}';
    }
}