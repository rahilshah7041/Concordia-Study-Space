/**
 * Simple test runner. Runs all test classes and prints a summary.
 * No external libraries required.
 *
 * Compile:  javac src/*.java tests/*.java -d out
 * Run:      java -cp out TestRunner
 */
public class TestRunner {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=".repeat(55));
        System.out.println("  CSEBS Unit Test Suite");
        System.out.println("=".repeat(55) + "\n");

        UserTests.run();
        RoomEquipmentTests.run();
        TimeSlotTests.run();
        ReservationTests.run();
        BookingSystemTests.run();

        System.out.println("\n" + "=".repeat(55));
        System.out.println("  Results: " + passed + " passed, " + failed + " failed.");
        System.out.println("=".repeat(55));

        if (failed > 0) System.exit(1);
    }

    /** Asserts a condition is true. */
    public static void assertTrue(String testName, boolean condition) {
        if (condition) {
            System.out.println("  [PASS] " + testName);
            passed++;
        } else {
            System.out.println("  [FAIL] " + testName);
            failed++;
        }
    }

    /** Asserts that two values are equal. */
    public static void assertEquals(String testName, Object expected, Object actual) {
        boolean ok = (expected == null && actual == null) ||
                     (expected != null && expected.equals(actual));
        if (ok) {
            System.out.println("  [PASS] " + testName);
            passed++;
        } else {
            System.out.println("  [FAIL] " + testName +
                    " (expected=" + expected + ", actual=" + actual + ")");
            failed++;
        }
    }

    /** Asserts that the runnable throws an exception. */
    public static void assertThrows(String testName, Runnable action) {
        try {
            action.run();
            System.out.println("  [FAIL] " + testName + " (expected exception, none thrown)");
            failed++;
        } catch (Exception e) {
            System.out.println("  [PASS] " + testName + " → " + e.getClass().getSimpleName());
            passed++;
        }
    }
}
