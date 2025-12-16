package com.github.cfogrady.dim.modifier.controllers;

import com.github.cfogrady.dim.modifier.SpriteImageTranslator;
import com.github.cfogrady.dim.modifier.SpriteReplacer;
import com.github.cfogrady.dim.modifier.data.AppState;
import com.github.cfogrady.dim.modifier.data.card.CardSprites;
import com.github.cfogrady.dim.modifier.data.card.Character;
import com.github.cfogrady.vb.dim.sprite.SpriteData;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.util.Map;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

@Slf4j
@RequiredArgsConstructor
public class StatsViewController implements Initializable {
    public static final int IDLE_SPRITE_IDX = 1;
    public static SpriteData.SpriteDimensions CUTIN_DIMENSIONS = SpriteData.SpriteDimensions.builder().width(80)
            .height(160).build();

    private final AppState appState;
    private final SpriteImageTranslator spriteImageTranslator;
    private final SpriteReplacer spriteReplacer;
    private final StatsGridController statsGridController;

    @FXML
    private StackPane backgroundStackPane;
    @FXML
    private ImageView imageView;
    @FXML
    private Button prevSpriteButton;
    @FXML
    private Button nextSpriteButton;
    @FXML
    private StackPane gridContainer;
    @FXML
    private ComboBox<String> animationComboBox;

    @Setter
    private Runnable refreshIdleSprite;
    @Setter
    private Runnable refreshAll;
    @Setter
    private Character<?, ?> character;
    private int spriteOption = 1;
    private Timeline animationTimeline;
    private int currentAnimationFrameIndex = 0;
    private final Map<String, int[]> standardAnimationFrames = new HashMap<>();
    private final Map<String, int[]> babyAnimationFrames = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        statsGridController.setStackPane(gridContainer);
        statsGridController.setResetView(refreshAll);

        // Configurar o imageView para usar nearest neighbor (sem interpolação)
        imageView.setSmooth(false);
        // Aplicar estilo CSS para forçar nearest-neighbor
        imageView.setStyle("-fx-interpolation: nearest-neighbor;");

