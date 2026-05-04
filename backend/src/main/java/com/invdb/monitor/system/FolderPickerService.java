package com.invdb.monitor.system;

import java.awt.HeadlessException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FolderPickerService {

    private static final Logger log = LoggerFactory.getLogger(FolderPickerService.class);

    public String pickFolder() {
        AtomicReference<String> selectedPath = new AtomicReference<>();

        try {
            SwingUtilities.invokeAndWait(() -> {
                JFrame parent = new JFrame();
                parent.setAlwaysOnTop(true);
                parent.setUndecorated(true);
                parent.setSize(0, 0);
                parent.setLocationRelativeTo(null);

                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                chooser.setMultiSelectionEnabled(false);
                chooser.setDialogTitle("Select Folder to Monitor");

                int result = chooser.showOpenDialog(parent);
                if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                    selectedPath.set(chooser.getSelectedFile().getAbsolutePath());
                }
                parent.dispose();
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Folder picker was interrupted", e);
            return null;
        } catch (InvocationTargetException | HeadlessException | SecurityException e) {
            log.error("Failed to open folder picker", e);
            return null;
        }

        return selectedPath.get();
    }
}
