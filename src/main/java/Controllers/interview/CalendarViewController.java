package Controllers.interview;

import Models.interview.Interview;
import Models.joboffers.JobOffer;
import Services.interview.InterviewService;
import Services.joboffers.JobOfferService;
import Utils.MyDatabase;
import Utils.UserContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Unified Calendar: Job Offer Deadlines + Interviews.
 * Toggle to view offers only, interviews only, or both.
 * Recruiter sees upcoming interviews in a dedicated panel.
 */
public class CalendarViewController {

    public enum ViewMode { OFFERS, INTERVIEWS, BOTH }
    private ViewMode currentMode = ViewMode.BOTH;

    private final JobOfferService jobOfferService = new JobOfferService();
    private final Map<LocalDate, List<JobOffer>>  offersByDate     = new HashMap<>();
    private final Map<LocalDate, List<Interview>> interviewsByDate = new HashMap<>();

    private YearMonth currentMonth = YearMonth.now();
    private LocalDate selectedDate = LocalDate.now();

    private GridPane calendarGrid;
    private Label    monthYearLabel;
    private VBox     eventListPanel;
    private Button btnViewBoth, btnViewOffers, btnViewInterviews;

    // ── Entry point ────────────────────────────────────────────────────────────

    public static void show() { new CalendarViewController().showCalendar(); }

    public void showCalendar() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Calendrier — Offres & Entretiens");
        stage.setMinWidth(1100); stage.setMinHeight(720);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #F5F6F8;");
        root.setTop(buildHeader());
        root.setCenter(buildContent());
        root.setBottom(buildLegend());

        loadData();
        refreshCalendar();
        updateEventsList(selectedDate);
        // No upcoming panel

