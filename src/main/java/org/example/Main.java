package org.example;

import Services.InterviewReminderScheduler;
import javafx.application.Application;

/**
 * Launcher — delegates to MainFX to avoid JavaFX module issues.
 */
public class Main {
    public static void main(String[] args) {
        Application.launch(MainFX.class, args);
    }
}