package com.namecheap.nameko.completion;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.application.WriteAction;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.namecheap.nameko.cache.ServiceCache;
import com.jetbrains.python.psi.PyReferenceExpression;
import com.namecheap.nameko.util.RpcProxyUtil;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class NamekoCompletionContributorTest extends BasePlatformTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Create the service definition first within a write command
        WriteAction.runAndWait(() -> {
            myFixture.configureByText("user_service.py",
                    "class UserService:\n" +
                            "    name = 'sq_user_service'\n" +
                            "    \n" +
                            "    @rpc" +
                            "    def get(self, user_id):\n" +
                            "        pass\n");
        });
    }

    public void testRpcProxyCompletion() {
        // Create the service files
        WriteAction.runAndWait(() -> {
            // Create user service with RPC method
            myFixture.configureByText("user_service.py",
                "class UserService:\n" +
                "    name = 'sq_user_service'\n" +
                "    @rpc\n" +
                "    def get(self, user_id):\n" +
                "        pass\n");

            // Create backup service that uses the user service
            myFixture.configureByText("backup_service.py",
                "class BackupService:\n" +
                "    sq_user_service = RpcProxy('sq_user_service')\n" +
                "    def test(self):\n" +
                "        self.sq_user_service.<caret>");
        });

        // Get the element at the dot position
        PsiElement element = myFixture.getFile().findElementAt(myFixture.getCaretOffset() - 1);
        assertNotNull("Element at dot should not be null", element);
        System.out.println("Found element " + element + " at cursor with parent " + element.getParent() + " of type " + element.getParent().getClass());

        // Get completion results and add debug info
        myFixture.completeBasic();

        LookupElement[] lookupElements = myFixture.getLookupElements();

        if (lookupElements == null) {
            System.out.println("No completion results found");
            // Debug the service cache
            System.out.println("Available services: " + ServiceCache.getServices(getProject(), 
                myFixture.getFile().getVirtualFile()));
            // Debug the PSI structure
            System.out.println("Parent element: " + element.getParent());
            fail("No completions available");
        }
        
        // Convert lookup elements to strings for easier assertion
        List<String> completions = Arrays.stream(lookupElements)
            .map(LookupElement::getLookupString)
            .collect(Collectors.toList());
        System.out.println("Available completions: " + completions);

        assertContainsElements(ServiceCache.getServices(getProject()).keySet(), "sq_user_service");
        // Assert the completions contain the expected method
        assertContainsElements(completions, "get");
    }
}