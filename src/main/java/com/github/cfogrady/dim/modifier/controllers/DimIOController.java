package com.github.cfogrady.dim.modifier.controllers;

import com.github.cfogrady.dim.modifier.data.AppState;
import com.github.cfogrady.dim.modifier.data.card.CardData;
import com.github.cfogrady.dim.modifier.data.card.CardDataIO;
import com.github.cfogrady.dim.modifier.data.card.CardSprites;
import com.github.cfogrady.dim.modifier.data.card.MetaData;
import com.github.cfogrady.dim.modifier.utils.DimIdReader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.lang.reflect.Field;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class DimIOController {
    public static final String BULLET = "\u2022";
    public static final String ERROR_SEPARATOR = System.lineSeparator() + BULLET + " ";

    private final Stage stage;
    private final CardDataIO cardDataIO;
    private final AppState appState;

    public void openDim(Runnable onCompletion) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select DIM File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("DIM Files", "*.bin"));

        // Definir o diretório inicial se disponível
        if (appState.getLastOpenedFilePath() != null) {
            File parentDir = appState.getLastOpenedFilePath().getParentFile();
            if (parentDir != null && parentDir.exists()) {
                fileChooser.setInitialDirectory(parentDir);
            }
        }

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try (InputStream fileInputStream = new FileInputStream(file)) {
                CardData<?, ?, ?> cardData = cardDataIO.readFromStream(fileInputStream);
                appState.setCardData(cardData);
                appState.setLastOpenedFilePath(file);
                if (onCompletion != null) {
                    onCompletion.run();
                }
            } catch (FileNotFoundException e) {
                log.error("Couldn't find selected file.", e);
            } catch (IOException e) {
                log.error("Couldn't close file???", e);
            }
        }
    }

    public void openDim(File file, Runnable onCompletion) {
        if (file != null) {
            try (InputStream fileInputStream = new FileInputStream(file)) {
                CardData<?, ?, ?> cardData = cardDataIO.readFromStream(fileInputStream);
                appState.setCardData(cardData);
                appState.setLastOpenedFilePath(file);
                if (onCompletion != null) {
                    onCompletion.run();
                }
            } catch (FileNotFoundException e) {
                log.error("Couldn't find selected file.", e);
                Alert alert = new Alert(Alert.AlertType.ERROR, "File not found: " + file.getAbsolutePath());
                alert.show();
            } catch (IOException e) {
                log.error("Error reading file", e);
                Alert alert = new Alert(Alert.AlertType.ERROR, "Error reading file: " + e.getMessage());
                alert.show();
            }
        }
    }

    public void openFile(File file) {
        try {
            CardData<?, ?, ?> cardData = cardDataIO.readFromFile(file);
            appState.setCardData(cardData);
            appState.setLastOpenedFilePath(file);
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Failed to load DIM");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    public void saveDim() {
        List<String> errors = appState.getCardData().checkForErrors();
        if (!errors.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.NONE,
                    "Cannot save. Errors in card data:" + ERROR_SEPARATOR + String.join(ERROR_SEPARATOR, errors));
            alert.getButtonTypes().add(ButtonType.OK);
            alert.show();
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save DIM File As...");

        // Definir o diretório inicial se disponível
        if (appState.getLastOpenedFilePath() != null) {
            File parentDir = appState.getLastOpenedFilePath().getParentFile();
            if (parentDir != null && parentDir.exists()) {
                fileChooser.setInitialDirectory(parentDir);
            }
            fileChooser.setInitialFileName(appState.getLastOpenedFilePath().getName());
        }

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            saveDimToFile(file);
            appState.setLastOpenedFilePath(file);
        }
    }

    public void saveDimToFile(File file) {
        List<String> errors = appState.getCardData().checkForErrors();
        if (!errors.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.NONE,
                    "Cannot save. Errors in card data:" + ERROR_SEPARATOR + String.join(ERROR_SEPARATOR, errors));
            alert.getButtonTypes().add(ButtonType.OK);
            alert.show();
            return;
        }
        cardDataIO.writeToFile(appState.getCardData(), file);
    }
}
