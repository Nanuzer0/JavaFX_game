module org.example.javafx_example {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;

    exports org.example.javafx_example;
    exports org.example.javafx_example.client;
    exports org.example.javafx_example.server;

    opens org.example.javafx_example to javafx.fxml;
    opens org.example.javafx_example.client to javafx.fxml;
}