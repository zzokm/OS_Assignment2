package gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import model.TimeStepRecord;
import scheduler.Scheduler;


public class GanttChartPanel extends ScrollPane {

    private static final double BLOCK_WIDTH = 36.0;
    private static final double BAR_HEIGHT = 42.0;
    private static final double LEFT_PAD = 18.0;
    private static final double TOP_PAD = 22.0;

    private static final Color[] PALETTE = {
        Color.web("#4e79a7"),
        Color.web("#59a14f"),
        Color.web("#f28e2b"),
        Color.web("#e15759"),
        Color.web("#76b7b2"),
        Color.web("#edc948"),
        Color.web("#b07aa1")
    };

    public static class Slot {
        public final int time;
        public final String label; // Process ID, "CS", or "IDLE"

        public Slot(int time, String label) {
            this.time = time;
            this.label = label;
        }
    }

    private final Canvas canvas;
    private final List<Slot> slots = new ArrayList<>();
    private final Map<String, Color> processColors = new HashMap<>();
    private int currentStep = -1;

    public GanttChartPanel() {
        canvas = new Canvas(620, 120);
        setContent(canvas);
        setFitToHeight(true);
        setPrefHeight(135);
        setStyle("-fx-background-color: white; -fx-border-color: #cccccc;");
        redraw();
    }

    public void setAcsTrace(List<TimeStepRecord> trace, int currentStep) {
        slots.clear();
        if (trace != null) {
            // Skip the last IDLE snapshot if it is just the completion marker
            int limit = trace.size();
            if (limit > 1 && "IDLE".equals(trace.get(limit - 1).runningProcessId())) {
                limit--;
            }
            for (int i = 0; i < limit; i++) {
                TimeStepRecord r = trace.get(i);
                slots.add(new Slot(r.time(), r.runningProcessId()));
            }
        }
        this.currentStep = currentStep;
        redraw();
    }

    public void setSjfTrace(List<Scheduler.TimeStepRecord> trace, int currentStep) {
        slots.clear();
        if (trace != null) {
            for (Scheduler.TimeStepRecord r : trace) {
                String label;
                if (r.activity() == Scheduler.Activity.CONTEXT_SWITCH) {
                    label = "CS";
                } else if (r.activity() == Scheduler.Activity.IDLE) {
                    label = "IDLE";
                } else {
                    label = r.processId();
                }
                slots.add(new Slot(r.time(), label));
            }
        }
        this.currentStep = currentStep;
        redraw();
    }

    public void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
        redraw();
    }

    public void clear() {
        slots.clear();
        processColors.clear();
        currentStep = -1;
        redraw();
    }

    private void redraw() {
        double neededWidth = Math.max(620, LEFT_PAD * 2 + slots.size() * BLOCK_WIDTH + 20);
        canvas.setWidth(neededWidth);

        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        if (slots.isEmpty()) {
            g.setFill(Color.GRAY);
            g.setFont(Font.font("System", 12));
            g.fillText("Run a simulation to view the Gantt chart.", 20, 62);
            return;
        }

        int visibleCount = (currentStep < 0) ? slots.size() : Math.min(currentStep + 1, slots.size());

        for (int i = 0; i < visibleCount; i++) {
            Slot s = slots.get(i);
            double x = LEFT_PAD + i * BLOCK_WIDTH;
            double y = TOP_PAD;

            if ("CS".equals(s.label)) {
                g.setFill(Color.LIGHTGRAY);
                g.fillRect(x, y, BLOCK_WIDTH, BAR_HEIGHT);
                g.setStroke(Color.GRAY);
                g.strokeLine(x, y, x + BLOCK_WIDTH, y + BAR_HEIGHT);
                g.strokeLine(x + BLOCK_WIDTH, y, x, y + BAR_HEIGHT);
            } else if ("IDLE".equals(s.label)) {
                g.setFill(Color.web("#f4f4f4"));
                g.fillRect(x, y, BLOCK_WIDTH, BAR_HEIGHT);
            } else {
                g.setFill(getColorForProcess(s.label));
                g.fillRect(x, y, BLOCK_WIDTH, BAR_HEIGHT);
            }

            g.setStroke(Color.web("#333333"));
            g.strokeRect(x, y, BLOCK_WIDTH, BAR_HEIGHT);

            // Label inside box
            g.setFill("CS".equals(s.label) || "IDLE".equals(s.label) ? Color.DARKSLATEGRAY : Color.WHITE);
            g.setFont(Font.font("System", 11));
            g.fillText(s.label, x + 7, y + 25);

            // Time tick below box
            g.setFill(Color.web("#444444"));
            g.setFont(Font.font("System", 10));
            g.fillText(String.valueOf(s.time), x - 2, y + BAR_HEIGHT + 15);
        }

        // Final end time tick
        double endX = LEFT_PAD + visibleCount * BLOCK_WIDTH;
        int endTime = slots.get(visibleCount - 1).time + 1;
        g.setFill(Color.web("#444444"));
        g.setFont(Font.font("System", 10));
        g.fillText(String.valueOf(endTime), endX - 2, TOP_PAD + BAR_HEIGHT + 15);

        // Playhead cursor when stepping
        if (currentStep >= 0 && currentStep < slots.size()) {
            double cursorX = LEFT_PAD + (currentStep + 1) * BLOCK_WIDTH;
            g.setStroke(Color.RED);
            g.setLineWidth(2.0);
            g.strokeLine(cursorX, TOP_PAD - 8, cursorX, TOP_PAD + BAR_HEIGHT + 4);
            g.setLineWidth(1.0);
        }
    }

    private Color getColorForProcess(String id) {
        return processColors.computeIfAbsent(id,
                k -> PALETTE[processColors.size() % PALETTE.length]);
    }
}