        setupAnimationFrames();
        setupAnimationComboBox();
    }

    private void setupAnimationFrames() {
        standardAnimationFrames.put("None", new int[] {});
        standardAnimationFrames.put("Idle", new int[] { 1, 2 });
        standardAnimationFrames.put("Walk", new int[] { 3, 4 });
        standardAnimationFrames.put("Run", new int[] { 5, 6 });
        standardAnimationFrames.put("Training", new int[] { 7, 8 });
        standardAnimationFrames.put("Happy", new int[] { 1, 9 });
        standardAnimationFrames.put("Sleep", new int[] { 10 });
        standardAnimationFrames.put("Attack", new int[] { 1, 11 });

        babyAnimationFrames.put("None", new int[] {});
        babyAnimationFrames.put("Idle", new int[] { 1, 2 });
        babyAnimationFrames.put("Walk", new int[] { 1, 3 });
        babyAnimationFrames.put("Happy", new int[] { 1, 4 });
        babyAnimationFrames.put("Sleep", new int[] { 5 });
    }

    private void setupAnimationComboBox() {
        animationComboBox.setOnAction(e -> {
            String selectedAnimation = animationComboBox.getValue();
            if (selectedAnimation == null || "None".equals(selectedAnimation)) {
                stopAnimation();
            } else {
                startAnimation(selectedAnimation);
            }
        });
    }

    private void updateAnimationOptions() {
        String currentSelection = animationComboBox.getValue();
        animationComboBox.getItems().clear();

        List<String> order;
        if (character.getStage() < 2) {
            order = List.of("None", "Idle", "Walk", "Happy", "Sleep");
        } else {
            order = List.of("None", "Idle", "Walk", "Run", "Training", "Happy", "Sleep", "Attack");
        }

        animationComboBox.getItems().addAll(order);

        if (currentSelection != null && order.contains(currentSelection)) {
            animationComboBox.setValue(currentSelection);
        } else {
            animationComboBox.setValue("None");
            stopAnimation();
        }
    }

    private void startAnimation(String animationName) {
        stopAnimation();
        Map<String, int[]> framesMap = character.getStage() < 2 ? babyAnimationFrames : standardAnimationFrames;
        int[] frames = framesMap.get(animationName);
        if (frames == null || frames.length == 0)
            return;

        currentAnimationFrameIndex = 0;

        // Hide navigation buttons during animation
        prevSpriteButton.setVisible(false);
        prevSpriteButton.setManaged(false);
        nextSpriteButton.setVisible(false);
        nextSpriteButton.setManaged(false);

        animationTimeline = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            int spriteIdx = frames[currentAnimationFrameIndex];
            if (spriteIdx < character.getSprites().size()) {
                refreshSprite(character.getSprites().get(spriteIdx));
                spriteOption = spriteIdx; // Update current selection so if we stop, we are at this frame
            }
            currentAnimationFrameIndex = (currentAnimationFrameIndex + 1) % frames.length;
        }));
        animationTimeline.setCycleCount(Timeline.INDEFINITE);
        animationTimeline.play();

        // Show first frame immediately
        int spriteIdx = frames[0];
        if (spriteIdx < character.getSprites().size()) {
            refreshSprite(character.getSprites().get(spriteIdx));
            spriteOption = spriteIdx;
        }
    }

    private void stopAnimation() {
        if (animationTimeline != null) {
            animationTimeline.stop();
            animationTimeline = null;
        }

        // Restore visibility
        prevSpriteButton.setVisible(true);
        prevSpriteButton.setManaged(true);
        nextSpriteButton.setVisible(true);
        nextSpriteButton.setManaged(true);

        refreshSpriteButtons(); // Re-enable buttons based on current sprite
    }

    public void clearState() {
        stopAnimation();
        if (animationComboBox != null) {
            animationComboBox.setValue("None");
        }
        spriteOption = 1;
    }

    public void refreshAll() {
        updateAnimationOptions();
        refreshSpriteSection();
        refreshGrid();
    }

    private void refreshSpriteSection() {
        if (spriteOption >= character.getSprites().size()) {
            spriteOption = IDLE_SPRITE_IDX;
        }
        SpriteData.Sprite sprite = character.getSprites().get(spriteOption);
        refreshSprite(sprite);
        refreshBackground(sprite);
        refreshSpriteButtons();
    }

    private void refreshSpriteButtons() {
        if (animationTimeline != null && animationTimeline.getStatus() == Timeline.Status.RUNNING) {
            prevSpriteButton.setDisable(true);
            nextSpriteButton.setDisable(true);
            return;
        }
        prevSpriteButton.setDisable(spriteOption == IDLE_SPRITE_IDX);
        prevSpriteButton.setOnAction(event -> {
            spriteOption--;
            refreshSprite(character.getSprites().get(spriteOption));
            refreshSpriteButtons();
        });
        nextSpriteButton.setDisable(spriteOption == character.getSprites().size() - 1);
        nextSpriteButton.setOnAction(event -> {
            spriteOption++;
            refreshSprite(character.getSprites().get(spriteOption));
            refreshSpriteButtons();
        });
    }

    private void refreshBackground(SpriteData.Sprite sprite) {
        backgroundStackPane.setBackground(getBackground()); // 160x320
        backgroundStackPane.setOnDragOver(e -> {
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
        backgroundStackPane.setOnDragDropped(e -> {
            if (e.getDragboard().hasFiles()) {
                List<File> files = e.getDragboard().getFiles();
                File file = files.get(0);
                SpriteData.Sprite newSprite = spriteReplacer.loadSpriteFromFile(file);
                replaceCharacterSprite(newSprite);
            }
        });
        backgroundStackPane.setOnMouseClicked(click -> {
            if (animationTimeline != null && animationTimeline.getStatus() == Timeline.Status.RUNNING) {
                return;
            }
            SpriteData.Sprite newSprite = spriteReplacer.loadSpriteFromFileChooser();
            replaceCharacterSprite(newSprite);
        });
    }

    private void refreshSprite(SpriteData.Sprite sprite) {
        setImageViewToSprite(sprite);
    }

    private void setImageViewToSprite(SpriteData.Sprite sprite) {
        log.info("Sprite Displayed size {}x{}. Being displayed at {}x{}: ", sprite.getWidth(), sprite.getHeight(),
                sprite.getWidth() * 2, sprite.getHeight() * 2);

        // Carregar a imagem original
        Image originalImage = spriteImageTranslator.loadImageFromSprite(sprite);

        // Definir um fator de escala fixo (2x)
        int scaleFactor = 2;

        // Criar uma imagem ampliada usando nearest-neighbor manualmente
        WritableImage scaledImage = createScaledImageWithNearestNeighbor(originalImage, scaleFactor);

        // Definir a imagem redimensionada no ImageView
        imageView.setImage(scaledImage);

        // Como já redimensionamos a imagem, definimos FitWidth/FitHeight para o tamanho
        // exato
        imageView.setFitWidth(sprite.getWidth() * scaleFactor);
        imageView.setFitHeight(sprite.getHeight() * scaleFactor);

        // Garantir que não haja suavização
        imageView.setSmooth(false);
    }

    /**
     * Cria uma imagem ampliada usando o algoritmo nearest-neighbor
     * para preservar os pixels nítidos
     */
    private WritableImage createScaledImageWithNearestNeighbor(Image originalImage, int scaleFactor) {
        int originalWidth = (int) originalImage.getWidth();
        int originalHeight = (int) originalImage.getHeight();
        int newWidth = originalWidth * scaleFactor;
        int newHeight = originalHeight * scaleFactor;

        WritableImage scaledImage = new WritableImage(newWidth, newHeight);
        PixelReader reader = originalImage.getPixelReader();
        PixelWriter writer = scaledImage.getPixelWriter();

        // Copiar cada pixel da imagem original para uma área de scaleFactor x
        // scaleFactor
        for (int y = 0; y < originalHeight; y++) {
            for (int x = 0; x < originalWidth; x++) {
                // Ler a cor do pixel original
                var color = reader.getColor(x, y);

                // Preencher uma área de scaleFactor x scaleFactor na imagem de destino
                for (int dy = 0; dy < scaleFactor; dy++) {
                    for (int dx = 0; dx < scaleFactor; dx++) {
                        writer.setColor(x * scaleFactor + dx, y * scaleFactor + dy, color);
                    }
                }
            }
        }

        return scaledImage;
    }

    private void replaceCharacterSprite(SpriteData.Sprite newSprite) {
        if (newSprite != null) {
            SpriteData.SpriteDimensions proposedDimensions = newSprite.getSpriteDimensions();
            if (isValidSpriteSize(proposedDimensions)) {
                character.getSprites().set(spriteOption, newSprite);
                if (spriteOption == AppState.SELECTION_SPRITE_IDX) {
                    refreshIdleSprite.run();
                }
                refreshSprite(newSprite);

                // Marcar alterações não salvas
                appState.markUnsavedChanges();
            } else {
                Alert alert = new Alert(Alert.AlertType.NONE,
                        CardSprites.getDimensionsText(proposedDimensions, character.getValidDimensions()));
                alert.getButtonTypes().add(ButtonType.OK);
                alert.show();
            }
        }
    }

    private boolean isValidSpriteSize(SpriteData.SpriteDimensions proposedDimensions) {
        int backgroundSprite = character.getSprites().size() - 1;
        if (spriteOption < backgroundSprite && character.isSpriteSizeValid(proposedDimensions)) {
            return true;
        } else if (spriteOption == backgroundSprite && proposedDimensions.equals(CUTIN_DIMENSIONS)) {
            return true;
        }
        return false;
    }

    private void refreshGrid() {
        statsGridController.refreshStatsGrid(character);
    }

    private Background getBackground() {
        SpriteData.Sprite sprite = appState.getSelectedBackground();
        Image originalImage = spriteImageTranslator.loadImageFromSprite(sprite);

        // Aplicar a mesma técnica de escala nearest-neighbor ao background
        int scaleFactor = 2;
        WritableImage scaledImage = createScaledImageWithNearestNeighbor(originalImage, scaleFactor);

        // Usar a imagem pré-escalada e configurar o BackgroundSize
        // para não aplicar redimensionamento adicional
        BackgroundSize size = new BackgroundSize(
                BackgroundSize.AUTO,
                BackgroundSize.AUTO,
                false, // Não repetir para preencher width
                false, // Não repetir para preencher height
                false, // Não dimensionar para width
                false // Não dimensionar para height
        );

        BackgroundImage backgroundImage = new BackgroundImage(
                scaledImage,
                BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT,
                BackgroundPosition.CENTER,
                size);

        return new Background(backgroundImage);
    }
}
