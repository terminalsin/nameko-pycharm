package com.namecheap.nameko.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.python.PyTokenTypes;
import com.jetbrains.python.psi.*;
import com.namecheap.nameko.cache.ServiceCache;
import com.namecheap.nameko.model.ServiceInfo;
import com.namecheap.nameko.model.ServiceMethod;
import com.namecheap.nameko.scanner.ServiceScanner;
import com.namecheap.nameko.util.RpcProxyUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.stream.Collectors;

public class NamekoCompletionContributor extends CompletionContributor {

    public NamekoCompletionContributor() {
        extend(null,
                PlatformPatterns.psiElement()
                    .withParent(PyReferenceExpression.class)
                    .afterLeaf(PlatformPatterns.psiElement(PyTokenTypes.DOT))
                    .inside(PyFunction.class),
                new CompletionProvider<>() {
                    @Override
                    protected void addCompletions(@NotNull CompletionParameters parameters,
                                                @NotNull ProcessingContext context,
                                                @NotNull CompletionResultSet result) {
                        PyReferenceExpression reference = PsiTreeUtil.getParentOfType(
                            parameters.getPosition(),
                            PyReferenceExpression.class,
                            false
                        );
                        
                        if (reference == null) {
                            //System.out.println("No reference expression found");
                            return;
                        }
                        //System.out.println("Found reference: " + reference.getText());

                        // Get the qualifier (the part before the dot)
                        PyExpression qualifier = reference.getQualifier();
                        if (qualifier == null || !RpcProxyUtil.isRpcProxyReference(qualifier)) {
                            //System.out.println("Not an RPC proxy reference");
                            return;
                        }

                        // Get service name from RpcProxy call
                        String serviceName = RpcProxyUtil.getServiceNameFromProxy(qualifier);
                        if (serviceName == null) {
                            return;
                        }

                        //System.out.println("====> Service name: " + serviceName);
                        // Add methods from service, excluding current file
                        Project project = reference.getProject();
                        VirtualFile currentFile = parameters.getOriginalFile().getVirtualFile();
                        Map<String, ServiceInfo> services = ServiceCache.getServices(project, currentFile);
                        ServiceInfo service = services.get(serviceName);

                        //System.out.println("Service: " + service);
                        result.startBatch();
                        if (service != null) {
                            for (ServiceMethod method : service.getMethods()) {
                                LookupElementBuilder element = createMethodLookupElement(method);
                                //System.out.println("Adding method: " + method.getName());
                                result.addElement(element);
                            }
                        }
                        result.endBatch();

                        //System.out.println("Completion done, result: " + result);

                    }
                });
    }

    private LookupElementBuilder createMethodLookupElement(ServiceMethod method) {
        return LookupElementBuilder.create(method.getName())
                .withTypeText(method.getReturnType())
                .withTailText(buildParameterPresentation(method))
                .withIcon(AllIcons.Nodes.Method);
    }

    private String buildParameterPresentation(ServiceMethod method) {
        return "(" + method.getParameters().stream()
                .map(p -> p.getName() + ": " + p.getType())
                .collect(Collectors.joining(", ")) + ")";
    }
} 