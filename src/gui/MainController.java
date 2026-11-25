package gui;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;
import metrics.SchedulingMetrics;
import model.Process;
import output.TraceTableFormatter;
import scheduler.SJFScheduler;
import scheduler.acs.ACSScheduler;

/**
 * Controller for MainView.fxml generated to match Scene Builder visual structure.
 */
public class MainController implements Initializable {
    private static final String ALGO_ACS = "Adaptive Container Scheduler (ACS)";
    private static final String ALGO_SJF = "Preemptive Shortest-Job First (SJF)";

    @FXML private ComboBox<String> algorithmComboBox;
    @FXML private Spinner<Integer> quantumSpinner;
    @FXML private Spinner<Integer> contextSwitchSpinner;

    @FXML private TableView<ProcessRow> processTable;
    @FXML private TableColumn<ProcessRow, String> procIdCol;
    @FXML private TableColumn<ProcessRow, Integer> procAtCol;
    @FXML private TableColumn<ProcessRow, Integer> procBtCol;
    @FXML private TableColumn<ProcessRow, Integer> procPriCol;

    @FXML private TextArea traceTextArea;

    @FXML private TableView<MetricsRow> metricsTable;
    @FXML private TableColumn<MetricsRow, String> mProcIdCol;
    @FXML private TableColumn<MetricsRow, Integer> mAtCol;
    @FXML private TableColumn<MetricsRow, Integer> mBtCol;
    @FXML private TableColumn<MetricsRow, Integer> mFtCol;
    @FXML private TableColumn<MetricsRow, Integer> mTatCol;
    @FXML private TableColumn<MetricsRow, Integer> mWtCol;
    @FXML private TableColumn<MetricsRow, Integer> mRtCol;

    @FXML private Label avgTatLabel;
    @FXML private Label avgWtLabel;
    @FXML private Label avgRtLabel;

    private final ObservableList<ProcessRow> processRows = FXCollections.observableArrayList();
    private final ObservableList<MetricsRow> metricsRows = FXCollections.observableArrayList();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Setup algorithm combo
        algorithmComboBox.setItems(FXCollections.observableArrayList(ALGO_ACS, ALGO_SJF));
        algorithmComboBox.setValue(ALGO_ACS);

