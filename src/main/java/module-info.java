module org.example.javafx_example {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;

    opens org.example.javafx_example to javafx.fxml;
    exports org.example.javafx_example;
}