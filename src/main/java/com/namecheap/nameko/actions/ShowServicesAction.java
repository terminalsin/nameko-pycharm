package com.namecheap.nameko.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.namecheap.nameko.SimpleRpcPlugin;
import com.namecheap.nameko.util.PluginLogger;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShowServicesAction extends AnAction implements DumbAware {
    
    public ShowServicesAction() {
        super("Show RPC Services", "Display detected RPC services", null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        PluginLogger.info("Showing services popup");
        
        // Get services from our plugin
        Map<String, SimpleRpcPlugin.ServiceInfo> services = SimpleRpcPlugin.getProjectServices(project);
        
        if (services.isEmpty()) {
            PluginLogger.info("No services found");
            showNoServicesPopup(project);
            return;
        }

        // Create list of service info
        List<String> serviceDetails = new ArrayList<>();
        for (Map.Entry<String, SimpleRpcPlugin.ServiceInfo> entry : services.entrySet()) {
            String serviceName = entry.getKey();
            SimpleRpcPlugin.ServiceInfo info = entry.getValue();
            
            StringBuilder details = new StringBuilder();
            details.append(serviceName).append(" (").append(info.methods.size()).append(" methods)");
            serviceDetails.add(details.toString());
            
            // Add method details
            for (SimpleRpcPlugin.MethodInfo method : info.methods) {
                details = new StringBuilder("  → ");
                details.append(method.name).append("(")
                       .append(method.parameters).append(")")
                       .append(" -> ").append(method.returnType);
                serviceDetails.add(details.toString());
            }
        }

        // Create and configure the list
        JBList<String> list = new JBList<>(serviceDetails);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
                
                if (value.toString().startsWith("  →")) {
                    setFont(getFont().deriveFont(Font.PLAIN));
                    setIcon(null);
                } else {
                    setFont(getFont().deriveFont(Font.BOLD));
                    // You could add an icon here if you have one
                }
                
                return c;
            }
        });

        // Create scroll pane
        JBScrollPane scrollPane = new JBScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        // Show popup
        PluginLogger.info("Showing popup with " + services.size() + " services");
        JBPopupFactory.getInstance()
            .createComponentPopupBuilder(scrollPane, list)
            .setTitle("Detected RPC Services")
            .setMovable(true)
            .setResizable(true)
            .setRequestFocus(true)
            .createPopup()
            .showCenteredInCurrentWindow(project);
    }

    private void showNoServicesPopup(Project project) {
        JLabel label = new JLabel("No RPC services detected", SwingConstants.CENTER);
        label.setPreferredSize(new Dimension(200, 100));

        JBPopupFactory.getInstance()
            .createComponentPopupBuilder(label, label)
            .setTitle("RPC Services")
            .createPopup()
            .showCenteredInCurrentWindow(project);
    }
}