        // Setup spinners
        quantumSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 3));
        contextSwitchSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 50, 1));

        // Setup process table
        procIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        procAtCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));
        procBtCol.setCellValueFactory(new PropertyValueFactory<>("burstTime"));
        procPriCol.setCellValueFactory(new PropertyValueFactory<>("initialPriority"));

        procIdCol.setCellFactory(TextFieldTableCell.forTableColumn());
        procIdCol.setOnEditCommit(e -> e.getRowValue().setId(e.getNewValue()));

        procAtCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        procAtCol.setOnEditCommit(e -> e.getRowValue().setArrivalTime(e.getNewValue()));

        procBtCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        procBtCol.setOnEditCommit(e -> e.getRowValue().setBurstTime(e.getNewValue()));

        procPriCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        procPriCol.setOnEditCommit(e -> e.getRowValue().setInitialPriority(e.getNewValue()));

        processTable.setItems(processRows);

        // Setup metrics table
        mProcIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        mAtCol.setCellValueFactory(new PropertyValueFactory<>("arrivalTime"));
        mBtCol.setCellValueFactory(new PropertyValueFactory<>("burstTime"));
        mFtCol.setCellValueFactory(new PropertyValueFactory<>("finishTime"));
        mTatCol.setCellValueFactory(new PropertyValueFactory<>("turnaroundTime"));
        mWtCol.setCellValueFactory(new PropertyValueFactory<>("waitingTime"));
        mRtCol.setCellValueFactory(new PropertyValueFactory<>("responseTime"));

        metricsTable.setItems(metricsRows);

        // Load default Test Case 2
        handleLoadTestCase2();
    }

    @FXML
    private void handleAddRow() {
        int next = processRows.size() + 1;
        processRows.add(new ProcessRow("P" + next, 0, 4, 1));
    }

    @FXML
    private void handleRemoveRow() {
        int idx = processTable.getSelectionModel().getSelectedIndex();
        if (idx >= 0) {
            processRows.remove(idx);
        } else if (!processRows.isEmpty()) {
            processRows.remove(processRows.size() - 1);
        }
    }

    @FXML
    private void handleLoadTestCase1() {
        algorithmComboBox.setValue(ALGO_ACS);
        quantumSpinner.getValueFactory().setValue(3);
        contextSwitchSpinner.getValueFactory().setValue(0);
        processRows.clear();
        processRows.add(new ProcessRow("P1", 0, 4, 2));
        processRows.add(new ProcessRow("P2", 1, 7, 3));
        processRows.add(new ProcessRow("P3", 3, 2, 1));
        handleReset();
    }

    @FXML
    private void handleLoadTestCase2() {
        algorithmComboBox.setValue(ALGO_ACS);
        quantumSpinner.getValueFactory().setValue(3);
        contextSwitchSpinner.getValueFactory().setValue(1);
        processRows.clear();
        processRows.add(new ProcessRow("P1", 0, 5, 4));
        processRows.add(new ProcessRow("P2", 1, 6, 2));
        processRows.add(new ProcessRow("P3", 4, 4, 3));
        handleReset();
    }

    @FXML
    private void handleReset() {
        traceTextArea.clear();
        metricsRows.clear();
        avgTatLabel.setText("-");
        avgWtLabel.setText("-");
        avgRtLabel.setText("-");
    }

    @FXML
    private void handleRun() {
        if (processRows.isEmpty()) {
            showAlert("No Processes", "Please add at least one process to simulate.");
            return;
        }

        List<Process> workload = new ArrayList<>();
        for (int i = 0; i < processRows.size(); i++) {
            ProcessRow r = processRows.get(i);
            try {
                workload.add(new Process(r.getId(), r.getArrivalTime(), r.getBurstTime(), r.getInitialPriority()));
            } catch (Exception ex) {
                showAlert("Invalid Process Data", "Error at row " + (i + 1) + ": " + ex.getMessage());
                return;
            }
        }

        try {
            int q = quantumSpinner.getValue();
            int cs = contextSwitchSpinner.getValue();
            String algo = algorithmComboBox.getValue();

            SchedulingMetrics metrics;
            String traceText;

            if (ALGO_ACS.equals(algo)) {
                ACSScheduler acs = new ACSScheduler(q, cs);
                acs.loadProcesses(workload);
                acs.runSimulation();
                metrics = acs.getMetrics();
                traceText = TraceTableFormatter.formatAcsTrace(acs.getExecutionTrace());
            } else {
                SJFScheduler sjf = new SJFScheduler(cs);
                sjf.loadProcesses(workload);
                sjf.runSimulation();
                metrics = sjf.getMetrics();
                traceText = TraceTableFormatter.formatSjfTrace(sjf.getExecutionTrace());
            }

            traceTextArea.setText(traceText);

            metricsRows.clear();
            for (Process p : metrics.getProcesses()) {
                metricsRows.add(new MetricsRow(
                        p.getId(),
                        p.getArrivalTime(),
                        p.getBurstTime(),
                        p.getFinishTime(),
                        p.getTurnaroundTime(),
                        p.getWaitingTime(),
                        p.getResponseTime()
                ));
            }

            avgTatLabel.setText(String.format(Locale.US, "%.2f", metrics.getAverageTurnaroundTime()));
            avgWtLabel.setText(String.format(Locale.US, "%.2f", metrics.getAverageWaitingTime()));
            avgRtLabel.setText(String.format(Locale.US, "%.2f", metrics.getAverageResponseTime()));
        } catch (Exception ex) {
            showAlert("Simulation Error", ex.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class ProcessRow {
        private String id;
        private int arrivalTime;
        private int burstTime;
        private int initialPriority;

        public ProcessRow(String id, int arrivalTime, int burstTime, int initialPriority) {
            this.id = id;
            this.arrivalTime = arrivalTime;
            this.burstTime = burstTime;
            this.initialPriority = initialPriority;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public int getArrivalTime() { return arrivalTime; }
        public void setArrivalTime(int arrivalTime) { this.arrivalTime = arrivalTime; }
        public int getBurstTime() { return burstTime; }
        public void setBurstTime(int burstTime) { this.burstTime = burstTime; }
        public int getInitialPriority() { return initialPriority; }
        public void setInitialPriority(int initialPriority) { this.initialPriority = initialPriority; }
    }

    public static class MetricsRow {
        private final String id;
        private final int arrivalTime;
        private final int burstTime;
        private final int finishTime;
        private final int turnaroundTime;
        private final int waitingTime;
        private final int responseTime;

        public MetricsRow(String id, int at, int bt, int ft, int tat, int wt, int rt) {
            this.id = id;
            this.arrivalTime = at;
            this.burstTime = bt;
            this.finishTime = ft;
            this.turnaroundTime = tat;
            this.waitingTime = wt;
            this.responseTime = rt;
        }

        public String getId() { return id; }
        public int getArrivalTime() { return arrivalTime; }
        public int getBurstTime() { return burstTime; }
        public int getFinishTime() { return finishTime; }
        public int getTurnaroundTime() { return turnaroundTime; }
        public int getWaitingTime() { return waitingTime; }
        public int getResponseTime() { return responseTime; }
    }
}
