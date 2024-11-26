package com.namecheap.nameko.util;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileCollector {
    
    @NotNull
    public static Collection<VirtualFile> findPythonFiles(@NotNull Project project) {
        return ReadAction.compute(() -> {
            ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
            
            // Get all Python files in project scope
            Collection<VirtualFile> allPythonFiles = FilenameIndex.getAllFilesByExt(
                project, "py", GlobalSearchScope.projectScope(project)
            );
            
            // Filter files that are:
            // 1. In source roots
            // 2. Not in excluded folders
            // 3. Not in test folders (unless configured to include them)
            return allPythonFiles.stream()
                .filter(file -> fileIndex.isInContent(file))  // Excludes files in .git, build, etc.
                .filter(file -> !fileIndex.isExcluded(file))  // Excludes explicitly excluded directories
                .filter(file -> {
                    Module module = fileIndex.getModuleForFile(file);
                    if (module == null) return false;
                    
                    // Check if file is in source roots
                    ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
                    VirtualFile[] sourceRoots = rootManager.getSourceRoots(false); // false = non-test sources
                    
                    for (VirtualFile root : sourceRoots) {
                        if (file.getPath().startsWith(root.getPath())) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toSet());
        });
    }
    
    @NotNull
    public static Set<String> getExcludedPaths(@NotNull Project project) {
        Set<String> excludedPaths = new HashSet<>();
        
        // Add module-specific excluded paths
        ModuleManager moduleManager = ModuleManager.getInstance(project);
        for (Module module : moduleManager.getModules()) {
            ModuleRootManager rootManager = ModuleRootManager.getInstance(module);
            excludedPaths.addAll(Stream.of(rootManager.getExcludeRoots())
                .map(VirtualFile::getPath)
                .collect(Collectors.toSet()));
        }
        
        // Add common Python paths to exclude
        excludedPaths.addAll(getPythonExcludePaths());
        
        return excludedPaths;
    }
    
    @NotNull
    private static Set<String> getPythonExcludePaths() {
        Set<String> paths = new HashSet<>();
        paths.add(".git");
        paths.add(".hg");
        paths.add(".svn");
        paths.add(".tox");
        paths.add(".venv");
        paths.add("venv");
        paths.add("env");
        paths.add("__pycache__");
        paths.add(".pytest_cache");
        paths.add(".mypy_cache");
        paths.add(".coverage");
        paths.add("build");
        paths.add("dist");
        paths.add(".eggs");
        paths.add("*.egg-info");
        return paths;
    }
}