        stage.setScene(new Scene(root, 1100, 720));
        stage.show();
    }

    // =========================================================================
    // Header
    // =========================================================================

    private VBox buildHeader() {
        VBox header = new VBox(12);
        header.setStyle("-fx-background-color: linear-gradient(to right,#2c3e50,#3d5a80); -fx-padding: 20 25;");

        // Title row
        HBox titleRow = new HBox(15);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label("📅");
        iconLbl.setStyle("-fx-font-size: 28px;");

        VBox titleBox = new VBox(2);
        Label title = new Label("Calendrier Unifié");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: white;");
        Label sub = new Label("Deadlines des offres d'emploi  ·  Entretiens planifiés");
        sub.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.75);");
        titleBox.getChildren().addAll(title, sub);

        Region sp1 = new Region(); HBox.setHgrow(sp1, Priority.ALWAYS);
        titleRow.getChildren().addAll(iconLbl, titleBox, sp1);

        // Toggle row
        HBox toggleRow = new HBox(8);
        toggleRow.setAlignment(Pos.CENTER_LEFT);

        Label viewLbl = new Label("Afficher :");
        viewLbl.setStyle("-fx-text-fill: rgba(255,255,255,0.80); -fx-font-size: 13px;");

        btnViewBoth        = toggleBtn("Tout",            true);
        btnViewOffers      = toggleBtn("Offres seules",   false);
        btnViewInterviews  = toggleBtn("Entretiens seuls",false);

        btnViewBoth.setOnAction(e       -> switchView(ViewMode.BOTH));
        btnViewOffers.setOnAction(e     -> switchView(ViewMode.OFFERS));
        btnViewInterviews.setOnAction(e -> switchView(ViewMode.INTERVIEWS));

        Region sp2 = new Region(); HBox.setHgrow(sp2, Priority.ALWAYS);

        Button btnToday = new Button("Aujourd'hui");
        btnToday.setStyle("-fx-background-color: rgba(255,255,255,0.20); -fx-text-fill: white; " +
                "-fx-font-size: 12px; -fx-padding: 7 18; -fx-background-radius: 20; -fx-cursor: hand;");
        btnToday.setOnAction(e -> {
            currentMonth = YearMonth.now();
            selectedDate = LocalDate.now();
            refreshCalendar();
            updateEventsList(selectedDate);
        });

        toggleRow.getChildren().addAll(viewLbl, btnViewBoth, btnViewOffers, btnViewInterviews, sp2, btnToday);
        header.getChildren().addAll(titleRow, toggleRow);
        return header;
    }

    private Button toggleBtn(String text, boolean active) {
        Button btn = new Button(text);
        applyToggleStyle(btn, active);
        return btn;
    }

    private void applyToggleStyle(Button btn, boolean active) {
        if (active) {
            btn.setStyle("-fx-background-color: white; -fx-text-fill: #2c3e50; -fx-font-weight: 700; " +
                         "-fx-padding: 7 18; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 12px;");
        } else {
            btn.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: rgba(255,255,255,0.9); " +
                         "-fx-padding: 7 18; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 12px;");
        }
    }

    private void switchView(ViewMode mode) {
        currentMode = mode;
        applyToggleStyle(btnViewBoth,       mode == ViewMode.BOTH);
        applyToggleStyle(btnViewOffers,     mode == ViewMode.OFFERS);
        applyToggleStyle(btnViewInterviews, mode == ViewMode.INTERVIEWS);
        refreshCalendar();
        updateEventsList(selectedDate);
    }

    // =========================================================================
    // Content
    // =========================================================================

    private HBox buildContent() {
        HBox content = new HBox(16);
        content.setStyle("-fx-padding: 16;");
        VBox.setVgrow(content, Priority.ALWAYS);

        VBox calSection = buildCalendarSection();
        calSection.setPrefWidth(640); calSection.setMinWidth(520);

        VBox evSection = buildEventListSection();
        evSection.setMinWidth(360); HBox.setHgrow(evSection, Priority.ALWAYS);

        content.getChildren().addAll(calSection, evSection);
        return content;
    }

    // ── Calendar section ──────────────────────────────────────────────────────

    private VBox buildCalendarSection() {
        VBox sec = new VBox(14);
        sec.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-padding: 20; " +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,3);");
        VBox.setVgrow(sec, Priority.ALWAYS);

        // Navigation
        HBox nav = new HBox(12); nav.setAlignment(Pos.CENTER);
        Button bPrev = navBtn("◀");
        bPrev.setOnAction(e -> { currentMonth = currentMonth.minusMonths(1); refreshCalendar(); });
        monthYearLabel = new Label();
        monthYearLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #2c3e50; -fx-min-width: 230;");
        monthYearLabel.setAlignment(Pos.CENTER);
        Button bNext = navBtn("▶");
        bNext.setOnAction(e -> { currentMonth = currentMonth.plusMonths(1); refreshCalendar(); });
        nav.getChildren().addAll(bPrev, monthYearLabel, bNext);

        // Days-of-week header
        GridPane dowHdr = new GridPane(); dowHdr.setHgap(5);
        String[] dows = {"Lun","Mar","Mer","Jeu","Ven","Sam","Dim"};
        for (int i = 0; i < 7; i++) {
            Label l = new Label(dows[i]);
            l.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #6c757d; " +
                    "-fx-pref-width: 76; -fx-alignment: center;");
            l.setAlignment(Pos.CENTER);
            dowHdr.add(l, i, 0);
        }

        calendarGrid = new GridPane();
        calendarGrid.setHgap(5); calendarGrid.setVgap(5);
        calendarGrid.setAlignment(Pos.CENTER);

        sec.getChildren().addAll(nav, new Separator(), dowHdr, calendarGrid);
        return sec;
    }

    private Button navBtn(String t) {
        Button b = new Button(t);
        b.setStyle("-fx-background-color: #f0f4ff; -fx-text-fill: #2c3e50; -fx-font-size: 15px; " +
                "-fx-padding: 7 16; -fx-background-radius: 8; -fx-cursor: hand;");
        return b;
    }

    // ── Event list section ────────────────────────────────────────────────────

    private VBox buildEventListSection() {
        VBox sec = new VBox(10);
        sec.setStyle("-fx-background-color: white; -fx-background-radius: 14; -fx-padding: 18; " +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,3);");
        VBox.setVgrow(sec, Priority.ALWAYS);

        Label title = new Label("📋 Événements du jour sélectionné");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        eventListPanel = new VBox(10);
        Label ph = new Label("Cliquez sur une date pour voir les événements");
        ph.setStyle("-fx-text-fill: #adb5bd; -fx-font-style: italic; -fx-padding: 12 0;");
        eventListPanel.getChildren().add(ph);

        ScrollPane scroll = new ScrollPane(eventListPanel);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        sec.getChildren().addAll(title, new Separator(), scroll);
        return sec;
    }

    // ── Legend ────────────────────────────────────────────────────────────────

    private HBox buildLegend() {
        HBox leg = new HBox(24);
        leg.setStyle("-fx-background-color: white; -fx-padding: 12 22; " +
                "-fx-border-color: #e9ecef; -fx-border-width: 1 0 0 0;");
        leg.setAlignment(Pos.CENTER);
        leg.getChildren().addAll(
            legItem("#dc3545", "Entretien planifié"),
            legItem("#28a745", "Entretien urgent (< 3j)"),
            legItem("#17a2b8", "Entretien cette semaine"),
            legItem("#ffc107", "Deadline offre proche"),
            legItem("#5BA3F5", "Deadline offre ce mois"),
            legItem("#6c757d", "Date passée")
        );
        return leg;
    }

    private HBox legItem(String color, String text) {
        HBox item = new HBox(6); item.setAlignment(Pos.CENTER_LEFT);
        Circle dot = new Circle(6); dot.setFill(Color.web(color));
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #495057;");
        item.getChildren().addAll(dot, lbl);
        return item;
    }

    // =========================================================================
    // Data loading
    // =========================================================================

    private void loadData() {
        offersByDate.clear(); interviewsByDate.clear();

        // Job offers
        try {
            for (JobOffer o : jobOfferService.getAllJobOffers())
                if (o.getDeadline() != null)
                    offersByDate.computeIfAbsent(o.getDeadline().toLocalDate(), k -> new ArrayList<>()).add(o);
        } catch (Exception e) { System.err.println("Offers load: " + e.getMessage()); }

        // Interviews
        try {
            List<Interview> interviews;
            if (UserContext.getRole() == UserContext.Role.RECRUITER)
                interviews = fetchInterviewsByRecruiter(UserContext.getRecruiterId());
            else
                interviews = InterviewService.getAll();

            for (Interview iv : interviews)
                if (iv.getScheduledAt() != null)
                    interviewsByDate.computeIfAbsent(iv.getScheduledAt().toLocalDate(), k -> new ArrayList<>()).add(iv);
        } catch (Exception e) { System.err.println("Interviews load: " + e.getMessage()); }
    }

    private List<Interview> fetchInterviewsByRecruiter(Long rid) {
        List<Interview> list = new ArrayList<>();
        if (rid == null) return InterviewService.getAll();
        String sql = "SELECT * FROM interview WHERE recruiter_id = ? ORDER BY scheduled_at ASC";
        try (PreparedStatement ps = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setLong(1, rid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Interview i = new Interview();
                i.setId(rs.getLong("id"));
                i.setApplicationId(rs.getLong("application_id"));
                i.setRecruiterId(rs.getLong("recruiter_id"));
                i.setScheduledAt(rs.getTimestamp("scheduled_at").toLocalDateTime());
                i.setDurationMinutes(rs.getInt("duration_minutes"));
                i.setMode(rs.getString("mode"));
                i.setMeetingLink(rs.getString("meeting_link"));
                i.setLocation(rs.getString("location"));
                i.setNotes(rs.getString("notes"));
                i.setStatus(rs.getString("status"));
                list.add(i);
            }
        } catch (SQLException e) { System.err.println("fetchByRecruiter: " + e.getMessage()); }
        return list;
    }

    // =========================================================================
    // Calendar rendering
    // =========================================================================

    private void refreshCalendar() {
        calendarGrid.getChildren().clear();

        String mName = currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        monthYearLabel.setText(mName.substring(0,1).toUpperCase() + mName.substring(1) + " " + currentMonth.getYear());

        int startDow = currentMonth.atDay(1).getDayOfWeek().getValue(); // 1=Mon
        int daysInMonth = currentMonth.lengthOfMonth();
        int dayNum = 1;

        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 7; col++) {
                int idx = row * 7 + col + 1;
                if (idx >= startDow && dayNum <= daysInMonth) {
                    calendarGrid.add(buildDayCell(currentMonth.atDay(dayNum)), col, row);
                    dayNum++;
                } else {
                    StackPane empty = new StackPane();
                    empty.setPrefSize(76, 70);
                    empty.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10;");
                    calendarGrid.add(empty, col, row);
                }
            }
        }
    }

    private StackPane buildDayCell(LocalDate date) {
        StackPane cell = new StackPane();
        cell.setPrefSize(76, 70);
        cell.setAlignment(Pos.TOP_CENTER);

        boolean isToday    = date.equals(LocalDate.now());
        boolean isSelected = date.equals(selectedDate);

        List<JobOffer>  offers     = visibleOffers(date);
        List<Interview> interviews = visibleInterviews(date);
        boolean hasOffers     = !offers.isEmpty();
        boolean hasInterviews = !interviews.isEmpty();

        String bg, border, bw;
        if (isSelected)    { bg = "#3d5a80"; border = "#2c3e50"; bw = "2"; }
        else if (isToday)  { bg = "#e8f4fd"; border = "#5BA3F5"; bw = "2"; }
        else               { bg = "white";   border = "#e9ecef"; bw = "1"; }

        cell.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 10; " +
                "-fx-border-color: " + border + "; -fx-border-radius: 10; " +
                "-fx-border-width: " + bw + "; -fx-cursor: hand;");

        VBox inner = new VBox(3);
        inner.setAlignment(Pos.TOP_CENTER);
        inner.setPadding(new Insets(6, 4, 4, 4));

        // Day number
        String numColor = isSelected ? "white" : isToday ? "#1976d2" : "#2c3e50";
        Label numLbl = new Label(String.valueOf(date.getDayOfMonth()));
        numLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: " + (isToday ? "700" : "500") +
                "; -fx-text-fill: " + numColor + ";");
        inner.getChildren().add(numLbl);

        // Dot indicators
        if (hasInterviews || hasOffers) {
            HBox dots = new HBox(3); dots.setAlignment(Pos.CENTER);

            // Interview dots (red shades)
            for (int i = 0; i < Math.min(2, interviews.size()); i++) {
                Circle d = new Circle(4);
                d.setFill(Color.web(interviewDotColor(date)));
                dots.getChildren().add(d);
            }
            if (interviews.size() > 2) {
                Label p = new Label("+" + (interviews.size()-2));
                p.setStyle("-fx-font-size: 8px; -fx-text-fill: #dc3545; -fx-font-weight:700;");
                dots.getChildren().add(p);
            }
            // Offer dots (blue shades)
            for (int i = 0; i < Math.min(2, offers.size()); i++) {
                Circle d = new Circle(4);
                d.setFill(Color.web(offerDotColor(date)));
                dots.getChildren().add(d);
            }
            if (offers.size() > 2) {
                Label p = new Label("+" + (offers.size()-2));
                p.setStyle("-fx-font-size: 8px; -fx-text-fill: #5BA3F5; -fx-font-weight:700;");
                dots.getChildren().add(p);
            }
            inner.getChildren().add(dots);

            // Mini count labels
            VBox counts = new VBox(1); counts.setAlignment(Pos.CENTER);
            String selFade = isSelected ? "rgba(255,255,255,0.90)" : null;
            if (hasInterviews) {
                Label l = new Label(interviews.size() + " entretien" + (interviews.size()>1?"s":""));
                l.setStyle("-fx-font-size: 8px; -fx-text-fill: " + (selFade!=null?selFade:"#dc3545") + "; -fx-font-weight:700;");
                counts.getChildren().add(l);
            }
            if (hasOffers) {
                Label l = new Label(offers.size() + " offre" + (offers.size()>1?"s":""));
                l.setStyle("-fx-font-size: 8px; -fx-text-fill: " + (selFade!=null?"rgba(255,255,255,0.75)":"#5BA3F5") + "; -fx-font-weight:700;");
                counts.getChildren().add(l);
            }
            inner.getChildren().add(counts);
        }
        cell.getChildren().add(inner);

        // Tooltip
        if (hasInterviews || hasOffers) {
            StringBuilder tip = new StringBuilder();
            if (hasInterviews) tip.append(interviews.size()).append(" entretien(s)\n");
            if (hasOffers)     tip.append(offers.size()).append(" deadline(s) offre\n");
            Tooltip.install(cell, new Tooltip(tip.toString().trim()));
        }

        // Hover
        cell.setOnMouseEntered(e -> {
            if (!isSelected) cell.setStyle("-fx-background-color: #eef5ff; -fx-background-radius: 10; " +
                    "-fx-border-color: #5BA3F5; -fx-border-radius: 10; -fx-border-width: 2; -fx-cursor: hand;");
        });
        cell.setOnMouseExited(e -> {
            if (!isSelected) cell.setStyle("-fx-background-color: " + (isToday?"#e8f4fd":"white") +
                    "; -fx-background-radius: 10; -fx-border-color: " + (isToday?"#5BA3F5":"#e9ecef") +
                    "; -fx-border-radius: 10; -fx-border-width: " + (isToday?"2":"1") + "; -fx-cursor: hand;");
        });

        cell.setOnMouseClicked(e -> {
            selectedDate = date;
            refreshCalendar();
            updateEventsList(date);
        });
        return cell;
    }

    // =========================================================================
    // Event list
    // =========================================================================

    private void updateEventsList(LocalDate date) {
        if (eventListPanel == null) return;
        eventListPanel.getChildren().clear();

        if (date == null) { addPlaceholder("Cliquez sur une date"); return; }

        String dateTxt = date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH));
        Label dateHdr = new Label("📅 " + dateTxt.substring(0,1).toUpperCase() + dateTxt.substring(1));
        dateHdr.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #3d5a80; -fx-padding: 0 0 4 0;");
        eventListPanel.getChildren().add(dateHdr);

        List<Interview> interviews = visibleInterviews(date);
        List<JobOffer>  offers     = visibleOffers(date);

        if (interviews.isEmpty() && offers.isEmpty()) { addPlaceholder("Aucun événement ce jour"); return; }

        if (!interviews.isEmpty()) {
            eventListPanel.getChildren().add(sectionHeader("🎤 Entretiens (" + interviews.size() + ")", "#dc3545"));
            for (Interview iv : interviews) eventListPanel.getChildren().add(buildInterviewCard(iv));
        }
        if (!offers.isEmpty()) {
            eventListPanel.getChildren().add(sectionHeader("💼 Deadlines offres (" + offers.size() + ")", "#5BA3F5"));
            for (JobOffer o : offers) eventListPanel.getChildren().add(buildOfferCard(o));
        }
    }

    private void addPlaceholder(String msg) {
        Label l = new Label(msg);
        l.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 13px; -fx-font-style: italic; -fx-padding: 16 0;");
        eventListPanel.getChildren().add(l);
    }

    private Label sectionHeader(String text, String color) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: " + color + "; -fx-padding: 6 0 2 0;");
        return l;
    }

    // ── Interview card (rich) ─────────────────────────────────────────────────

    private VBox buildInterviewCard(Interview iv) {
        VBox card = new VBox(8);
        String sc = statusColor(iv.getStatus());
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 14; " +
                "-fx-border-color: " + sc + "; -fx-border-radius: 10; -fx-border-width: 2; " +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");

        // Header row
        HBox hdr = new HBox(8); hdr.setAlignment(Pos.CENTER_LEFT);

        Label statusBadge = badge(fmtStatus(iv.getStatus()), sc, "white");
        Label modeBadge   = badge("ONLINE".equals(iv.getMode()) ? "🌐 En ligne" : "🏢 Sur site", "#e3f2fd", "#1565c0");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label timeLbl = new Label("🕐 " + iv.getScheduledAt().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeLbl.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");

        hdr.getChildren().addAll(statusBadge, modeBadge, sp, timeLbl);

        // Candidate & offer
        String[] info = candidateInfo(iv.getApplicationId());
        Label candidateLbl = new Label("👤 " + info[0]);
        candidateLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        Label offerLbl = new Label("💼 " + info[1]);
        offerLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #495057;");
        offerLbl.setWrapText(true);

        // Duration
        Label durLbl = new Label("⏱ Durée : " + iv.getDurationMinutes() + " min");
        durLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d;");

        card.getChildren().addAll(hdr, candidateLbl, offerLbl, durLbl);

        // Meeting link / location
        if ("ONLINE".equals(iv.getMode()) && iv.getMeetingLink() != null && !iv.getMeetingLink().isBlank()) {
            HBox linkRow = new HBox(6); linkRow.setAlignment(Pos.CENTER_LEFT);
            Label linkLbl = new Label("🔗 " + iv.getMeetingLink());
            linkLbl.setStyle("-fx-text-fill: #1565c0; -fx-font-size: 11px; -fx-underline: true;");
            linkLbl.setMaxWidth(280);
            linkRow.getChildren().add(linkLbl);
            card.getChildren().add(linkRow);
        } else if ("ON_SITE".equals(iv.getMode()) && iv.getLocation() != null && !iv.getLocation().isBlank()) {
            Label locLbl = new Label("📍 " + iv.getLocation());
            locLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #495057;");
            card.getChildren().add(locLbl);
        }

        // Notes snippet
        if (iv.getNotes() != null && !iv.getNotes().isBlank()) {
            String n = iv.getNotes().length() > 90 ? iv.getNotes().substring(0, 90) + "…" : iv.getNotes();
            Label notesLbl = new Label("📝 " + n);
            notesLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d; -fx-font-style: italic;");
            notesLbl.setWrapText(true);
            card.getChildren().add(notesLbl);
        }

        // Urgency countdown
        long hoursUntil = ChronoUnit.HOURS.between(LocalDateTime.now(), iv.getScheduledAt());
        if (hoursUntil > 0 && hoursUntil <= 48) {
            String urgText = hoursUntil < 24
                    ? "🔥 Dans " + hoursUntil + "h — Préparez-vous !"
                    : "⚡ Demain à " + iv.getScheduledAt().format(DateTimeFormatter.ofPattern("HH:mm"));
            Label urgLbl = new Label(urgText);
            urgLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #dc3545; " +
                    "-fx-background-color: #fff3f3; -fx-padding: 4 10; -fx-background-radius: 6;");
            card.getChildren().add(urgLbl);
        }
        return card;
    }

    // ── Job offer card ────────────────────────────────────────────────────────

    private VBox buildOfferCard(JobOffer offer) {
        VBox card = new VBox(8);
        String dc = offerDotColor(offer.getDeadline().toLocalDate());
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-padding: 12; " +
                "-fx-border-color: " + dc + "; -fx-border-radius: 10; -fx-border-width: 1.5; " +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.05),6,0,0,2);");

        HBox hdr = new HBox(8); hdr.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl = new Label(offer.getTitle());
        titleLbl.setStyle("-fx-font-size: 13px; -fx-font-weight: 700; -fx-text-fill: #2c3e50;");
        titleLbl.setWrapText(true); HBox.setHgrow(titleLbl, Priority.ALWAYS);
        Label ctBadge = badge(fmtContract(offer.getContractType()), "#e3f2fd", "#1565c0");
        hdr.getChildren().addAll(titleLbl, ctBadge);

        Label locLbl = new Label("📍 " + (offer.getLocation()!=null?offer.getLocation():"Non spécifié"));
        locLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6c757d;");

        long days = ChronoUnit.DAYS.between(LocalDate.now(), offer.getDeadline().toLocalDate());
        String txt;
        if      (days < 0)  txt = "⏰ Expirée il y a " + Math.abs(days) + " jour(s)";
        else if (days == 0) txt = "🔥 Deadline aujourd'hui !";
        else if (days == 1) txt = "⚡ Deadline demain !";
        else                txt = "📅 " + days + " jour(s) restant(s)";

        Label dlLbl = new Label(txt);
        dlLbl.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + dc + ";");

        card.getChildren().addAll(hdr, locLbl, dlLbl);
        return card;
    }

    // =========================================================================
    // DB helpers
    // =========================================================================

    /** Returns [candidateName, jobOfferTitle] from application id */
    private String[] candidateInfo(Long appId) {
        if (appId == null) return new String[]{"Candidat inconnu", "Offre inconnue"};
        String sql = """
            SELECT u.first_name, u.last_name, jo.title
            FROM job_application ja
            JOIN users u   ON ja.candidate_id = u.id
            JOIN job_offer jo ON ja.offer_id  = jo.id
            WHERE ja.id = ?
            """;
        try (PreparedStatement ps = MyDatabase.getInstance().getConnection().prepareStatement(sql)) {
            ps.setLong(1, appId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String name  = (rs.getString("first_name") + " " + rs.getString("last_name")).trim();
                String title = rs.getString("title");
                return new String[]{
                    name.isEmpty()  ? "Candidat #" + appId  : name,
                    title != null ? title : "Offre #" + appId
                };
            }
        } catch (SQLException e) { System.err.println("candidateInfo: " + e.getMessage()); }
        return new String[]{"Candidat #" + appId, "Candidature #" + appId};
    }

    // =========================================================================
    // Filters
    // =========================================================================

    private List<JobOffer>  visibleOffers(LocalDate d) {
        if (currentMode == ViewMode.INTERVIEWS) return Collections.emptyList();
        return offersByDate.getOrDefault(d, Collections.emptyList());
    }

    private List<Interview> visibleInterviews(LocalDate d) {
        if (currentMode == ViewMode.OFFERS) return Collections.emptyList();
        return interviewsByDate.getOrDefault(d, Collections.emptyList());
    }

    // =========================================================================
    // Color / formatting helpers
    // =========================================================================

    private String interviewDotColor(LocalDate d) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), d);
        if (days < 0)  return "#6c757d";
        if (days <= 1) return "#dc3545";
        if (days <= 3) return "#28a745";
        return "#17a2b8";
    }

    private String offerDotColor(LocalDate d) {
        long days = ChronoUnit.DAYS.between(LocalDate.now(), d);
        if (days < 0)  return "#6c757d";
        if (days <= 3) return "#ffc107";
        if (days <= 7) return "#fd7e14";
        return "#5BA3F5";
    }

    private String statusColor(String s) {
        if (s == null) return "#6c757d";
        return switch (s) {
            case "SCHEDULED" -> "#dc3545";
            case "DONE"      -> "#28a745";
            case "CANCELLED" -> "#6c757d";
            default          -> "#17a2b8";
        };
    }

    private String fmtStatus(String s) {
        if (s == null) return "INCONNU";
        return switch (s) {
            case "SCHEDULED" -> "Planifié";
            case "DONE"      -> "Terminé";
            case "CANCELLED" -> "Annulé";
            default          -> s;
        };
    }

    private String fmtContract(Models.joboffers.ContractType t) {
        if (t == null) return "N/A";
        return switch (t) {
            case CDI        -> "CDI";
            case CDD        -> "CDD";
            case INTERNSHIP -> "Stage";
            case FREELANCE  -> "Freelance";
            case PART_TIME  -> "Temps partiel";
            case FULL_TIME  -> "Temps plein";
        };
    }

    private Label badge(String text, String bg, String fg) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
                "-fx-padding: 3 10; -fx-background-radius: 12; -fx-font-size: 10px; -fx-font-weight: 700;");
        return l;
    }
}








