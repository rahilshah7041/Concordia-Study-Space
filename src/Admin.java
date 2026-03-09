import java.util.List;

/**
 * Represents an admin user with additional policy and reporting powers.
 *
 * UML: Admin  (inherits Staff)
 *   + configurePolicies()  : void
 *   + generateUsageReport(): void
 */
public class Admin extends Staff {

    public Admin(String userID, String name, String email, String password,
                 String staffID, String department) {
        super(userID, name, email, password, staffID, department);
    }

    // ── Domain methods ─────────────────────────────────────────────

    /**
     * Configures the global booking policy.
     */
    public void configurePolicies(Policy policy, int maxDurationMinutes,
                                   int maxReservationsPerDay, int strikeThreshold) {
        requireLogin();
        if (maxDurationMinutes <= 0 || maxReservationsPerDay <= 0 || strikeThreshold <= 0)
            throw new IllegalArgumentException("Policy values must be greater than 0.");
        policy.setMaxDurationMinutes(maxDurationMinutes);
        policy.setMaxReservationsPerDay(maxReservationsPerDay);
        policy.setStrikeThreshold(strikeThreshold);
        System.out.println("[Admin:" + getName() + "] Policies updated: " + policy);
    }

    /**
     * Prints a usage report for all students.
     */
    public void generateUsageReport(List<Student> students) {
        requireLogin();
        System.out.println("\n=== CSEBS Usage Report (by " + getName() + ") ===");
        for (Student s : students) {
            System.out.printf("  %-20s | reservations: %d | strikes: %d | suspended: %s%n",
                    s.getName(),
                    s.getReservations().size(),
                    s.getStrikeCount(),
                    s.isSuspended() ? "YES" : "no");
        }
        System.out.println("=== End of Report ===\n");
    }

    /**
     * Forcibly suspends a student by adding strikes until threshold is reached.
     */
    public void suspendStudent(Student student, String reason) {
        requireLogin();
        while (!student.isSuspended()) {
            student.addStrike("Admin suspension: " + reason);
        }
        System.out.println("[Admin:" + getName() + "] " + student.getName() + " SUSPENDED.");
    }

    // ── Helper ─────────────────────────────────────────────────────

    private void requireLogin() {
        if (!isLoggedIn())
            throw new IllegalStateException(getName() + " must be logged in.");
    }

    @Override
    public String toString() {
        return "Admin{ userID='" + getUserID() + "', name='" + getName() + "' }";
    }
}
