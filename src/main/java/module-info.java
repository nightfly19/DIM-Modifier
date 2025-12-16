module DIM.Modifier.main {
    requires static lombok;
    requires org.slf4j;
    requires org.slf4j.simple;
    requires vb.dim.reader;
    requires javafx.controls;
    requires javafx.fxml;
    requires java.prefs;
    requires java.desktop;
    requires javafx.swing;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.datatype.jsr310;

    exports com.github.cfogrady.dim.modifier to javafx.graphics;
    exports com.github.cfogrady.dim.modifier.controls to javafx.fxml;

    opens com.github.cfogrady.dim.modifier to javafx.fxml;
    opens com.github.cfogrady.dim.modifier.controls to javafx.fxml;
    opens com.github.cfogrady.dim.modifier.controllers to javafx.fxml;
}