package gui;

import java.util.List;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import model.Process;


public class QueueVisualizer extends VBox {

    private final Label cpuStatusLabel;
    private final ListView<String> tier1List;
    private final ListView<String> tier2List;

    public QueueVisualizer() {
        setSpacing(8);
        setPadding(new Insets(6));

        cpuStatusLabel = new Label("CPU: IDLE   |   Time: -");
        cpuStatusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

        tier1List = new ListView<>();
        tier1List.setPrefHeight(95);
        TitledPane tier1Pane = new TitledPane("Tier 1 Queue (Round Robin)", tier1List);
        tier1Pane.setCollapsible(false);

        tier2List = new ListView<>();
        tier2List.setPrefHeight(95);
        TitledPane tier2Pane = new TitledPane("Tier 2 Queue (Priority + Aging)", tier2List);
        tier2Pane.setCollapsible(false);

        HBox queuesRow = new HBox(10, tier1Pane, tier2Pane);
        HBox.setHgrow(tier1Pane, Priority.ALWAYS);
        HBox.setHgrow(tier2Pane, Priority.ALWAYS);

        getChildren().addAll(cpuStatusLabel, queuesRow);
    }

    public void updateState(int time, String cpuProcess, List<Process> tier1, List<Process> tier2) {
        cpuStatusLabel.setText("CPU: " + cpuProcess + "   |   Time: " + time);

        if (tier1 == null || tier1.isEmpty()) {
            tier1List.setItems(FXCollections.observableArrayList("[Empty]"));
        } else {
            tier1List.setItems(FXCollections.observableArrayList(
                    tier1.stream()
                            .map(p -> p.getId() + " (rem: " + p.getRemainingBurstTime()
                                    + ", pri: " + p.getEffectivePriority() + ")")
                            .toList()
            ));
        }

        if (tier2 == null || tier2.isEmpty()) {
            tier2List.setItems(FXCollections.observableArrayList("[Empty]"));
        } else {
            tier2List.setItems(FXCollections.observableArrayList(
                    tier2.stream()
                            .map(p -> p.getId() + " (rem: " + p.getRemainingBurstTime()
                                    + ", effPri: " + p.getEffectivePriority() + ")")
                            .toList()
            ));
        }
    }

    public void clear() {
        cpuStatusLabel.setText("CPU: IDLE   |   Time: -");
        tier1List.getItems().clear();
        tier2List.getItems().clear();
    }
}
