package com.github.cfogrady.dim.modifier.data;

import com.github.cfogrady.dim.modifier.controllers.DimIOController;
import com.github.cfogrady.dim.modifier.data.card.CardData;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
@RequiredArgsConstructor
public class AutoSaveManager {
    private static final String AUTO_SAVE_FILE_NAME = "autosave.bin";
    private static final long AUTO_SAVE_INTERVAL = 5 * 60 * 1000; // 5 minutes

    private final AppState appState;
    private final DimIOController dimIOController;
    private final Timer timer = new Timer(true);

    public void startAutoSave() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> performAutoSave());
            }
        }, AUTO_SAVE_INTERVAL, AUTO_SAVE_INTERVAL);
    }

    public void stopAutoSave() {
        timer.cancel();
        deleteAutoSave();
    }

    private void performAutoSave() {
        if (appState.getCardData() != null) {
            try {
                File autoSaveFile = new File(System.getProperty("java.io.tmpdir"), AUTO_SAVE_FILE_NAME);
                dimIOController.saveDimToFile(autoSaveFile);
                log.info("Auto-save completed to {}", autoSaveFile.getAbsolutePath());
            } catch (Exception e) {
                log.error("Auto-save failed", e);
            }
        }
    }

    public void checkAutoSave() {
        File autoSaveFile = new File(System.getProperty("java.io.tmpdir"), AUTO_SAVE_FILE_NAME);
        if (autoSaveFile.exists()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Crash Recovery");
            alert.setHeaderText("Unsaved work found");
            alert.setContentText(
                    "It seems the application closed unexpectedly. Do you want to restore your unsaved work?");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                dimIOController.openDim(autoSaveFile, () -> {
                    log.info("Restored from auto-save");
                    // Optionally set file path to null so user is forced to Save As
                    appState.setLastOpenedFilePath(null);
                });
            } else {
                deleteAutoSave();
            }
        }
    }

    public void deleteAutoSave() {
        File autoSaveFile = new File(System.getProperty("java.io.tmpdir"), AUTO_SAVE_FILE_NAME);
        if (autoSaveFile.exists()) {
            autoSaveFile.delete();
        }
    }
}
