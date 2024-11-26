package com.namecheap.nameko.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.jetbrains.python.psi.PyFile;
import com.namecheap.nameko.model.ParameterInfo;
import com.namecheap.nameko.model.ServiceInfo;
import com.namecheap.nameko.scanner.ServiceScanner;
import com.namecheap.nameko.util.FileCollector;
import com.namecheap.nameko.util.PluginLogger;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ShowServicesAction extends AnAction implements DumbAware {

    public ShowServicesAction() {
        super("Show RPC Services", "Display detected RPC services", null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Scanning RPC Services", true) {
            private final Map<String, ServiceInfo> services = new HashMap<>();
            private final AtomicInteger processedFiles = new AtomicInteger(0);
            private int totalFiles = 0;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    indicator.setIndeterminate(true);
                    indicator.setText("Finding Python files...");

                    // Get all valid Python files
                    Collection<VirtualFile> pythonFiles = FileCollector.findPythonFiles(project);

                    totalFiles = pythonFiles.size();
                    PluginLogger.info("Found " + totalFiles + " Python files to scan");

                    indicator.setIndeterminate(false);
                    indicator.setText("Preparing to scan files...");

                    // Process files in chunks
                    List<VirtualFile> filesList = new ArrayList<>(pythonFiles);
                    int chunkSize = 50;

                    for (int i = 0; i < filesList.size(); i += chunkSize) {
                        if (indicator.isCanceled()) {
                            return;
                        }

                        int endIndex = Math.min(i + chunkSize, filesList.size());
                        List<VirtualFile> chunk = filesList.subList(i, endIndex);

                        processFileChunk(chunk, indicator);

                        // Update progress
                        int processed = processedFiles.get();
                        double progress = (double) processed / totalFiles;
                        indicator.setFraction(progress);
                        indicator.setText(String.format("Scanning Python files: %d/%d",
                                processed, totalFiles));
                    }

                } catch (Exception ex) {
                    PluginLogger.error("Error scanning services", ex);
                }
            }

            private void processFileChunk(List<VirtualFile> files, ProgressIndicator indicator) {
                ReadAction.run(() -> {
                    for (VirtualFile file : files) {
                        if (indicator.isCanceled()) return;

                        try {
                            scanFile(file);
                        } catch (Exception e) {
                            PluginLogger.warn("Error processing file: " + file.getPath());
                        } finally {
                            processedFiles.incrementAndGet();
                        }
                    }
                });
            }

            private void scanFile(VirtualFile file) {
                PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                if (!(psiFile instanceof PyFile)) return;

                ServiceScanner scanner = new ServiceScanner();
                Map<String, ServiceInfo> fileServices = scanner.scanFile((PyFile) psiFile, new ProgressIndicatorBase());

                if (!fileServices.isEmpty()) {
                    synchronized (services) {
                        services.putAll(fileServices);
                    }
                }
            }

            @Override
            public void onSuccess() {
                showResults();
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                showError(error);
            }

            private void showResults() {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (services.isEmpty()) {
                        showNoServicesPopup();
                        return;
                    }

                    List<String> serviceDetails = new ArrayList<>();
                    serviceDetails.add(String.format("Found %d services in %d files:",
                            services.size(), totalFiles));
                    serviceDetails.add(""); // Empty line for spacing

                    services.forEach((name, info) -> {
                        serviceDetails.add(name + " (" + info.getMethods().size() + " methods)");
                        info.getMethods().forEach(method ->
                                serviceDetails.add("  → " + method.getName() + "(" +
                                        formatParameters(method.getParameters()) + ") -> " + 
                                        method.getReturnType())
                        );
                        serviceDetails.add(""); // Empty line between services
                    });

                    showPopup(serviceDetails);
                });
            }

            private String formatParameters(List<ParameterInfo> parameters) {
                return parameters.stream()
                    .map(param -> {
                        StringBuilder sb = new StringBuilder(param.getName());
                        if (param.getType() != null) {
                            sb.append(": ").append(param.getType());
                        }
                        if (param.getDefaultValue() != null) {
                            sb.append(" = ").append(param.getDefaultValue());
                        }
                        return sb.toString();
                    })
                    .collect(Collectors.joining(", "));
            }

            private void showPopup(List<String> details) {
                JBList<String> list = new JBList<>(details);
                list.setCellRenderer(new ServiceListCellRenderer());
                list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

                JBScrollPane scrollPane = new JBScrollPane(list);
                scrollPane.setPreferredSize(new Dimension(800, 600));

                JBPopupFactory.getInstance()
                        .createComponentPopupBuilder(scrollPane, list)
                        .setTitle("RPC Services")
                        .setMovable(true)
                        .setResizable(true)
                        .createPopup()
                        .showCenteredInCurrentWindow(project);
            }

            private void showNoServicesPopup() {
                JPanel panel = new JPanel(new BorderLayout());
                JLabel label = new JLabel(String.format(
                        "No RPC services found in %d scanned files", totalFiles
                ));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                panel.add(label, BorderLayout.CENTER);
                panel.setPreferredSize(new Dimension(300, 100));

                JBPopupFactory.getInstance()
                        .createComponentPopupBuilder(panel, label)
                        .setTitle("RPC Services")
                        .createPopup()
                        .showCenteredInCurrentWindow(project);
            }

            private void showError(Throwable error) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    JPanel panel = new JPanel(new BorderLayout());
                    JTextArea textArea = new JTextArea(error.getMessage());
                    textArea.setEditable(false);
                    textArea.setLineWrap(true);
                    textArea.setWrapStyleWord(true);
                    panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
                    panel.setPreferredSize(new Dimension(400, 200));

                    JBPopupFactory.getInstance()
                            .createComponentPopupBuilder(panel, textArea)
                            .setTitle("Error Scanning Services")
                            .createPopup()
                            .showCenteredInCurrentWindow(project);
                });
            }
        });
    }

    private static class ServiceListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof String) {
                String text = (String) value;
                if (text.startsWith("  →")) {
                    setFont(getFont().deriveFont(Font.PLAIN));
                    setForeground(isSelected ? list.getSelectionForeground() : Color.GRAY);
                } else if (text.contains(" services in ")) {
                    setFont(getFont().deriveFont(Font.BOLD, 14f));
                } else if (text.endsWith(" methods)")) {
                    setFont(getFont().deriveFont(Font.BOLD));
                    setForeground(isSelected ? list.getSelectionForeground() : new Color(0, 100, 0));
                }
            }

            return c;
        }
    }
}