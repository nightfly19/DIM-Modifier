package com.github.cfogrady.dim.modifier.controllers;

import com.github.cfogrady.dim.modifier.SpriteImageTranslator;
import com.github.cfogrady.dim.modifier.SpriteReplacer;
import com.github.cfogrady.dim.modifier.controls.ImageIntListView;
import com.github.cfogrady.dim.modifier.utils.NameSpriteGenerator;
import com.github.cfogrady.dim.modifier.data.AppState;
import com.github.cfogrady.dim.modifier.data.card.Character;
import com.github.cfogrady.vb.dim.sprite.SpriteData;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.geometry.Side;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.input.MouseButton;
import com.github.cfogrady.dim.modifier.controls.NameGenerationDialog;
import javafx.scene.control.TabPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

@Slf4j
@RequiredArgsConstructor
public class CharacterViewController implements Initializable {
    private final Timer timer;
    private final AppState appState;
    private final SpriteImageTranslator spriteImageTranslator;
    private final SpriteReplacer spriteReplacer;
    private final Node statsSubView;
    private final StatsViewController statsViewController;
    private final TransformationViewController transformationViewController;
    private final Node transformationsSubView;

    @FXML
    private ImageIntListView characterSelectionListView;

    @FXML
    private StackPane nameBox;
    @FXML
    private TabPane tabPane;
    @FXML
    private Tab statsTab;
    @FXML
    private Tab transformationsTab;
    @FXML
    private Button newCharacterButton;
    @FXML
    private Button deleteCharacterButton;
    @FXML
    private Button exportCharacterSpritesButton;
    @FXML
    private Button importCharacterSpritesButton;
    @FXML
    private AnchorPane statsSubViewPane;
    @FXML
    private AnchorPane transformationsSubViewPane;

    private enum SubViewSelection {
        STATS,
        TRANSFORMATIONS;
    }

