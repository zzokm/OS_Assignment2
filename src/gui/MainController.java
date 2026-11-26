package gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.converter.IntegerStringConverter;
import metrics.SchedulingMetrics;
import model.Process;
import model.TimeStepRecord;
import output.TraceTableFormatter;
import scheduler.SJFScheduler;
import scheduler.Scheduler;
import scheduler.acs.ACSScheduler;

public class MainController {

    public static class ProcessRow {
        private final SimpleStringProperty id;
        private final SimpleIntegerProperty at;
        private final SimpleIntegerProperty bt;
        private final SimpleIntegerProperty pri;

        public ProcessRow(String id, int at, int bt, int pri) {
            this.id = new SimpleStringProperty(id);
            this.at = new SimpleIntegerProperty(at);
            this.bt = new SimpleIntegerProperty(bt);
            this.pri = new SimpleIntegerProperty(pri);
        }

        public String getId() { return id.get(); }
        public void setId(String v) { id.set(v); }
        public int getAt() { return at.get(); }
        public void setAt(int v) { at.set(v); }
        public int getBt() { return bt.get(); }
        public void setBt(int v) { bt.set(v); }
        public int getPri() { return pri.get(); }
        public void setPri(int v) { pri.set(v); }
    }

    @FXML private ComboBox<String> algorithmBox;
    @FXML private Spinner<Integer> quantumSpinner;
    @FXML private Spinner<Integer> csSpinner;

    @FXML private TableView<ProcessRow> processTable;
    @FXML private TableColumn<ProcessRow, String> colId;
    @FXML private TableColumn<ProcessRow, Integer> colAt;
    @FXML private TableColumn<ProcessRow, Integer> colBt;
    @FXML private TableColumn<ProcessRow, Integer> colPri;

    @FXML private Button playPauseButton;
    @FXML private Slider speedSlider;
    @FXML private VBox visualContainer;
    @FXML private TextArea traceArea;

    @FXML private TableView<Process> metricsTable;
    @FXML private TableColumn<Process, String> mColId;
    @FXML private TableColumn<Process, Number> mColAt;
    @FXML private TableColumn<Process, Number> mColBt;
    @FXML private TableColumn<Process, Number> mColFt;
    @FXML private TableColumn<Process, Number> mColTat;
    @FXML private TableColumn<Process, Number> mColWt;
    @FXML private TableColumn<Process, Number> mColRt;

    @FXML private Label avgTatLabel;
    @FXML private Label avgWtLabel;
    @FXML private Label avgRtLabel;

    private final ObservableList<ProcessRow> processRows = FXCollections.observableArrayList();
    private final GanttChartPanel ganttChart = new GanttChartPanel();
    private final QueueVisualizer queueVisualizer = new QueueVisualizer();

    private List<TimeStepRecord> acsTrace;
    private List<Scheduler.TimeStepRecord> sjfTrace;
    private SchedulingMetrics lastMetrics;
    private boolean isAcsMode = true;
    private int currentStepIndex = -1;
    private Timeline playTimeline;
    private String lastSimulatedConfigSignature;

