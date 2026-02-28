package Controllers;

import Models.JobOffer;
import Services.JobOfferService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur pour afficher un calendrier des deadlines des offres d'emploi
 * 100% local, sans API externe
 */
public class CalendarViewController {

    private JobOfferService jobOfferService;
    private YearMonth currentMonth;
    private GridPane calendarGrid;
    private Label monthYearLabel;
    private VBox eventListPanel;
    private Map<LocalDate, List<JobOffer>> offersByDate;
    private LocalDate selectedDate;

    public CalendarViewController() {
        this.jobOfferService = new JobOfferService();
        this.currentMonth = YearMonth.now();
        this.offersByDate = new HashMap<>();
    }

    /**
     * Affiche la fenêtre du calendrier
     */
    public void showCalendar() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("📅 Calendrier des Deadlines");
        stage.setMinWidth(1000);
        stage.setMinHeight(700);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f5f6f8;");

        // En-tête
        VBox header = createHeader();
        root.setTop(header);

        // Contenu principal
        HBox content = new HBox(20);
        content.setStyle("-fx-padding: 20;");

        // Calendrier à gauche
        VBox calendarSection = createCalendarSection();
        calendarSection.setPrefWidth(600);

        // Liste des événements à droite
        VBox eventsSection = createEventsSection();
        eventsSection.setPrefWidth(350);
        HBox.setHgrow(eventsSection, Priority.ALWAYS);

        content.getChildren().addAll(calendarSection, eventsSection);
        root.setCenter(content);

        // Légende en bas
        HBox legend = createLegend();
        root.setBottom(legend);

        // Charger les données
        loadOffers();
        updateCalendar();

