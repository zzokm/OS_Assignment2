package gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;
import model.Process;

/**
 * GUI input & control panel for configuring processes, algorithm parameters, and simulation playback.
 */
public final class InputControlPanel extends JPanel {
    public enum Algorithm {
        PREEMPTIVE_SJF("Preemptive SJF (SRTF)"),
        ACS("Adaptive Container Scheduler (ACS)");

        private final String label;

        Algorithm(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final DefaultTableModel processTableModel;
    private final JTable processTable;
    private final JComboBox<Algorithm> algorithmBox;
    private final JSpinner quantumSpinner;
    private final JSpinner contextSwitchSpinner;

    private final JButton runButton = new JButton("Run Simulation");
    private final JButton resetButton = new JButton("Reset");

    public InputControlPanel(Runnable onRun, Runnable onReset) {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top controls: Algorithm, Quantum, Context Switch
        JPanel configPanel = new JPanel(new GridLayout(3, 2, 6, 6));
        configPanel.setBorder(BorderFactory.createTitledBorder("Scheduler Configuration"));

        algorithmBox = new JComboBox<>(Algorithm.values());
        algorithmBox.setSelectedItem(Algorithm.ACS);
        quantumSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));
        contextSwitchSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 50, 1));

        configPanel.add(new JLabel("Algorithm:"));
        configPanel.add(algorithmBox);
        configPanel.add(new JLabel("Time Quantum (Q):"));
        configPanel.add(quantumSpinner);
        configPanel.add(new JLabel("Context Switch (CS):"));
        configPanel.add(contextSwitchSpinner);

        // Center: Editable Process Table
        String[] columns = {"ID", "Arrival Time", "Burst Time", "Initial Priority"};
        processTableModel = new DefaultTableModel(columns, 0);
        processTable = new JTable(processTableModel);
        JScrollPane tableScroll = new JScrollPane(processTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Processes Workload"));

        // Table action buttons + Presets
        JPanel tableButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton addRowBtn = new JButton("+ Add Process");
        JButton removeRowBtn = new JButton("- Remove");
        JButton tc1Btn = new JButton("Load TC 1");
        JButton tc2Btn = new JButton("Load TC 2");

        addRowBtn.addActionListener(e -> {
            int nextNum = processTableModel.getRowCount() + 1;
            processTableModel.addRow(new Object[]{"P" + nextNum, 0, 4, 1});
        });
        removeRowBtn.addActionListener(e -> {
            int row = processTable.getSelectedRow();
            if (row >= 0) {
                processTableModel.removeRow(row);
            } else if (processTableModel.getRowCount() > 0) {
                processTableModel.removeRow(processTableModel.getRowCount() - 1);
            }
        });
        tc1Btn.addActionListener(e -> loadTestCase1());
        tc2Btn.addActionListener(e -> loadTestCase2());

        tableButtons.add(addRowBtn);
        tableButtons.add(removeRowBtn);
        tableButtons.add(tc1Btn);
        tableButtons.add(tc2Btn);

        JPanel centerPanel = new JPanel(new BorderLayout(4, 4));
        centerPanel.add(tableScroll, BorderLayout.CENTER);
        centerPanel.add(tableButtons, BorderLayout.SOUTH);

        // Bottom: Run & Reset
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        runButton.addActionListener(e -> onRun.run());
        resetButton.addActionListener(e -> onReset.run());
        actionPanel.add(resetButton);
        actionPanel.add(runButton);

        add(configPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(actionPanel, BorderLayout.SOUTH);

        loadTestCase2();
    }

    public void loadTestCase1() {
        processTableModel.setRowCount(0);
        processTableModel.addRow(new Object[]{"P1", 0, 4, 2});
        processTableModel.addRow(new Object[]{"P2", 1, 7, 3});
        processTableModel.addRow(new Object[]{"P3", 3, 2, 1});
        quantumSpinner.setValue(3);
        contextSwitchSpinner.setValue(0);
    }

    public void loadTestCase2() {
        processTableModel.setRowCount(0);
        processTableModel.addRow(new Object[]{"P1", 0, 5, 4});
        processTableModel.addRow(new Object[]{"P2", 1, 6, 2});
        processTableModel.addRow(new Object[]{"P3", 4, 4, 3});
        quantumSpinner.setValue(3);
        contextSwitchSpinner.setValue(1);
    }

    public Algorithm getSelectedAlgorithm() {
        return (Algorithm) algorithmBox.getSelectedItem();
    }

    public int getQuantum() {
        return (Integer) quantumSpinner.getValue();
    }

    public int getContextSwitch() {
        return (Integer) contextSwitchSpinner.getValue();
    }

    public List<Process> readProcesses() {
        if (processTable.isEditing()) {
            processTable.getCellEditor().stopCellEditing();
        }
        List<Process> list = new ArrayList<>();
        for (int r = 0; r < processTableModel.getRowCount(); r++) {
            try {
                String id = String.valueOf(processTableModel.getValueAt(r, 0)).trim();
                int at = Integer.parseInt(String.valueOf(processTableModel.getValueAt(r, 1)).trim());
                int bt = Integer.parseInt(String.valueOf(processTableModel.getValueAt(r, 2)).trim());
                int pri = Integer.parseInt(String.valueOf(processTableModel.getValueAt(r, 3)).trim());
                list.add(new Process(id, at, bt, pri));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Invalid process values at row " + (r + 1) + ": " + ex.getMessage(),
                        "Input Error", JOptionPane.ERROR_MESSAGE);
                return List.of();
            }
        }
        return list;
    }
}