    @FXML
    public void initialize() {
        algorithmBox.setItems(FXCollections.observableArrayList(
                "Adaptive Container Scheduler (ACS)",
                "Preemptive SJF (SRTF)"
        ));
        algorithmBox.getSelectionModel().selectFirst();

        quantumSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 3));
        csSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 50, 1));

        quantumSpinner.valueProperty().addListener((obs, oldVal, newVal) -> stopPlayback());
        csSpinner.valueProperty().addListener((obs, oldVal, newVal) -> stopPlayback());
        algorithmBox.valueProperty().addListener((obs, oldVal, newVal) -> stopPlayback());

        setupProcessTable();
        setupMetricsTable();

        visualContainer.getChildren().addAll(ganttChart, queueVisualizer);
        handleLoadTc2();
    }

    private void setupProcessTable() {
        processTable.setItems(processRows);

        colId.setCellValueFactory(data -> data.getValue().id);
        colId.setCellFactory(TextFieldTableCell.forTableColumn());
        colId.setOnEditCommit(e -> {
            stopPlayback();
            e.getRowValue().setId(e.getNewValue());
        });

        colAt.setCellValueFactory(data -> data.getValue().at.asObject());
        colAt.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colAt.setOnEditCommit(e -> {
            stopPlayback();
            e.getRowValue().setAt(e.getNewValue());
        });

        colBt.setCellValueFactory(data -> data.getValue().bt.asObject());
        colBt.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colBt.setOnEditCommit(e -> {
            stopPlayback();
            e.getRowValue().setBt(e.getNewValue());
        });

        colPri.setCellValueFactory(data -> data.getValue().pri.asObject());
        colPri.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colPri.setOnEditCommit(e -> {
            stopPlayback();
            e.getRowValue().setPri(e.getNewValue());
        });
    }

    private void setupMetricsTable() {
        mColId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        mColAt.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getArrivalTime()));
        mColBt.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getBurstTime()));
        mColFt.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getFinishTime()));
        mColTat.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getTurnaroundTime()));
        mColWt.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getWaitingTime()));
        mColRt.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getResponseTime()));
    }

    private String getCurrentConfigSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(algorithmBox.getSelectionModel().getSelectedIndex()).append(";");
        sb.append(quantumSpinner.getValue()).append(";");
        sb.append(csSpinner.getValue()).append(";");
        for (ProcessRow row : processRows) {
            String id = row.getId() == null ? "" : row.getId().trim();
            sb.append(id).append(",")
              .append(row.getAt()).append(",")
              .append(row.getBt()).append(",")
              .append(row.getPri()).append(";");
        }
        return sb.toString();
    }

    @FXML
    private void handleAddProcess() {
        stopPlayback();
        int next = processRows.size() + 1;
        processRows.add(new ProcessRow("P" + next, 0, 4, 1));
    }

    @FXML
    private void handleRemoveProcess() {
        stopPlayback();
        int selected = processTable.getSelectionModel().getSelectedIndex();
        if (selected >= 0) {
            processRows.remove(selected);
        } else if (!processRows.isEmpty()) {
            processRows.remove(processRows.size() - 1);
        }
    }

    @FXML
    private void handleLoadTc1() {
        stopPlayback();
        processRows.setAll(
                new ProcessRow("P1", 0, 4, 2),
                new ProcessRow("P2", 1, 7, 3),
                new ProcessRow("P3", 3, 2, 1)
        );
        quantumSpinner.getValueFactory().setValue(3);
        csSpinner.getValueFactory().setValue(0);
        resetSimulationState();
    }

    @FXML
    private void handleLoadTc2() {
        stopPlayback();
        processRows.setAll(
                new ProcessRow("P1", 0, 5, 4),
                new ProcessRow("P2", 1, 6, 2),
                new ProcessRow("P3", 4, 4, 3)
        );
        quantumSpinner.getValueFactory().setValue(3);
        csSpinner.getValueFactory().setValue(1);
        resetSimulationState();
    }

    @FXML
    private void handleRunAll() {
        stopPlayback();
        if (!prepareSimulation()) {
            return;
        }
        currentStepIndex = getTotalSteps() - 1;
        updateVisualsForCurrentStep();
        showFinalMetrics();
    }

    @FXML
    private void handleStep() {
        stopPlayback();
        String currentSig = getCurrentConfigSignature();
        boolean configChanged = lastSimulatedConfigSignature == null || !currentSig.equals(lastSimulatedConfigSignature);

        if (configChanged || (acsTrace == null && sjfTrace == null)) {
            if (!prepareSimulation()) {
                return;
            }
            currentStepIndex = -1;
        }

        if (currentStepIndex + 1 < getTotalSteps()) {
            currentStepIndex++;
            updateVisualsForCurrentStep();
            if (currentStepIndex == getTotalSteps() - 1) {
                showFinalMetrics();
            }
        }
    }

    @FXML
    private void handlePlayPause() {
        String currentSig = getCurrentConfigSignature();
        boolean configChanged = lastSimulatedConfigSignature == null || !currentSig.equals(lastSimulatedConfigSignature);

        if (configChanged) {
            stopPlayback();
            if (!prepareSimulation()) {
                return;
            }
            currentStepIndex = -1;
        } else {
            if (playTimeline != null) {
                stopPlayback();
                return;
            }
            if (acsTrace == null && sjfTrace == null) {
                if (!prepareSimulation()) {
                    return;
                }
                currentStepIndex = -1;
            }
        }

        if (currentStepIndex >= getTotalSteps() - 1) {
            currentStepIndex = -1;
        }

        startPlayback();
    }

    private void startPlayback() {
        playPauseButton.setText("Pause");

        if (currentStepIndex < 0 && getTotalSteps() > 0) {
            currentStepIndex = 0;
            updateVisualsForCurrentStep();
            if (currentStepIndex == getTotalSteps() - 1) {
                showFinalMetrics();
                stopPlayback();
                return;
            }
        }

        double delayMs = 1100.0 - speedSlider.getValue();
        playTimeline = new Timeline(new KeyFrame(Duration.millis(Math.max(80, delayMs)), e -> {
            if (currentStepIndex + 1 < getTotalSteps()) {
                currentStepIndex++;
                updateVisualsForCurrentStep();
                if (currentStepIndex == getTotalSteps() - 1) {
                    showFinalMetrics();
                    stopPlayback();
                }
            } else {
                showFinalMetrics();
                stopPlayback();
            }
        }));
        playTimeline.setCycleCount(Timeline.INDEFINITE);
        playTimeline.play();
    }

    @FXML
    private void handleReset() {
        resetSimulationState();
    }

    private void resetSimulationState() {
        stopPlayback();
        acsTrace = null;
        sjfTrace = null;
        lastMetrics = null;
        lastSimulatedConfigSignature = null;
        currentStepIndex = -1;
        ganttChart.clear();
        queueVisualizer.clear();
        traceArea.clear();
        metricsTable.getItems().clear();
        avgTatLabel.setText("Avg Turnaround Time: -");
        avgWtLabel.setText("Avg Waiting Time: -");
        avgRtLabel.setText("Avg Response Time: -");
    }

    private void stopPlayback() {
        if (playTimeline != null) {
            playTimeline.stop();
            playTimeline = null;
        }
        playPauseButton.setText("Play");
    }

    private boolean prepareSimulation() {
        List<Process> workload = readWorkload();
        if (workload.isEmpty()) {
            return false;
        }

        try {
            int q = quantumSpinner.getValue();
            int cs = csSpinner.getValue();
            isAcsMode = algorithmBox.getSelectionModel().getSelectedIndex() == 0;

            if (isAcsMode) {
                ACSScheduler acs = new ACSScheduler(q, cs);
                acs.loadProcesses(workload);
                acs.runSimulation();
                acsTrace = acs.getExecutionTrace();
                sjfTrace = null;
                lastMetrics = acs.getMetrics();
                traceArea.setText(TraceTableFormatter.formatAcsTrace(acsTrace));
            } else {
                SJFScheduler sjf = new SJFScheduler(cs);
                sjf.loadProcesses(workload);
                sjf.runSimulation();
                @SuppressWarnings("unchecked")
                List<Scheduler.TimeStepRecord> trace = (List<Scheduler.TimeStepRecord>) sjf.getExecutionTrace();
                sjfTrace = trace;
                acsTrace = null;
                lastMetrics = sjf.getMetrics();
                traceArea.setText(TraceTableFormatter.formatSjfTrace(sjfTrace));
            }
            lastSimulatedConfigSignature = getCurrentConfigSignature();
            return true;
        } catch (Exception ex) {
            showError("Simulation Error", ex.getMessage());
            return false;
        }
    }

    private int getTotalSteps() {
        if (isAcsMode && acsTrace != null) {
            return Math.max(0, acsTrace.size() - 1);
        }
        if (!isAcsMode && sjfTrace != null) {
            return sjfTrace.size();
        }
        return 0;
    }

    private void updateVisualsForCurrentStep() {
        if (isAcsMode && acsTrace != null && currentStepIndex >= 0) {
            ganttChart.setAcsTrace(acsTrace, currentStepIndex);
            TimeStepRecord rec = acsTrace.get(currentStepIndex);
            queueVisualizer.updateState(rec.time(), rec.runningProcessId(),
                    rec.tier1Queue(), rec.tier2Queue());
        } else if (!isAcsMode && sjfTrace != null && currentStepIndex >= 0) {
            ganttChart.setSjfTrace(sjfTrace, currentStepIndex);
            Scheduler.TimeStepRecord rec = sjfTrace.get(currentStepIndex);
            String cpu = rec.activity() == Scheduler.Activity.CONTEXT_SWITCH ? "CS"
                    : (rec.activity() == Scheduler.Activity.IDLE ? "IDLE" : rec.processId());
            queueVisualizer.updateState(rec.time(), cpu, List.of(), List.of());
        }
    }

    private void showFinalMetrics() {
        if (lastMetrics == null) {
            return;
        }
        metricsTable.setItems(FXCollections.observableArrayList(lastMetrics.getProcesses()));
        avgTatLabel.setText(String.format(Locale.US, "Avg Turnaround Time: %.2f",
                lastMetrics.getAverageTurnaroundTime()));
        avgWtLabel.setText(String.format(Locale.US, "Avg Waiting Time: %.2f",
                lastMetrics.getAverageWaitingTime()));
        avgRtLabel.setText(String.format(Locale.US, "Avg Response Time: %.2f",
                lastMetrics.getAverageResponseTime()));
    }

    private List<Process> readWorkload() {
        List<Process> list = new ArrayList<>();
        for (int i = 0; i < processRows.size(); i++) {
            ProcessRow row = processRows.get(i);
            try {
                list.add(new Process(row.getId().trim(), row.getAt(), row.getBt(), row.getPri()));
            } catch (Exception ex) {
                showError("Invalid Process Row " + (i + 1), ex.getMessage());
                return List.of();
            }
        }
        if (list.isEmpty()) {
            showError("Empty Workload", "Please add at least one process.");
        }
        return list;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
