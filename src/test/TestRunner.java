package test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import metrics.MetricsEngine;
import metrics.SchedulingMetrics;
import model.Process;
import output.TraceTableFormatter;
import scheduler.SJFScheduler;
import scheduler.acs.ACSScheduler;

public final class TestRunner {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        runTest("ACS Test Case 1 (Q=3, CS=0)", TestRunner::testAcsTestCase1);
        runTest("ACS Test Case 2 (Q=3, CS=1)", TestRunner::testAcsTestCase2);
        runTest("Preemptive SJF (CS=0)", TestRunner::testSjfNoContextSwitch);
        runTest("Preemptive SJF (CS=1)", TestRunner::testSjfWithContextSwitch);
        runTest("MetricsEngine Validation", TestRunner::testMetricsEngineValidation);

        System.out.println();
        System.out.printf("Summary: %d passed, %d failed%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testAcsTestCase1() {
        List<Process> workload = List.of(
                new Process("P1", 0, 4, 2),
                new Process("P2", 1, 7, 3),
                new Process("P3", 3, 2, 1)
        );
        ACSScheduler acs = new ACSScheduler(3, 0);
        acs.loadProcesses(workload);
        acs.runSimulation();

        SchedulingMetrics metrics = acs.getMetrics();
        Map<String, Process> byId = mapById(metrics.getProcesses());

        assertEquals(9, byId.get("P1").getFinishTime(), "TC1 P1 FT");
        assertEquals(9, byId.get("P1").getTurnaroundTime(), "TC1 P1 TAT");
        assertEquals(5, byId.get("P1").getWaitingTime(), "TC1 P1 WT");

        assertEquals(13, byId.get("P2").getFinishTime(), "TC1 P2 FT");
        assertEquals(12, byId.get("P2").getTurnaroundTime(), "TC1 P2 TAT");
        assertEquals(5, byId.get("P2").getWaitingTime(), "TC1 P2 WT");

        assertEquals(8, byId.get("P3").getFinishTime(), "TC1 P3 FT");
        assertEquals(5, byId.get("P3").getTurnaroundTime(), "TC1 P3 TAT");
        assertEquals(3, byId.get("P3").getWaitingTime(), "TC1 P3 WT");

        assertApprox(8.67, metrics.getAverageTurnaroundTime(), 0.01, "TC1 Avg TAT");
        assertApprox(4.33, metrics.getAverageWaitingTime(), 0.01, "TC1 Avg WT");

        System.out.println("--- ACS Test Case 1 ---");
        System.out.println(TraceTableFormatter.formatAcsTrace(acs.getExecutionTrace()));
        System.out.println(TraceTableFormatter.formatMetricsTable(metrics));
    }

    private static void testAcsTestCase2() {
        List<Process> workload = List.of(
                new Process("P1", 0, 5, 4),
                new Process("P2", 1, 6, 2),
                new Process("P3", 4, 4, 3)
        );
        ACSScheduler acs = new ACSScheduler(3, 1);
        acs.loadProcesses(workload);
        acs.runSimulation();

        SchedulingMetrics metrics = acs.getMetrics();
        Map<String, Process> byId = mapById(metrics.getProcesses());

        assertEquals(18, byId.get("P1").getFinishTime(), "TC2 P1 FT");
        assertEquals(18, byId.get("P1").getTurnaroundTime(), "TC2 P1 TAT");
        assertEquals(13, byId.get("P1").getWaitingTime(), "TC2 P1 WT");

        assertEquals(15, byId.get("P2").getFinishTime(), "TC2 P2 FT");
        assertEquals(14, byId.get("P2").getTurnaroundTime(), "TC2 P2 TAT");
        assertEquals(8, byId.get("P2").getWaitingTime(), "TC2 P2 WT");

        assertEquals(20, byId.get("P3").getFinishTime(), "TC2 P3 FT");
        assertEquals(16, byId.get("P3").getTurnaroundTime(), "TC2 P3 TAT");
        assertEquals(12, byId.get("P3").getWaitingTime(), "TC2 P3 WT");

        assertApprox(16.00, metrics.getAverageTurnaroundTime(), 0.01, "TC2 Avg TAT");
        assertApprox(11.00, metrics.getAverageWaitingTime(), 0.01, "TC2 Avg WT");

        System.out.println("--- ACS Test Case 2 ---");
        System.out.println(TraceTableFormatter.formatAcsTrace(acs.getExecutionTrace()));
        System.out.println(TraceTableFormatter.formatMetricsTable(metrics));
    }

    private static void testSjfNoContextSwitch() {
        List<Process> workload = List.of(
                new Process("P1", 0, 4, 2),
                new Process("P2", 1, 7, 3),
                new Process("P3", 3, 2, 1)
        );
        SJFScheduler sjf = new SJFScheduler(0);
        sjf.loadProcesses(workload);
        sjf.runSimulation();

        SchedulingMetrics metrics = sjf.getMetrics();
        Map<String, Process> byId = mapById(metrics.getProcesses());

        assertEquals(4, byId.get("P1").getFinishTime(), "SJF(CS=0) P1 FT");
        assertEquals(13, byId.get("P2").getFinishTime(), "SJF(CS=0) P2 FT");
        assertEquals(6, byId.get("P3").getFinishTime(), "SJF(CS=0) P3 FT");
        assertApprox(6.33, metrics.getAverageTurnaroundTime(), 0.01, "SJF(CS=0) Avg TAT");
        assertApprox(2.00, metrics.getAverageWaitingTime(), 0.01, "SJF(CS=0) Avg WT");
    }

    private static void testSjfWithContextSwitch() {
        List<Process> workload = List.of(
                new Process("P1", 0, 6, 1),
                new Process("P2", 2, 2, 1)
        );
        SJFScheduler sjf = new SJFScheduler(1);
        sjf.loadProcesses(workload);
        sjf.runSimulation();

        SchedulingMetrics metrics = sjf.getMetrics();
        Map<String, Process> byId = mapById(metrics.getProcesses());

        //t=0..2 p1(rem=4), t=2..3 cs, t=3..5 p2(ft=5), t=5..6 cs, t=6..10 p1(ft=10)
        assertEquals(5, byId.get("P2").getFinishTime(), "SJF(CS=1) P2 FT");
        assertEquals(10, byId.get("P1").getFinishTime(), "SJF(CS=1) P1 FT");
    }

    private static void testMetricsEngineValidation() {
        boolean threw = false;
        try {
            MetricsEngine.compute(List.of());
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        if (!threw) {
            throw new AssertionError("Expected IllegalArgumentException for empty list");
        }
    }

    private static Map<String, Process> mapById(List<Process> processes) {
        return processes.stream().collect(Collectors.toMap(Process::getId, p -> p));
    }

    private static void runTest(String name, Runnable test) {
        try {
            test.run();
            passed++;
            System.out.println("[PASS] " + name);
        } catch (Throwable t) {
            failed++;
            System.err.println("[FAIL] " + name + ": " + t.getMessage());
        }
    }

    private static void assertEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + " expected " + expected + " but got " + actual);
        }
    }

    private static void assertApprox(double expected, double actual, double eps, String label) {
        if (Math.abs(expected - actual) > eps) {
            throw new AssertionError(String.format("%s expected %.2f but got %.2f", label, expected, actual));
        }
    }
}
