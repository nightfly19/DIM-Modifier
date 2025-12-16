package com.github.cfogrady.dim.modifier.controls;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import javafx.geometry.VPos;
import javafx.geometry.HPos;

import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import java.util.function.BiConsumer;
import lombok.Setter;

@Slf4j
public class ImageIntListView extends ListView<ImageIntPair> {

    @Setter
    private BiConsumer<Integer, Integer> onReorder;

    public void handleReorder(int oldIndex, int newIndex) {
        if (onReorder != null) {
            onReorder.accept(oldIndex, newIndex);
        }
    }

    public ImageIntListView() {
        super();
    }

    public ImageIntListView(ObservableList<ImageIntPair> valueLabels, double imageScaler, Background background,
            String noneText) {
        super();
        this.setItems(valueLabels);
        this.setCellFactory(lv -> new ImageIntCell(imageScaler, noneText, background));
    }

    public void initialize(ObservableList<ImageIntPair> valueLabels, double imageScaler, Background background,
            String noneText) {
        try {
            this.setItems(valueLabels);
            this.setCellFactory(lv -> new ImageIntCell(imageScaler, noneText, background));
        } catch (Exception e) {
            log.error("Error initializing ImageIntListView: {}", e.getMessage(), e);
        }
    }

    @RequiredArgsConstructor
    public static class ImageIntCell extends ListCell<ImageIntPair> {
        private final double scaler;
        private final String noneText;
        private final Background background;
        private static final DataFormat SERIALIZED_MIME_TYPE = new DataFormat("application/x-java-serialized-object");

        @Override
        protected void updateItem(ImageIntPair option, boolean empty) {
            try {
                super.updateItem(option, empty);
                setText(null);
                setGraphic(null);

                if (empty) {
                    setText(null);
                    setGraphic(null);
                    setOnDragDetected(null);
                    setOnDragOver(null);
                    setOnDragDropped(null);
                    setOnDragDone(null);
                } else if (option == null || option.getValue() == null) {
                    StackPane stackPane = new StackPane(new Text(noneText == null ? "NONE" : noneText));
                    stackPane.getStyleClass().add("image-int-cell-placeholder");
                    VBox.setMargin(stackPane, new Insets(10));
                    setGraphic(stackPane);
                    setOnDragDetected(null);
                    setOnDragOver(null);
                    setOnDragDropped(null);
                    setOnDragDone(null);
                } else {
                    ImageView imageView = new ImageView(option.getLabel());
                    imageView.setFitWidth(option.getLabel().getWidth() * scaler);
                    imageView.setFitHeight(option.getLabel().getHeight() * scaler);

                    Text idText = new Text(String.valueOf(option.getValue()));

                    GridPane gridPane = new GridPane();
                    gridPane.getStyleClass().add("image-int-cell-grid");
                    gridPane.add(idText, 0, 0);
                    gridPane.add(imageView, 1, 0);

                    // Definindo a largura da primeira coluna para um valor fixo
                    ColumnConstraints col1 = new ColumnConstraints(24); // 50 é um exemplo de largura fixa, ajuste
                                                                        // conforme necessário
                    // Permitindo que a segunda coluna (a da imagem) se expanda para preencher o
                    // espaço restante
                    ColumnConstraints col2 = new ColumnConstraints();
                    col2.setHgrow(Priority.ALWAYS);

                    gridPane.getColumnConstraints().addAll(col1, col2);

                    GridPane.setValignment(idText, VPos.CENTER);
                    GridPane.setHalignment(idText, HPos.CENTER); // Centralizando o ID horizontalmente
                    GridPane.setHalignment(imageView, HPos.CENTER); // Centralizando a imagem horizontalmente

                    if (background != null) {
                        gridPane.setBackground(background);
                    }
                    VBox.setMargin(gridPane, new Insets(10));
                    setGraphic(gridPane);

                    // Drag and Drop implementation
                    setOnDragDetected(event -> {
                        if (getItem() == null || getItem().getValue() < 2) { // Lock indices 0 and 1
                            return;
                        }
                        Dragboard db = startDragAndDrop(TransferMode.MOVE);
                        ClipboardContent content = new ClipboardContent();
                        content.put(SERIALIZED_MIME_TYPE, getItem().getValue());
                        db.setContent(content);
                        event.consume();
                    });

                    setOnDragOver(event -> {
                        if (event.getGestureSource() != this &&
                                event.getDragboard().hasContent(SERIALIZED_MIME_TYPE)) {
                            if (getItem() != null && getItem().getValue() >= 2) { // Lock indices 0 and 1
                                event.acceptTransferModes(TransferMode.MOVE);
                            }
                        }
                        event.consume();
                    });

                    setOnDragDropped(event -> {
                        Dragboard db = event.getDragboard();
                        boolean success = false;
                        if (db.hasContent(SERIALIZED_MIME_TYPE)) {
                            Integer sourceIndex = (Integer) db.getContent(SERIALIZED_MIME_TYPE);
                            Integer targetIndex = getItem().getValue();

                            if (getListView() instanceof ImageIntListView) {
                                ((ImageIntListView) getListView()).handleReorder(sourceIndex, targetIndex);
                            }

                            success = true;
                        }
                        event.setDropCompleted(success);
                        event.consume();
                    });

                    setOnDragDone(event -> {
                        event.consume();
                    });
                }
            } catch (Exception e) {
                log.error("Error updating item in ImageIntCell: {}", e.getMessage(), e);
                try {
                    // Tentativa de fallback simples com apenas texto
                    setText(option != null ? "Item " + option.getValue() : "ERRO");
                } catch (Exception ex) {
                    // Em caso de falha total, apenas limpa a célula
                    setText(null);
                    setGraphic(null);
                }
            }
        }
    }
}
