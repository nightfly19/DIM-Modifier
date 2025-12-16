package com.github.cfogrady.dim.modifier.controls;

import com.github.cfogrady.dim.modifier.utils.NameSpriteGenerator;
import com.github.cfogrady.dim.modifier.SpriteImageTranslator;
import com.github.cfogrady.vb.dim.sprite.SpriteData;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.KeyValue;
import javafx.util.Duration;
import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import java.io.File;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Slf4j
public class NameGenerationDialog extends Dialog<SpriteData.Sprite> {
    private final NameSpriteGenerator generator;
    private final SpriteImageTranslator translator;
    private final TextField nameField;
    private final ComboBox<String> fontSelector;

    private final ImageView previewImage;
    private final ScrollPane scrollPane;
    private Timeline scrollTimeline;

    public NameGenerationDialog(NameSpriteGenerator generator, SpriteImageTranslator translator) {
        this.generator = generator;
        this.translator = translator;
        this.setTitle("Generate Name Sprite");
        this.setHeaderText("Select font and enter name");

        // Remove default button types to have full control over layout
        // Remove default button types to have full control over layout
        this.getDialogPane().getButtonTypes().clear();
        // Add a hidden CLOSE button type to ensure the window close (X) works
        this.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Hide the button bar or the specific button to keep our custom layout
        // We can do this in the setOnShown listener or by looking up the button now if
        // possible.
        // Easier to just hide the button bar area if it's empty, but since we added a
        // type, it might show.
        // Let's hide the button in setOnShown.

        nameField = new TextField();
        nameField.setPromptText("Enter Name");

        fontSelector = new ComboBox<>();
        setupFontSelector();

        previewImage = new ImageView();
        previewImage.setPreserveRatio(true);
        previewImage.setSmooth(false); // Ensure pixelated rendering (crisp edges)
        // Manual scaling used instead of setFitHeight to guarantee nearest neighbor

        VBox content = new VBox(15);
        content.setMinWidth(400); // Start wider
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #2b2b2b; -fx-font-family: 'Segoe UI', sans-serif;");

        // Font Selection Section
        VBox fontSection = new VBox(5);
        Label fontLabel = new Label("Font:");
        fontLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-weight: bold;");
        fontSelector.setMaxWidth(Double.MAX_VALUE);
        fontSelector.setStyle(
                "-fx-background-color: #3c3f41; -fx-text-fill: white; -fx-border-color: #555; -fx-border-radius: 3;");
        fontSection.getChildren().addAll(fontLabel, fontSelector);

        // Name Input Section
        VBox nameSection = new VBox(5);
        Label nameLabel = new Label("Name:");
        nameLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-weight: bold;");
        nameField.setStyle(
                "-fx-background-color: #3c3f41; -fx-text-fill: white; -fx-border-color: #555; -fx-border-radius: 3;");
        nameSection.getChildren().addAll(nameLabel, nameField);

        // Preview Section
        VBox previewSection = new VBox(5);
        Label previewLabel = new Label("Preview:");
        previewLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-weight: bold;");

        javafx.scene.layout.StackPane previewContainer = new javafx.scene.layout.StackPane(previewImage);
        previewContainer.setStyle("-fx-background-color: #1e1e1e;"); // Background for the image container itself
        previewContainer.setPadding(new Insets(10));
        previewContainer.setAlignment(javafx.geometry.Pos.CENTER_LEFT); // Align left so it grows to the right

        scrollPane = new ScrollPane(previewContainer); // Initialize the member variable
        scrollPane.setFitToHeight(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle(
                "-fx-background-color: transparent; -fx-background: transparent; -fx-border-color: #555; -fx-border-radius: 5;");
        scrollPane.setMinHeight(80);
        scrollPane.setPrefHeight(80);

        previewSection.getChildren().addAll(previewLabel, scrollPane);

        // Custom Button Section for perfect centering
        javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(20);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
        buttonBox.setPadding(new Insets(10, 0, 0, 0));

        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle(
                "-fx-background-color: #444; -fx-text-fill: white; -fx-background-radius: 5; -fx-border-color: #666; -fx-border-radius: 5; -fx-min-width: 100px;");
        cancelButton.setOnMouseEntered(e -> cancelButton.setStyle(
                "-fx-background-color: #555; -fx-text-fill: white; -fx-background-radius: 5; -fx-border-color: #777; -fx-border-radius: 5; -fx-min-width: 100px;"));
        cancelButton.setOnMouseExited(e -> cancelButton.setStyle(
                "-fx-background-color: #444; -fx-text-fill: white; -fx-background-radius: 5; -fx-border-color: #666; -fx-border-radius: 5; -fx-min-width: 100px;"));
        cancelButton.setOnAction(e -> {
            setResult(null);
            close();
        });

        Button generateButton = new Button("Generate");
        generateButton.setStyle(
                "-fx-background-color: #444; -fx-text-fill: white; -fx-background-radius: 5; -fx-border-color: #666; -fx-border-radius: 5; -fx-min-width: 100px;");
        generateButton.setOnMouseEntered(e -> generateButton.setStyle(
                "-fx-background-color: #555; -fx-text-fill: white; -fx-background-radius: 5; -fx-border-color: #777; -fx-border-radius: 5; -fx-min-width: 100px;"));
        generateButton.setOnMouseExited(e -> generateButton.setStyle(
                "-fx-background-color: #444; -fx-text-fill: white; -fx-background-radius: 5; -fx-border-color: #666; -fx-border-radius: 5; -fx-min-width: 100px;"));
        generateButton.setOnAction(e -> {
            SpriteData.Sprite result = generateSprite();
            setResult(result);
            close();
        });

        buttonBox.getChildren().addAll(cancelButton, generateButton);

        content.getChildren().addAll(fontSection, nameSection, previewSection, buttonBox);

        this.getDialogPane().setContent(content);
        this.getDialogPane().setStyle("-fx-background-color: #2b2b2b;");

        // Listeners for real-time preview
        nameField.textProperty().addListener((observable, oldValue, newValue) -> updatePreview());
        fontSelector.valueProperty().addListener((observable, oldValue, newValue) -> {
            if ("Add Font...".equals(newValue)) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select Font Sprite Sheet");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Images", "*.png"));
                File file = fileChooser.showOpenDialog(getDialogPane().getScene().getWindow());
                if (file != null) {
                    generator.addFont(file);
                    setupFontSelector();
                    fontSelector.getSelectionModel().select(file.getName());
                } else {
                    // Revert to old value if cancelled
                    Platform.runLater(() -> fontSelector.getSelectionModel().select(oldValue));
                }
            } else {
                updatePreview();
            }
        });

        // Initial preview
        updatePreview();
        // Style and center buttons after dialog is shown
        this.setOnShown(e -> {
            // Hide the default button bar buttons (specifically the CLOSE one we added)
            // We only want our custom buttons to be visible.
            ButtonBar buttonBar = (ButtonBar) this.getDialogPane().lookup(".button-bar");
            if (buttonBar != null) {
                buttonBar.setVisible(false);
                buttonBar.setManaged(false);
            }
        });
    }

    private void updatePreview() {
        if (scrollTimeline != null) {
            scrollTimeline.stop();
        }
        scrollPane.setHvalue(0);

        try {
            SpriteData.Sprite sprite = generateSprite();
            if (sprite != null) {
                Image rawImg = translator.loadImageFromSprite(sprite);
                // Manually scale image 3x to ensure nearest-neighbor (crisp) rendering
                Image scaledImg = scaleImage(rawImg, 3);
                previewImage.setImage(scaledImg);

                // Calculate displayed width (already scaled + padding)
                double displayContentWidth = scaledImg.getWidth() + 20; // +20 for padding

                Platform.runLater(() -> setupScrollAnimation(displayContentWidth));

            } else {
                previewImage.setImage(null);
            }
        } catch (Exception e) {
            // Ignore errors during preview (e.g. invalid chars or no font loaded)
            previewImage.setImage(null);
        }
    }

    private void setupScrollAnimation(double contentWidth) {
        // We need viewport width.
        // If scrollPane is not laid out, width is 0.
        if (scrollPane.getWidth() == 0) {
            // Wait for first layout
            scrollPane.widthProperty().addListener((obs, old, val) -> {
                if (scrollTimeline == null || scrollTimeline.getStatus() == Animation.Status.STOPPED) {
                    setupScrollAnimation(contentWidth);
                }
            });
            return;
        }

        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        if (viewportWidth <= 0)
            viewportWidth = scrollPane.getWidth() - 20; // Estimate

        double scrollDistance = contentWidth - viewportWidth;

        if (scrollDistance > 0) {
            // Speed: 30px per second (matches NameUpdater approx)
            double durationSeconds = scrollDistance / 30.0;

            scrollTimeline = new Timeline();
            scrollTimeline.getKeyFrames().addAll(
                    new KeyFrame(Duration.ZERO, new KeyValue(scrollPane.hvalueProperty(), 0)),
                    new KeyFrame(Duration.seconds(3), new KeyValue(scrollPane.hvalueProperty(), 0)), // Wait 3s
                    new KeyFrame(Duration.seconds(3 + durationSeconds), new KeyValue(scrollPane.hvalueProperty(), 1.0)), // Scroll
                    new KeyFrame(Duration.seconds(3 + durationSeconds + 2),
                            new KeyValue(scrollPane.hvalueProperty(), 1.0)) // Pause at end for 2s
            );
            scrollTimeline.setCycleCount(Animation.INDEFINITE);
            scrollTimeline.play();
        }
    }

    private Image scaleImage(Image source, int scale) {
        int width = (int) source.getWidth();
        int height = (int) source.getHeight();
        int newWidth = width * scale;
        int newHeight = height * scale;

        WritableImage newImage = new WritableImage(newWidth, newHeight);
        PixelWriter writer = newImage.getPixelWriter();
        PixelReader reader = source.getPixelReader();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Color color = reader.getColor(x, y);
                for (int dx = 0; dx < scale; dx++) {
                    for (int dy = 0; dy < scale; dy++) {
                        writer.setColor(x * scale + dx, y * scale + dy, color);
                    }
                }
            }
        }
        return newImage;
    }

    private void setupFontSelector() {
        List<String> fonts = generator.getAvailableFonts();
        fontSelector.getItems().clear();
        fontSelector.getItems().addAll(fonts);
        fontSelector.getItems().add("Add Font...");

        if (!fonts.isEmpty() && fontSelector.getSelectionModel().getSelectedItem() == null) {
            fontSelector.getSelectionModel().select(0);
        }
    }

    private SpriteData.Sprite generateSprite() {
        String name = nameField.getText();
        String font = fontSelector.getValue();
        if (name == null || name.isEmpty() || font == null || "Add Font...".equals(font)) {
            return null;
        }
        return generator.generateNameSprite(name.toUpperCase(), font);
    }
}
