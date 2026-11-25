package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import metrics.SchedulingMetrics;
import model.Process;
import output.TraceTableFormatter;
import scheduler.SJFScheduler;
import scheduler.Scheduler;
import scheduler.acs.ACSScheduler;

/**
 * Main Swing window shell integrating workload controls, execution trace output, and metrics summary.
 */
public final class MainFrame extends JFrame {
    private final InputControlPanel inputPanel;
    private final JTextArea traceArea;
    private final DefaultTableModel metricsTableModel;
    private final JLabel avgTatLabel = new JLabel("Avg Turnaround Time: -");
    private final JLabel avgWtLabel = new JLabel("Avg Waiting Time: -");
    private final JLabel avgRtLabel = new JLabel("Avg Response Time: -");

    public MainFrame() {
        super("CS241 Assignment 2 — Cloud-Cluster CPU Scheduling Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1100, 700));

        inputPanel = new InputControlPanel(this::runSimulation, this::resetOutputs);
        inputPanel.setPreferredSize(new Dimension(380, 700));

        // Center: Trace Output Area (Host slot for Student 5's GanttChartPanel & QueueVisualizer)
        traceArea = new JTextArea();
        traceArea.setEditable(false);
        traceArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane traceScroll = new JScrollPane(traceArea);
        traceScroll.setBorder(BorderFactory.createTitledBorder("Execution Timeline & Trace"));

        // Bottom Right: Metrics Table & Averages
        String[] cols = {"Process", "AT", "BT", "FT", "TAT", "WT", "RT"};
        metricsTableModel = new DefaultTableModel(cols, 0);
        JTable metricsTable = new JTable(metricsTableModel);
        JScrollPane metricsScroll = new JScrollPane(metricsTable);
        metricsScroll.setBorder(BorderFactory.createTitledBorder("Process Metrics"));

        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 12, 4));
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        summaryPanel.add(avgTatLabel);
        summaryPanel.add(avgWtLabel);
        summaryPanel.add(avgRtLabel);

        JPanel metricsContainer = new JPanel(new BorderLayout(4, 4));
        metricsContainer.add(metricsScroll, BorderLayout.CENTER);
        metricsContainer.add(summaryPanel, BorderLayout.SOUTH);

        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, traceScroll, metricsContainer);
        rightSplit.setResizeWeight(0.55);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputPanel, rightSplit);
        mainSplit.setResizeWeight(0.35);

        setContentPane(mainSplit);
        pack();
        setLocationRelativeTo(null);
    }

    private void runSimulation() {
        List<Process> processes = inputPanel.readProcesses();
        if (processes.isEmpty()) {
            return;
        }

        try {
            InputControlPanel.Algorithm algo = inputPanel.getSelectedAlgorithm();
            int q = inputPanel.getQuantum();
            int cs = inputPanel.getContextSwitch();

            SchedulingMetrics metrics;
            String traceText;

            if (algo == InputControlPanel.Algorithm.ACS) {
                ACSScheduler acs = new ACSScheduler(q, cs);
                acs.loadProcesses(processes);
                acs.runSimulation();
                metrics = acs.getMetrics();
                traceText = TraceTableFormatter.formatAcsTrace(acs.getExecutionTrace());
            } else {
                SJFScheduler sjf = new SJFScheduler(cs);
                sjf.loadProcesses(processes);
                sjf.runSimulation();
                metrics = sjf.getMetrics();
                traceText = TraceTableFormatter.formatSjfTrace(sjf.getExecutionTrace());
            }

            traceArea.setText(traceText);
            traceArea.setCaretPosition(0);
            updateMetricsDisplay(metrics);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Simulation error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateMetricsDisplay(SchedulingMetrics metrics) {
        metricsTableModel.setRowCount(0);
        for (Process p : metrics.getProcesses()) {
            metricsTableModel.addRow(new Object[]{
                    p.getId(),
                    p.getArrivalTime(),
                    p.getBurstTime(),
                    p.getFinishTime(),
                    p.getTurnaroundTime(),
                    p.getWaitingTime(),
                    p.getResponseTime()
            });
        }
        avgTatLabel.setText(String.format(Locale.US, "Avg Turnaround Time: %.2f",
                metrics.getAverageTurnaroundTime()));
        avgWtLabel.setText(String.format(Locale.US, "Avg Waiting Time: %.2f",
                metrics.getAverageWaitingTime()));
        avgRtLabel.setText(String.format(Locale.US, "Avg Response Time: %.2f",
                metrics.getAverageResponseTime()));
    }

    private void resetOutputs() {
        traceArea.setText("");
        metricsTableModel.setRowCount(0);
        avgTatLabel.setText("Avg Turnaround Time: -");
        avgWtLabel.setText("Avg Waiting Time: -");
        avgRtLabel.setText("Avg Response Time: -");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
