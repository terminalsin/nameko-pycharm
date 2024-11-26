package com.namecheap.nameko.cache;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.jetbrains.python.psi.PyFile;
import com.namecheap.nameko.model.ServiceInfo;
import com.namecheap.nameko.scanner.ServiceScanner;
import com.namecheap.nameko.util.FileCollector;
import com.namecheap.nameko.util.PluginLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceCache {
    private static final Map<String, CachedValue<Map<String, ServiceInfo>>> projectServicesCache = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, CachedValue<Map<String, ServiceInfo>>>> fileCache = new ConcurrentHashMap<>();
    private static final Map<String, BackgroundTaskManager> projectTaskManagers = new ConcurrentHashMap<>();

    public static void initializeProject(@NotNull Project project) {
        String projectKey = project.getName();
        projectTaskManagers.computeIfAbsent(projectKey, k -> new BackgroundTaskManager());
        
        // Start initial background load
        refreshProjectCache(project, null);
    }

    public static Map<String, ServiceInfo> getServices(@NotNull Project project, @Nullable VirtualFile excludeFile) {
        final long ms = System.currentTimeMillis();
        String projectKey = project.getName();
        CachedValue<Map<String, ServiceInfo>> cachedValue = projectServicesCache.get(projectKey);

        if (cachedValue == null) {
            // If cache is not initialized, do a blocking load
            System.out.println("Cache not initialized");
            return loadServicesBlocking(project, excludeFile);
        }
        final long ms2 = System.currentTimeMillis();


        return cachedValue.getValue();
    }

    private static Map<String, ServiceInfo> loadServicesBlocking(@NotNull Project project, @Nullable VirtualFile excludeFile) {
        return ProgressManager.getInstance().runProcess(() -> {
            System.out.println("Loading services blocking");
            String projectKey = project.getName();
            String excludePath = excludeFile != null ? excludeFile.getPath() : "";

            Collection<VirtualFile> pythonFiles = FileCollector.findPythonFiles(project);
            Map<String, ServiceInfo> allServices = new HashMap<>();
            ProgressIndicator progress = new ProgressIndicatorBase();
            progress.setText("Loading RPC Services");

            for (VirtualFile file : pythonFiles) {
                if (file.getPath().equals(excludePath)) {
                    continue;
                }
                progress.checkCanceled();
                updateProjectCache(project, file);
            }

            return projectServicesCache.get(projectKey).getValue();
        }, new ProgressIndicatorBase());
    }

    public static void refreshProjectCache(@NotNull Project project, @Nullable VirtualFile changedFile) {
        String projectKey = project.getName();
        BackgroundTaskManager taskManager = projectTaskManagers.get(projectKey);
        
        if (taskManager == null) {
            return;
        }

        taskManager.scheduleTask(() -> {
            ProgressManager.getInstance().run(new Task.Backgroundable(project, "Updating RPC Services Cache", false) {
                @Override
                public void run(@NotNull ProgressIndicator indicator) {
                    try {
                        indicator.setText("Updating RPC Services Cache");
                        updateProjectCache(project, changedFile);
                    } catch (Exception e) {
                        PluginLogger.error("Error updating service cache", e);
                    }
                }
            });
        });
    }

    private static void updateProjectCache(@NotNull Project project, @Nullable VirtualFile changedFile) {
        String projectKey = project.getName();
        
        projectServicesCache.put(projectKey, 
            CachedValuesManager.getManager(project).createCachedValue(() -> {
                Collection<VirtualFile> pythonFiles = FileCollector.findPythonFiles(project);
                Map<String, ServiceInfo> allServices = new HashMap<>();
                String excludePath = changedFile != null ? changedFile.getPath() : "";

                for (VirtualFile file : pythonFiles) {
                    if (file.getPath().equals(excludePath)) {
                        continue;
                    }
                    allServices.putAll(getServicesFromFile(project, file, new ProgressIndicatorBase()));
                }

                Object[] dependencies = pythonFiles.stream()
                    .filter(f -> !f.getPath().equals(excludePath))
                    .map(f -> PsiManager.getInstance(project).findFile(f))
                    .filter(f -> f != null)
                    .toArray();

                return CachedValueProvider.Result.create(allServices, new Object[]{});
            })
        );
    }

    private static Map<String, ServiceInfo> getServicesFromFile(@NotNull Project project, 
                                                              @NotNull VirtualFile file,
                                                              @NotNull ProgressIndicator progress) {
        String projectKey = project.getName();
        String fileKey = file.getPath();

        return fileCache.computeIfAbsent(projectKey, k -> new ConcurrentHashMap<>())
            .computeIfAbsent(fileKey, k -> CachedValuesManager.getManager(project).createCachedValue(
                () -> {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                    if (!(psiFile instanceof PyFile)) {
                        return CachedValueProvider.Result.create(
                            new HashMap<>(),
                            psiFile
                        );
                    }

                    ServiceScanner scanner = new ServiceScanner();
                    Map<String, ServiceInfo> services = scanner.scanFile((PyFile) psiFile, progress);
                    
                    return CachedValueProvider.Result.create(
                        services,
                        psiFile
                    );
                }
            )).getValue();
    }

    public static void refreshFile(@NotNull Project project, @NotNull VirtualFile file) {
        String projectKey = project.getName();
        Map<String, CachedValue<Map<String, ServiceInfo>>> projectCache = fileCache.get(projectKey);
        if (projectCache != null) {
            projectCache.remove(file.getPath());
        }
    }

    public static void cleanupProject(@NotNull Project project) {
        String projectKey = project.getName();
        projectServicesCache.remove(projectKey);
        fileCache.remove(projectKey);
        BackgroundTaskManager taskManager = projectTaskManagers.remove(projectKey);
        if (taskManager != null) {
            taskManager.cancelScheduledTask();
        }
    }
}