        Scene scene = new Scene(root, 1000, 700);
        stage.setScene(scene);
        stage.show();
    }

    private VBox createHeader() {
        VBox header = new VBox(10);
        header.setStyle("-fx-background-color: linear-gradient(to right, #5BA3F5, #3d8bd4); -fx-padding: 20;");
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("📅 Calendrier des Deadlines");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: white;");

        Label subtitle = new Label("Visualisez les dates limites de candidature de toutes les offres d'emploi");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.85);");

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    private VBox createCalendarSection() {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3); -fx-padding: 20;");

        // Navigation du mois
        HBox navigation = new HBox(15);
        navigation.setAlignment(Pos.CENTER);

        Button btnPrevMonth = new Button("◀");
        btnPrevMonth.setStyle("-fx-background-color: #e9ecef; -fx-text-fill: #495057; " +
                             "-fx-font-size: 16px; -fx-padding: 8 15; -fx-background-radius: 8; -fx-cursor: hand;");
        btnPrevMonth.setOnAction(e -> {
            currentMonth = currentMonth.minusMonths(1);
            updateCalendar();
        });

        monthYearLabel = new Label();
        monthYearLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: 700; -fx-text-fill: #2c3e50; -fx-min-width: 200;");
        monthYearLabel.setAlignment(Pos.CENTER);

        Button btnNextMonth = new Button("▶");
        btnNextMonth.setStyle("-fx-background-color: #e9ecef; -fx-text-fill: #495057; " +
                             "-fx-font-size: 16px; -fx-padding: 8 15; -fx-background-radius: 8; -fx-cursor: hand;");
        btnNextMonth.setOnAction(e -> {
            currentMonth = currentMonth.plusMonths(1);
            updateCalendar();
        });

        Button btnToday = new Button("Aujourd'hui");
        btnToday.setStyle("-fx-background-color: #5BA3F5; -fx-text-fill: white; " +
                         "-fx-font-size: 12px; -fx-padding: 8 15; -fx-background-radius: 8; -fx-cursor: hand;");
        btnToday.setOnAction(e -> {
            currentMonth = YearMonth.now();
            selectedDate = LocalDate.now();
            updateCalendar();
            updateEventsList(selectedDate);
        });

        navigation.getChildren().addAll(btnPrevMonth, monthYearLabel, btnNextMonth, btnToday);

        // Jours de la semaine
        GridPane daysHeader = new GridPane();
        daysHeader.setHgap(5);
        daysHeader.setAlignment(Pos.CENTER);

        String[] days = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (int i = 0; i < 7; i++) {
            Label dayLabel = new Label(days[i]);
            dayLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #6c757d; " +
                             "-fx-pref-width: 70; -fx-alignment: center;");
            dayLabel.setAlignment(Pos.CENTER);
            daysHeader.add(dayLabel, i, 0);
        }

        // Grille du calendrier
        calendarGrid = new GridPane();
        calendarGrid.setHgap(5);
        calendarGrid.setVgap(5);
        calendarGrid.setAlignment(Pos.CENTER);

        section.getChildren().addAll(navigation, new Separator(), daysHeader, calendarGrid);
        return section;
    }

    private VBox createEventsSection() {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: white; -fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3); -fx-padding: 20;");

        Label title = new Label("📋 Offres du jour sélectionné");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        eventListPanel = new VBox(10);
        eventListPanel.setStyle("-fx-padding: 10 0;");

        Label emptyLabel = new Label("Cliquez sur une date pour voir les offres");
        emptyLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic;");
        eventListPanel.getChildren().add(emptyLabel);

        ScrollPane scrollPane = new ScrollPane(eventListPanel);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        section.getChildren().addAll(title, new Separator(), scrollPane);
        return section;
    }

    private HBox createLegend() {
        HBox legend = new HBox(30);
        legend.setStyle("-fx-background-color: white; -fx-padding: 15 20; " +
                       "-fx-border-color: #e9ecef; -fx-border-width: 1 0 0 0;");
        legend.setAlignment(Pos.CENTER);

        legend.getChildren().addAll(
            createLegendItem("#28a745", "Date limite proche (< 3 jours)"),
            createLegendItem("#ffc107", "Date limite cette semaine"),
            createLegendItem("#5BA3F5", "Date limite ce mois"),
            createLegendItem("#6c757d", "Date passée")
        );

        return legend;
    }

    private HBox createLegendItem(String color, String text) {
        HBox item = new HBox(8);
        item.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(6);
        dot.setFill(Color.web(color));

        Label label = new Label(text);
        label.setStyle("-fx-font-size: 12px; -fx-text-fill: #495057;");

        item.getChildren().addAll(dot, label);
        return item;
    }

    private void loadOffers() {
        offersByDate.clear();
        try {
            List<JobOffer> offers = jobOfferService.getAllJobOffers();
            for (JobOffer offer : offers) {
                if (offer.getDeadline() != null) {
                    LocalDate deadline = offer.getDeadline().toLocalDate();
                    offersByDate.computeIfAbsent(deadline, k -> new ArrayList<>()).add(offer);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des offres: " + e.getMessage());
        }
    }

    private void updateCalendar() {
        calendarGrid.getChildren().clear();

        // Mettre à jour le label du mois
        String monthName = currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        monthYearLabel.setText(monthName.substring(0, 1).toUpperCase() + monthName.substring(1) + " " + currentMonth.getYear());

        LocalDate firstOfMonth = currentMonth.atDay(1);
        int dayOfWeekValue = firstOfMonth.getDayOfWeek().getValue(); // 1 = Lundi

        int dayCounter = 1;
        int daysInMonth = currentMonth.lengthOfMonth();

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                int cellIndex = row * 7 + col + 1;

                if (cellIndex >= dayOfWeekValue && dayCounter <= daysInMonth) {
                    LocalDate date = currentMonth.atDay(dayCounter);
                    StackPane dayCell = createDayCell(date, dayCounter);
                    calendarGrid.add(dayCell, col, row);
                    dayCounter++;
                } else {
                    // Cellule vide
                    StackPane emptyCell = new StackPane();
                    emptyCell.setPrefSize(70, 60);
                    emptyCell.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8;");
                    calendarGrid.add(emptyCell, col, row);
                }
            }
        }
    }

    private StackPane createDayCell(LocalDate date, int dayNumber) {
        StackPane cell = new StackPane();
        cell.setPrefSize(70, 60);
        cell.setAlignment(Pos.TOP_CENTER);

        boolean isToday = date.equals(LocalDate.now());
        boolean isSelected = date.equals(selectedDate);
        boolean hasOffers = offersByDate.containsKey(date);
        int offerCount = hasOffers ? offersByDate.get(date).size() : 0;

        // Style de base
        String bgColor = "#ffffff";
        String borderColor = "#e9ecef";
        String textColor = "#2c3e50";

        if (isToday) {
            bgColor = "#e3f2fd";
            borderColor = "#5BA3F5";
        }
        if (isSelected) {
            bgColor = "#5BA3F5";
            textColor = "white";
            borderColor = "#3d8bd4";
        }

        cell.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8; " +
                     "-fx-border-color: " + borderColor + "; -fx-border-radius: 8; -fx-border-width: 1; -fx-cursor: hand;");

        VBox content = new VBox(3);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(5));

        // Numéro du jour
        Label dayLabel = new Label(String.valueOf(dayNumber));
        dayLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: " + (isToday ? "700" : "400") + "; -fx-text-fill: " + textColor + ";");

        content.getChildren().add(dayLabel);

        // Indicateurs d'offres
        if (hasOffers) {
            HBox indicators = new HBox(2);
            indicators.setAlignment(Pos.CENTER);

            String indicatorColor = getDeadlineColor(date);

            // Afficher jusqu'à 3 points + nombre si plus
            int dotsToShow = Math.min(offerCount, 3);
            for (int i = 0; i < dotsToShow; i++) {
                Circle dot = new Circle(4);
                dot.setFill(Color.web(indicatorColor));
                indicators.getChildren().add(dot);
            }

            if (offerCount > 3) {
                Label countLabel = new Label("+" + (offerCount - 3));
                countLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: " + indicatorColor + "; -fx-font-weight: 600;");
                indicators.getChildren().add(countLabel);
            }

            content.getChildren().add(indicators);
        }

        cell.getChildren().add(content);

        // Événements de la cellule
        cell.setOnMouseEntered(e -> {
            if (!isSelected) {
                cell.setStyle("-fx-background-color: #e9ecef; -fx-background-radius: 8; " +
                             "-fx-border-color: #5BA3F5; -fx-border-radius: 8; -fx-border-width: 1; -fx-cursor: hand;");
            }
        });

        cell.setOnMouseExited(e -> {
            if (!isSelected) {
                cell.setStyle("-fx-background-color: " + (isToday ? "#e3f2fd" : "#ffffff") + "; -fx-background-radius: 8; " +
                             "-fx-border-color: " + (isToday ? "#5BA3F5" : "#e9ecef") + "; -fx-border-radius: 8; -fx-border-width: 1; -fx-cursor: hand;");
            }
        });

        cell.setOnMouseClicked(e -> {
            selectedDate = date;
            updateCalendar();
            updateEventsList(date);
        });

        // Tooltip
        if (hasOffers) {
            Tooltip tooltip = new Tooltip(offerCount + " offre(s) avec deadline ce jour");
            Tooltip.install(cell, tooltip);
        }

        return cell;
    }

    private String getDeadlineColor(LocalDate deadline) {
        LocalDate today = LocalDate.now();

        if (deadline.isBefore(today)) {
            return "#6c757d"; // Passé - gris
        }

        long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, deadline);

        if (daysUntil <= 3) {
            return "#28a745"; // Urgent - vert
        } else if (daysUntil <= 7) {
            return "#ffc107"; // Cette semaine - jaune
        } else {
            return "#5BA3F5"; // Plus tard - bleu
        }
    }

    private void updateEventsList(LocalDate date) {
        eventListPanel.getChildren().clear();

        if (date == null) {
            Label emptyLabel = new Label("Sélectionnez une date");
            emptyLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic;");
            eventListPanel.getChildren().add(emptyLabel);
            return;
        }

        // Titre avec la date
        Label dateTitle = new Label("📅 " + date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)));
        dateTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #5BA3F5;");
        eventListPanel.getChildren().add(dateTitle);

        List<JobOffer> offers = offersByDate.get(date);
        if (offers == null || offers.isEmpty()) {
            Label noOffers = new Label("Aucune deadline pour cette date");
            noOffers.setStyle("-fx-text-fill: #6c757d; -fx-font-style: italic; -fx-padding: 20 0;");
            eventListPanel.getChildren().add(noOffers);
            return;
        }

        Label countLabel = new Label(offers.size() + " offre(s) avec deadline");
        countLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d; -fx-padding: 0 0 10 0;");
        eventListPanel.getChildren().add(countLabel);

        for (JobOffer offer : offers) {
            VBox offerCard = createOfferCard(offer);
            eventListPanel.getChildren().add(offerCard);
        }
    }

    private VBox createOfferCard(JobOffer offer) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 12; " +
                     "-fx-border-color: #e9ecef; -fx-border-radius: 8;");

        // Titre
        Label title = new Label(offer.getTitle());
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");
        title.setWrapText(true);

        // Localisation
        HBox locationBox = new HBox(5);
        locationBox.setAlignment(Pos.CENTER_LEFT);
        Label locationIcon = new Label("📍");
        Label location = new Label(offer.getLocation() != null ? offer.getLocation() : "Non spécifié");
        location.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d;");
        locationBox.getChildren().addAll(locationIcon, location);

        // Type de contrat
        HBox contractBox = new HBox(5);
        contractBox.setAlignment(Pos.CENTER_LEFT);
        Label contractIcon = new Label("💼");
        Label contract = new Label(offer.getContractType() != null ? offer.getContractType().toString() : "Non spécifié");
        contract.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d;");
        contractBox.getChildren().addAll(contractIcon, contract);

        // Statut de la deadline
        String deadlineStatus = getDeadlineStatus(offer.getDeadline() != null ? offer.getDeadline().toLocalDate() : null);
        Label statusLabel = new Label(deadlineStatus);
        statusLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 600; -fx-text-fill: " +
            getDeadlineColor(offer.getDeadline() != null ? offer.getDeadline().toLocalDate() : LocalDate.now()) + ";");

        card.getChildren().addAll(title, locationBox, contractBox, statusLabel);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #e9ecef; -fx-background-radius: 8; -fx-padding: 12; " +
                                                  "-fx-border-color: #5BA3F5; -fx-border-radius: 8; -fx-cursor: hand;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-padding: 12; " +
                                                 "-fx-border-color: #e9ecef; -fx-border-radius: 8;"));

        return card;
    }

    private String getDeadlineStatus(LocalDate deadline) {
        if (deadline == null) return "";

        LocalDate today = LocalDate.now();
        long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, deadline);

        if (daysUntil < 0) {
            return "⏰ Deadline passée il y a " + Math.abs(daysUntil) + " jour(s)";
        } else if (daysUntil == 0) {
            return "🔥 Deadline aujourd'hui !";
        } else if (daysUntil == 1) {
            return "⚡ Deadline demain !";
        } else if (daysUntil <= 3) {
            return "⚠️ " + daysUntil + " jours restants";
        } else if (daysUntil <= 7) {
            return "📅 " + daysUntil + " jours restants";
        } else {
            return "📅 " + daysUntil + " jours restants";
        }
    }

    /**
     * Méthode statique pour ouvrir le calendrier
     */
    public static void show() {
        CalendarViewController controller = new CalendarViewController();
        controller.showCalendar();
    }
}



