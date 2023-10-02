module games.dragonhowl.onelick {
    requires java.base;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires mrpacklib;

    exports games.dragonhowl.oneclick;
    opens games.dragonhowl.oneclick;
}