    private int characterSelection = 0;
    private NameUpdater nameUpdater;
    private SubViewSelection subViewSelection = SubViewSelection.STATS;
    private final NameSpriteGenerator nameSpriteGenerator = new NameSpriteGenerator();
    private final ContextMenu nameBoxContextMenu = new ContextMenu();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeCharacterButtons();
        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (newValue == statsTab) {
                    subViewSelection = SubViewSelection.STATS;
                } else if (newValue == transformationsTab) {
                    subViewSelection = SubViewSelection.TRANSFORMATIONS;
                }
                updateSubViews();
                log.debug("Tab changed to: {}", subViewSelection);
            } catch (Exception e) {
                log.error("Error changing character tab: {}", e.getMessage(), e);
            }
        });
        statsViewController.setRefreshIdleSprite(this::initializeCharacterSelectionListView);
        statsViewController.setRefreshAll(this::refreshAll);
        statsViewController.initialize(location, resources);
    }

    public void clearState() {
        characterSelection = 0;
        statsViewController.clearState();
    }

    public void refreshAll() {
        try {
            initializeCharacterSelectionListView();
            try {
                initializeNameBox();
            } catch (Exception e) {
                log.error("Error initializing name box: {}", e.getMessage(), e);
            }
            try {
                updateNewDeleteButtons();
            } catch (Exception e) {
                log.error("Error updating buttons: {}", e.getMessage(), e);
            }
            try {
                updateSubViews();
            } catch (Exception e) {
                log.error("Error updating subviews: {}", e.getMessage(), e);
            }
            log.debug("Complete refresh completed successfully");
        } catch (Exception e) {
            log.error("Critical error during refreshAll(): {}", e.getMessage(), e);
        }
    }

    private void initializeCharacterButtons() {
        newCharacterButton.setOnAction(e -> {
            appState.getCardData().addCharacter(characterSelection, spriteImageTranslator);
            characterSelection++;
            refreshAll();
        });
        deleteCharacterButton.setOnAction(e -> {
            appState.getCardData().deleteCharacter(characterSelection);
            int numberOfRemainingCharacters = appState.getCardData().getCharacters().size();
            if (characterSelection >= numberOfRemainingCharacters) {
                characterSelection = numberOfRemainingCharacters - 1;
            }
            refreshAll();
        });
        exportCharacterSpritesButton.setOnAction(e -> {
            spriteImageTranslator.exportCharacterSpriteSheet(appState.getCharacter(characterSelection));
        });
        importCharacterSpritesButton.setOnAction(e -> {
            spriteImageTranslator.importSpriteSheet(appState.getCharacter(characterSelection));
            refreshAll();
        });
    }

    private void initializeCharacterSelectionListView() {
        try {
            characterSelectionListView.initialize(
                    spriteImageTranslator.createImageValuePairs(appState.getIdleForCharacters()), 1.0, null, null);
            characterSelectionListView.setOnReorder((oldIndex, newIndex) -> {
                try {
                    appState.getCardData().reorderCharacters(oldIndex, newIndex);
                    characterSelection = newIndex;
                    refreshAll();
                } catch (Exception e) {
                    log.error("Error reordering characters: {}", e.getMessage(), e);
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Error reordering characters: " + e.getMessage());
                    alert.show();
                }
            });
            characterSelectionListView.getSelectionModel().selectedItemProperty()
                    .addListener((observable, oldValue, newValue) -> {
                        if (newValue != null) {
                            try {
                                characterSelection = newValue.getValue();
                                if (nameUpdater != null) {
                                    nameUpdater.cancel();
                                }
                                initializeNameBox();
                                updateSubViews();
                                log.debug("Character selection completed for character: {}", characterSelection);
                            } catch (Exception e) {
                                log.error("Error processing character selection: {}", e.getMessage(), e);
                            }
                        }
                    });
        } catch (Exception e) {
            log.error("Error initializing character selection list view: {}", e.getMessage(), e);
        }
    }

    private static final String NAME_TEXT_ERROR = "Name sprites must be a multiple of 80 pixels in width and 15 pixels in height. Examples: 80x15, 160x15, 240x15, etc.";

    private void initializeNameBox() {
        Character<?, ?> character = appState.getCardData().getCharacters().get(characterSelection);
        SpriteData.Sprite nameSprite = character.getSprites().get(0);
        Image image = spriteImageTranslator.loadImageFromSprite(nameSprite);
        ImageView imageView = new ImageView(image);
        imageView.setViewport(new Rectangle2D(0, 0, 80, 15));
        if (nameSprite.getWidth() > 80) {
            nameUpdater = new NameUpdater(nameSprite.getWidth(), imageView, -80);
            timer.scheduleAtFixedRate(nameUpdater, 0, 33);
        }
        nameBox.getChildren().clear();
        nameBox.getChildren().add(imageView);
        nameBox.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY) {
                if (nameBoxContextMenu.isShowing()) {
                    nameBoxContextMenu.hide();
                    return;
                }

                nameBoxContextMenu.getItems().clear();

                MenuItem selectImageItem = new MenuItem("Select Image");
                selectImageItem.setOnAction(e -> {
                    SpriteData.Sprite newNameSprite = spriteReplacer.replaceSprite(nameSprite, false, true);
                    if (newNameSprite != null) {
                        replaceNameSprite(character, newNameSprite);
                    }
                });

                MenuItem generateNameItem = new MenuItem("Generate Name");
                generateNameItem.setOnAction(e -> {
                    if (nameSpriteGenerator.getAvailableFonts().isEmpty()) {
                        ButtonType addFontButton = new ButtonType("Add Font");
                        Alert alert = new Alert(Alert.AlertType.ERROR,
                                "No fonts found in resources/fonts. Please add font sprite sheets (e.g. VB_Alphabet_ENG.png) to the 'fonts' folder.",
                                addFontButton, ButtonType.OK);
                        alert.showAndWait().ifPresent(type -> {
                            if (type == addFontButton) {
                                javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                                fileChooser.setTitle("Select Font Sprite Sheet");
                                fileChooser.getExtensionFilters()
                                        .add(new javafx.stage.FileChooser.ExtensionFilter("PNG Images", "*.png"));
                                File file = fileChooser.showOpenDialog(nameBox.getScene().getWindow());
                                if (file != null) {
                                    nameSpriteGenerator.addFont(file);
                                    Alert successAlert = new Alert(Alert.AlertType.INFORMATION,
                                            "Font added successfully!");
                                    successAlert.show();
                                }
                            }
                        });
                        return;
                    }

                    NameGenerationDialog dialog = new NameGenerationDialog(nameSpriteGenerator, spriteImageTranslator);
                    dialog.showAndWait().ifPresent(generatedSprite -> {
                        replaceNameSprite(character, generatedSprite);
                    });
                });

                nameBoxContextMenu.getItems().addAll(selectImageItem, generateNameItem);
                nameBoxContextMenu.show(nameBox, Side.BOTTOM, 0, 0);
            }
        });
        nameBox.setOnDragDropped(e -> {
            if (e.getDragboard().hasFiles()) {
                List<File> files = e.getDragboard().getFiles();
                File file = files.get(0);
                SpriteData.Sprite newSprite = spriteReplacer.loadSpriteFromFile(file);
                replaceNameSprite(character, newSprite);
            }
        });
        nameBox.setOnDragOver(e -> {
            if (e.getDragboard().hasImage()) {
                e.acceptTransferModes(TransferMode.ANY);
                log.info("Drag Over Image");
                e.consume();
            } else if (e.getDragboard().hasFiles()) {
                if (e.getDragboard().getFiles().size() > 1) {
                    log.info("Can only load 1 file at a time");
                } else {
                    e.acceptTransferModes(TransferMode.ANY);
                    e.consume();
                }
            }
        });
    }

    private void replaceNameSprite(Character<?, ?> character, SpriteData.Sprite nameSprite) {
        SpriteData.SpriteDimensions newSpriteDimensions = nameSprite.getSpriteDimensions();
        if (newSpriteDimensions.getHeight() == 15 && newSpriteDimensions.getWidth() % 80 == 0) {
            character.getSprites().set(0, nameSprite);
            initializeNameBox();
        } else {
            Alert alert = new Alert(Alert.AlertType.NONE, NAME_TEXT_ERROR);
            alert.getButtonTypes().add(ButtonType.OK);
            alert.show();
        }
    }

    private void updateNewDeleteButtons() {
        newCharacterButton.setDisable(appState.getCardData().getCharacters().size() >= appState.getCardData()
                .getNumberOfAvailableCharacterSlots());
        deleteCharacterButton.setDisable(appState.getCardData().getCharacters().size() == 1);
    }

    private void updateSubViews() {
        try {
            switch (subViewSelection) {
                case STATS -> tabPane.getSelectionModel().select(statsTab);
                case TRANSFORMATIONS -> tabPane.getSelectionModel().select(transformationsTab);
            }

            statsSubViewPane.getChildren().clear();
            transformationsSubViewPane.getChildren().clear();

            statsSubViewPane.getChildren().add(statsSubView);
            transformationsSubViewPane.getChildren().add(transformationsSubView);

            switch (subViewSelection) {
                case STATS -> {
                    statsViewController.setCharacter(appState.getCharacter(characterSelection));
                    statsViewController.refreshAll();
                }
                case TRANSFORMATIONS -> {
                    transformationViewController.setCharacter(appState.getCharacter(characterSelection));
                    transformationViewController.refreshAll();
                }
            }
            log.debug("SubViews updated successfully for character {}", characterSelection);
        } catch (Exception e) {
            log.error("Error during subview update for character {}: {}", characterSelection, e.getMessage(), e);
        }
    }

    @AllArgsConstructor
    public static class NameUpdater extends TimerTask {
        private final int spriteWidth;
        private final ImageView imageView;
        private int offsetX;

        @Override
        public void run() {
            // hack to make it pause at the start and end.
            if (offsetX > spriteWidth) {
                offsetX = -80;
                imageView.setViewport(new Rectangle2D(0, 0, 80, 15));
            } else if (offsetX < 0) {
                imageView.setViewport(new Rectangle2D(0, 0, 80, 15));
            } else {
                imageView.setViewport(new Rectangle2D(offsetX % spriteWidth, 0, 80, 15));
            }
            offsetX += 1;
        }
    }
}
