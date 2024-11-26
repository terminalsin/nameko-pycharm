package com.namecheap.nameko.listener;

import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.Project;
import com.namecheap.nameko.cache.ServiceCache;
import com.namecheap.nameko.util.FileCollector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PythonFileListener implements BulkFileListener {
    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        for (VFileEvent event : events) {
            VirtualFile file = event.getFile();
            if (file != null && "py".equals(file.getExtension())) {
                for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                    ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);

                    if (FileCollector.isFileGoodForIndexing(fileIndex, file)) {
                        ServiceCache.refreshProjectCache(project, file);
                    }
                }
            }
        }
    }
} 