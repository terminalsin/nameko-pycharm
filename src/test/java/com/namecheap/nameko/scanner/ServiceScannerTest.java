package com.namecheap.nameko.scanner;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.testFramework.IndexingTestUtil;
import com.jetbrains.python.psi.PyFile;
import com.namecheap.nameko.model.ServiceInfo;
import com.namecheap.nameko.model.ServiceMethod;

import java.util.Map;

@org.junit.runner.RunWith(org.junit.runners.JUnit4.class)
public class ServiceScannerTest extends BasePlatformTestCase {
    private ServiceScanner scanner;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        scanner = new ServiceScanner();
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            // test specific tear down calls
        } finally {
            super.tearDown();
        }
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/testData";
    }

    @org.junit.Test
    public void testBasicServiceScanning() {
        String content = """
            class TestService:
                name = 'test_service'
                
                @rpc
                def hello(self, name: str) -> str:
                    '''Says hello'''
                    return f"Hello {name}"
                    
                @rpc
                def add(self, a: int, b: int = 0) -> int:
                    '''Adds two numbers'''
                    return a + b
            """;

        PyFile file = (PyFile) myFixture.configureByText("test.py", content);
        
        // Wait for indexes to be ready
        IndexingTestUtil.waitUntilIndexesAreReady(getProject());
        
        Map<String, ServiceInfo> services = scanner.scanFile(file);

        assertEquals(1, services.size());
        ServiceInfo service = services.get("test_service");
        assertNotNull(service);
        assertEquals(2, service.getMethods().size());

        ServiceMethod helloMethod = service.findMethod("hello");
        assertNotNull(helloMethod);
        assertEquals("str", helloMethod.getReturnType());
        assertEquals(1, helloMethod.getParameters().size());
        assertEquals("Says hello", helloMethod.getDocumentation());

        ServiceMethod addMethod = service.findMethod("add");
        assertNotNull(addMethod);
        assertEquals("int", addMethod.getReturnType());
        assertEquals(2, addMethod.getParameters().size());
        assertEquals("0", addMethod.getParameters().get(1).getDefaultValue());
    }

    @org.junit.Test
    public void testIgnoresNonServices() {
        String content = """
            class RegularClass:
                def method(self):
                    pass
            """;

        PyFile file = (PyFile) myFixture.configureByText("test.py", content);
        Map<String, ServiceInfo> services = scanner.scanFile(file);

        assertTrue(services.isEmpty());
    }

    @org.junit.Test
    public void testServiceWithoutName() {
        String content = """
            class TestService:
                @rpc
                def hello(self) -> str:
                    return "Hello"
            """;

        PyFile file = (PyFile) myFixture.configureByText("test.py", content);
        Map<String, ServiceInfo> services = scanner.scanFile(file);

        assertTrue(services.isEmpty());
    }

    @org.junit.Test
    public void testServiceWithComplexTypes() {
        String content = """
            from typing import List, Dict, Optional
            
            class ComplexService:
                name = 'complex_service'
                
                @rpc
                def process_list(self, items: List[str]) -> List[int]:
                    '''Process a list of strings'''
                    return [len(item) for item in items]
                
                @rpc
                def process_dict(self, data: Dict[str, int]) -> Dict[str, str]:
                    '''Process a dictionary'''
                    return {}
                    
                @rpc
                def optional_param(self, value: Optional[str] = None) -> bool:
                    '''Handle optional parameter'''
                    return value is not None
            """;

        PyFile file = (PyFile) myFixture.configureByText("test.py", content);
        Map<String, ServiceInfo> services = scanner.scanFile(file);

        ServiceInfo service = services.get("complex_service");
        assertNotNull(service);
        assertEquals(3, service.getMethods().size());

        ServiceMethod listMethod = service.findMethod("process_list");
        assertNotNull(listMethod);
        assertEquals("List[int]", listMethod.getReturnType());
        assertEquals("List[str]", listMethod.getParameters().get(0).getType());

        ServiceMethod dictMethod = service.findMethod("process_dict");
        assertNotNull(dictMethod);
        assertEquals("Dict[str, str]", dictMethod.getReturnType());
        assertEquals("Dict[str, int]", dictMethod.getParameters().get(0).getType());

        ServiceMethod optionalMethod = service.findMethod("optional_param");
        assertNotNull(optionalMethod);
        assertEquals("bool", optionalMethod.getReturnType());
        assertEquals("None", optionalMethod.getParameters().get(0).getDefaultValue());
    }

    @org.junit.Test
    public void testMultipleServicesInFile() {
        String content = """
            class FirstService:
                name = 'first_service'
                
                @rpc
                def method1(self) -> None:
                    pass
                    
            class SecondService:
                name = 'second_service'
                
                @rpc
                def method2(self) -> str:
                    return "test"
            """;

        PyFile file = (PyFile) myFixture.configureByText("test.py", content);
        Map<String, ServiceInfo> services = scanner.scanFile(file);

        assertEquals(2, services.size());
        assertNotNull(services.get("first_service"));
        assertNotNull(services.get("second_service"));
    }

    @org.junit.Test
    public void testMethodWithoutTypeHints() {
        String content = """
            class SimpleService:
                name = 'simple_service'
                
                @rpc
                def no_types(self, param):
                    '''Method without type hints'''
                    return param
            """;

        PyFile file = (PyFile) myFixture.configureByText("test.py", content);
        Map<String, ServiceInfo> services = scanner.scanFile(file);

        ServiceInfo service = services.get("simple_service");
        assertNotNull(service);

        ServiceMethod method = service.findMethod("no_types");
        assertNotNull(method);
        assertEquals("Any", method.getReturnType());
        assertEquals("Any", method.getParameters().get(0).getType());
    }

    @org.junit.Test
    public void testNonRpcMethods() {
        String content = """
            class MixedService:
                name = 'mixed_service'
                
                @rpc
                def rpc_method(self) -> str:
                    return "RPC"
                    
                def regular_method(self) -> str:
                    return "Regular"
                    
                @property
                def some_property(self) -> str:
                    return "Property"
            """;

        PyFile file = (PyFile) myFixture.configureByText("test.py", content);
        Map<String, ServiceInfo> services = scanner.scanFile(file);

        ServiceInfo service = services.get("mixed_service");
        assertNotNull(service);
        assertEquals(1, service.getMethods().size());
        assertNotNull(service.findMethod("rpc_method"));
        assertNull(service.findMethod("regular_method"));
        assertNull(service.findMethod("some_property"));
    }
} 