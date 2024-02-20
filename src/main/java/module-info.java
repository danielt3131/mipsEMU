@SuppressWarnings("module")
module io.github.danielt3131.mipsemu {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;

    opens io.github.danielt3131.mipsemu to javafx.fxml;
    exports io.github.danielt3131.mipsemu;
}