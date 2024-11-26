package com.namecheap.nameko.debug;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.namecheap.nameko.util.PluginLogger;

public class DebugAction extends AnAction implements DumbAware {

    public DebugAction() {
        super("RPC Plugin Debug Info", 
              "Show debug information for RPC Plugin", 
              null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        // Log various debug information
        PluginLogger.debug("=== RPC Plugin Debug Info ===");
        PluginLogger.debug("Project: " + project.getName());
        PluginLogger.debug("Base Path: " + project.getBasePath());
        
        // Log memory usage
        Runtime runtime = Runtime.getRuntime();
        long memory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        PluginLogger.debug("Memory Usage: " + memory + "MB");
        
        // Show notification
        PluginLogger.showNotification(
            project,
            "Debug Info Written",
            "Debug information has been written to the event log",
            NotificationType.INFORMATION
        );
    }
}