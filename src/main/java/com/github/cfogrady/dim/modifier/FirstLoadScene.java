package com.github.cfogrady.dim.modifier;

import com.github.cfogrady.dim.modifier.data.AppState;
import com.github.cfogrady.dim.modifier.data.card.CardData;
import com.github.cfogrady.dim.modifier.controllers.LoadedViewController;
import com.github.cfogrady.dim.modifier.data.card.CardDataIO;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import javafx.geometry.Pos;

import java.io.*;
import java.net.URL;

@RequiredArgsConstructor
@Slf4j
public class FirstLoadScene {
    private final AppState appState;
    private final Stage stage;
    private final CardDataIO cardDataIO;
    private final LoadedViewController loadedViewController;
    private static final String DARK_THEME_CSS = "/dark-theme.css";

    public void setupScene() {
        // Create main container
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("root");

        // Add icon
        ImageView icon = new ImageView(new Image(getClass().getResourceAsStream("/icon.png")));
        icon.setFitHeight(100);
        icon.setFitWidth(100);
        icon.setPreserveRatio(true);

        // Add description
        Text description = new Text("DIM Modifier");
        description.getStyleClass().add("description-text");

        // Create button with custom styling
        Button button = new Button();
        button.setText("Open DIM File");
        button.getStyleClass().add("primary-button");
        button.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Select DIM File");
            File file = fileChooser.showOpenDialog(stage);
            if (file != null) {
                loadCard(file);
                setupLoadedDataView();
            }
        });

        // Add credits
        VBox creditsBox = new VBox(5);
        creditsBox.setAlignment(Pos.CENTER);

        javafx.scene.control.Hyperlink credits1 = new javafx.scene.control.Hyperlink("DIM Modifier (Modded by Aderek)");
        javafx.scene.control.Hyperlink credits2 = new javafx.scene.control.Hyperlink("Original software by Grady");

        credits1.getStyleClass().add("credits-text");
        credits2.getStyleClass().add("credits-text");

        // Remove border and background from Hyperlink to make it look more like text
        credits1.setStyle("-fx-border-color: transparent; -fx-padding: 0;");
        credits2.setStyle("-fx-border-color: transparent; -fx-padding: 0;");

        credits1.setOnAction(e -> openUrl("https://aderek.net"));
        credits2.setOnAction(e -> openUrl("https://github.com/cfogrady"));

        creditsBox.getChildren().addAll(credits1, credits2);

        // Add all elements to root
        root.getChildren().addAll(icon, description, button);

        // Recent Files Section
        com.github.cfogrady.dim.modifier.data.RecentFilesManager recentFilesManager = new com.github.cfogrady.dim.modifier.data.RecentFilesManager();
        java.util.List<File> recentFiles = recentFilesManager.getRecentFiles();

        if (!recentFiles.isEmpty()) {
            VBox recentFilesBox = new VBox(5);
            recentFilesBox.setAlignment(Pos.CENTER);
            recentFilesBox.setMaxWidth(500);
            recentFilesBox.getStyleClass().add("recent-files-box");

            Text recentFilesTitle = new Text("Last Opened");
            recentFilesTitle.getStyleClass().add("section-title");
            recentFilesTitle.setStyle("-fx-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
            recentFilesBox.getChildren().add(recentFilesTitle);

            VBox listContainer = new VBox(2);
            listContainer.setStyle("-fx-background-color: #2b2b2b; -fx-padding: 5; -fx-background-radius: 5;");

            for (File file : recentFiles) {
                javafx.scene.layout.HBox itemBox = new javafx.scene.layout.HBox(10);
                itemBox.setAlignment(Pos.CENTER_LEFT);
                itemBox.setPadding(new javafx.geometry.Insets(5, 10, 5, 10));
                itemBox.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

                // Hover effect
                itemBox.setOnMouseEntered(e -> itemBox
                        .setStyle("-fx-background-color: #3c3f41; -fx-cursor: hand; -fx-background-radius: 3;"));
                itemBox.setOnMouseExited(e -> itemBox.setStyle("-fx-background-color: transparent; -fx-cursor: hand;"));

                javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(file.getName());
                nameLabel.setStyle("-fx-text-fill: #e0e0e0; -fx-font-weight: bold;");

                javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
                javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                String path = file.getParent();
                if (path != null) {
                    String userHome = System.getProperty("user.home");
                    if (path.startsWith(userHome)) {
                        path = "~" + path.substring(userHome.length());
                    }
                } else {
                    path = "";
                }

                javafx.scene.control.Label pathLabel = new javafx.scene.control.Label(path);
                pathLabel.setStyle("-fx-text-fill: #808080; -fx-font-size: 10px;");

                itemBox.getChildren().addAll(nameLabel, spacer, pathLabel);

                itemBox.setOnMouseClicked(e -> {
                    if (file.exists()) {
                        loadCard(file);
                        setupLoadedDataView();
                    }
                });

                listContainer.getChildren().add(itemBox);
            }
            recentFilesBox.getChildren().add(listContainer);
            root.getChildren().add(recentFilesBox);
        }

        root.getChildren().add(creditsBox);

        Scene scene = new Scene(root, 640, 480);
        applyStylesheet(scene);

        stage.setScene(scene);
        stage.show();
    }

    private void openUrl(String url) {
        try {
            java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
        } catch (Exception e) {
            log.warn("Desktop.browse failed, trying fallback for URL: {}", url);
            try {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
                } else if (os.contains("mac")) {
                    Runtime.getRuntime().exec("open " + url);
                } else if (os.contains("nix") || os.contains("nux")) {
                    Runtime.getRuntime().exec("xdg-open " + url);
                } else {
                    log.error("Unsupported OS for opening URL: {}", os);
                }
            } catch (Exception ex) {
                log.error("Failed to open URL via fallback command", ex);
            }
        }
    }

    private void loadCard(File file) {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            CardData<?, ?, ?> cardData = cardDataIO.readFromStream(fileInputStream);
            appState.setCardData(cardData);
            appState.setLastOpenedFilePath(file);

            // Add to recent files
            com.github.cfogrady.dim.modifier.data.RecentFilesManager recentFilesManager = new com.github.cfogrady.dim.modifier.data.RecentFilesManager();
            recentFilesManager.addFile(file);
        } catch (IOException e) {
            log.error("Error loading file: {}", file.getAbsolutePath(), e);
        }
    }

    private void setupLoadedDataView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/LoadedView.fxml"));
            loader.setControllerFactory(p -> loadedViewController);
            Scene scene = new Scene(loader.load(), 1520, 720);

            // Aplicar o tema escuro
            applyStylesheet(scene);

            // Atualizar a visualização e o título
            loadedViewController.refreshAll();

            // Forçar a atualização do título usando o método do controller
            loadedViewController.updateWindowTitle();

            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            log.error("Unable to load layout for loaded data view!", e);
        }
    }

    /**
     * Aplica o CSS à cena
     */
    private void applyStylesheet(Scene scene) {
        try {
            URL cssResource = getClass().getResource(DARK_THEME_CSS);
            if (cssResource != null) {
                scene.getStylesheets().add(cssResource.toExternalForm());
            } else {
                log.error("CSS stylesheet not found: {}", DARK_THEME_CSS);
            }
        } catch (Exception e) {
            log.error("Error applying stylesheet", e);
        }
    }
}
