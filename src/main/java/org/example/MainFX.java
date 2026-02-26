package org.example;

import javafx.stage.Stage;

/**
 * Utility class for MainFX operations
 */
public class MainFX {

    /**
     * Toggle fullscreen mode for the primary stage
     */
    public static void toggleFullscreen() {
        Stage stage = Main.getPrimaryStage();
        if (stage != null) {
            stage.setFullScreen(!stage.isFullScreen());
        }
    }

    /**
     * Get the primary stage
     */
    public static Stage getPrimaryStage() {
        return Main.getPrimaryStage();
    }
}

