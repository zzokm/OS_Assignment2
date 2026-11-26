package analysis;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import metrics.SchedulingMetrics;
import model.Process;
import model.TimeStepRecord;
import scheduler.SJFScheduler;
import scheduler.Scheduler;
import scheduler.acs.ACSScheduler;


public class ExperimentRunner {

    public static void main(String[] args) {
        List<Process> tc1 = createTestCase1();
        List<Process> tc2 = createTestCase2();

        File reportDir = new File("report");
        if (!reportDir.exists()) {
            reportDir.mkdirs();
        }

        String csvPath = "report/sweep_results.csv";
        try (PrintWriter out = new PrintWriter(new FileWriter(csvPath))) {
            out.println("workload,scheduler,Q,CS,avg_wt,avg_tat,avg_rt,cpu_util_pct,cs_ticks,makespan");

            System.out.println("=== ACS Parameter Sweep: Test Case 1 ===");
            runAcsSweep("TC1", tc1, out);

            System.out.println("\n=== ACS Parameter Sweep: Test Case 2 ===");
            runAcsSweep("TC2", tc2, out);

            System.out.println("\n=== Preemptive SJF Comparison ===");
            runSjfSweep("TC1", tc1, out);
            runSjfSweep("TC2", tc2, out);

            System.out.println("\nSaved results to " + csvPath);
        } catch (IOException e) {
            System.err.println("Could not write CSV file: " + e.getMessage());
        }
    }

    private static void runAcsSweep(String name, List<Process> workload, PrintWriter out) {
        System.out.printf("%-4s %-4s %-8s %-8s %-8s %-9s %-8s%n",
                "Q", "CS", "AvgWT", "AvgTAT", "AvgRT", "CPU%", "CS_Ticks");

        for (int cs = 0; cs <= 3; cs++) {
            for (int q = 1; q <= 10; q++) {
                ACSScheduler acs = new ACSScheduler(q, cs);
                acs.loadProcesses(copyList(workload));
                acs.runSimulation();

                SchedulingMetrics metrics = acs.getMetrics();
                List<TimeStepRecord> trace = acs.getExecutionTrace();

                // Last record in ACS trace is the completion IDLE snapshot
                int makespan = Math.max(1, trace.size() - 1);
                int busyTicks = 0;
                int csTicks = 0;
                for (int i = 0; i < trace.size() - 1; i++) {
                    String running = trace.get(i).runningProcessId();
                    if ("CS".equals(running)) {
                        csTicks++;
                    } else if (!"IDLE".equals(running)) {
                        busyTicks++;
                    }
                }
                double cpuUtil = (busyTicks * 100.0) / makespan;

                out.printf(Locale.US, "%s,ACS,%d,%d,%.4f,%.4f,%.4f,%.2f,%d,%d%n",
                        name, q, cs,
                        metrics.getAverageWaitingTime(),
                        metrics.getAverageTurnaroundTime(),
                        metrics.getAverageResponseTime(),
                        cpuUtil, csTicks, makespan);

                if (q == 1 || q == 3 || q == 5 || q == 10) {
                    System.out.printf(Locale.US, "%-4d %-4d %-8.2f %-8.2f %-8.2f %-9.2f %-8d%n",
                            q, cs,
                            metrics.getAverageWaitingTime(),
                            metrics.getAverageTurnaroundTime(),
                            metrics.getAverageResponseTime(),
                            cpuUtil, csTicks);
                }
            }
        }
    }

    private static void runSjfSweep(String name, List<Process> workload, PrintWriter out) {
        for (int cs = 0; cs <= 3; cs++) {
            SJFScheduler sjf = new SJFScheduler(cs);
            sjf.loadProcesses(copyList(workload));
            sjf.runSimulation();

            SchedulingMetrics metrics = sjf.getMetrics();
            @SuppressWarnings("unchecked")
            List<Scheduler.TimeStepRecord> trace = (List<Scheduler.TimeStepRecord>) sjf.getExecutionTrace();

            int makespan = Math.max(1, trace.size());
            int busyTicks = 0;
            int csTicks = 0;
            for (Scheduler.TimeStepRecord step : trace) {
                if (step.activity() == Scheduler.Activity.RUNNING) {
                    busyTicks++;
                } else if (step.activity() == Scheduler.Activity.CONTEXT_SWITCH) {
                    csTicks++;
                }
            }
            double cpuUtil = (busyTicks * 100.0) / makespan;

            out.printf(Locale.US, "%s,SJF,0,%d,%.4f,%.4f,%.4f,%.2f,%d,%d%n",
                    name, cs,
                    metrics.getAverageWaitingTime(),
                    metrics.getAverageTurnaroundTime(),
                    metrics.getAverageResponseTime(),
                    cpuUtil, csTicks, makespan);

            System.out.printf(Locale.US, "%s SJF (CS=%d): AvgWT=%.2f, AvgTAT=%.2f, AvgRT=%.2f, CPU=%.2f%%%n",
                    name, cs,
                    metrics.getAverageWaitingTime(),
                    metrics.getAverageTurnaroundTime(),
                    metrics.getAverageResponseTime(),
                    cpuUtil);
        }
    }

    public static List<Process> createTestCase1() {
        List<Process> list = new ArrayList<>();
        list.add(new Process("P1", 0, 4, 2));
        list.add(new Process("P2", 1, 7, 3));
        list.add(new Process("P3", 3, 2, 1));
        return list;
    }

    public static List<Process> createTestCase2() {
        List<Process> list = new ArrayList<>();
        list.add(new Process("P1", 0, 5, 4));
        list.add(new Process("P2", 1, 6, 2));
        list.add(new Process("P3", 4, 4, 3));
        return list;
    }

    private static List<Process> copyList(List<Process> source) {
        List<Process> copy = new ArrayList<>();
        for (Process p : source) {
            copy.add(new Process(p));
        }
        return copy;
    }
}
