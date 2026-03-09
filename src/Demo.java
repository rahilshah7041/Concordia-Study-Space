import java.time.LocalDateTime;
import java.util.List;

/**
 * Entry point for the CSEBS demo.
 * Run with:  javac src/*.java -d out   then   java -cp out Main
 *
 * Demonstrates every class interaction from the UML diagram:
 *  - User login / logout
 *  - Student books a room and adds equipment
 *  - Double-booking prevention
 *  - Check-in, cancellation, no-show, strikes, suspension
 *  - Staff approves / overrides reservations
 *  - Admin configures policies and generates report
 */
public class Demo {

    public static void main(String[] args) {

        line("CSEBS - Concordia Study Space & Equipment Booking System");

        BookingSystem system = new BookingSystem();

        // ── Setup: rooms & equipment ──────────────────────────────────────────
        section("1. SYSTEM SETUP");
        Room roomH110 = new Room("H-110", "Hall Building", 10);
        Room roomEV1  = new Room("EV-1",  "EV Building",    6);
        Room roomLB2  = new Room("LB-2",  "Library",         4);
        system.addRoom(roomH110);
        system.addRoom(roomEV1);
        system.addRoom(roomLB2);

        Equipment projector = new Equipment("EQ-01", "Projector",  3);
        Equipment hdmi      = new Equipment("EQ-02", "HDMI Cable", 8);
        Equipment monitor   = new Equipment("EQ-03", "Monitor",    5);
        system.addEquipment(projector);
        system.addEquipment(hdmi);
        system.addEquipment(monitor);

        // ── Register users ────────────────────────────────────────────────────
        section("2. REGISTER USERS");
        Student alice = new Student("S001", "Alice Martin", "alice@concordia.ca", "alice123", "Computer Engineering");
        Student bob   = new Student("S002", "Bob Nguyen",   "bob@concordia.ca",   "bob456",   "Electrical Engineering");
        Student carol = new Student("S003", "Carol Smith",  "carol@concordia.ca", "carol789", "Software Engineering");
        Staff   lee   = new Staff  ("ST01", "Dr. Lee",      "lee@concordia.ca",   "staff001", "STF-100", "Library");
        Admin   admin = new Admin  ("AD01", "Admin",        "admin@concordia.ca", "admin999", "STF-000", "IT");

        system.registerStudent(alice);
        system.registerStudent(bob);
        system.registerStudent(carol);
        system.registerStaff(lee);
        system.registerStaff(admin);

        // ── Login ─────────────────────────────────────────────────────────────
        section("3. LOGIN");
        alice.login("alice123");
        bob.login("bob456");
        carol.login("carol789");
        lee.login("staff001");
        admin.login("admin999");
        bob.login("wrongpassword");   // intentional fail

        // ── Student searches rooms ────────────────────────────────────────────
        section("4. ALICE SEARCHES ROOMS");
        List<Room> available = alice.searchRooms(system.getRooms());
        for (Room r : available) System.out.println("  " + r);

        // ── Alice books H-110 ─────────────────────────────────────────────────
        section("5. ALICE BOOKS ROOM H-110");
        LocalDateTime day1_9am  = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime day1_11am = day1_9am.plusHours(2);
        TimeSlot slot1 = new TimeSlot(day1_9am, day1_11am);
        Reservation res1 = system.bookRoom(alice, roomH110, slot1, 8);
        System.out.println("Created: " + res1);

        // ── Alice adds equipment ──────────────────────────────────────────────
        section("6. ALICE ADDS EQUIPMENT");
        system.addEquipmentToReservation(res1, projector, 1);
        system.addEquipmentToReservation(res1, hdmi, 2);
        System.out.println("Projector remaining: " + projector.getQuantityAvailable());

        // ── Double booking attempt ────────────────────────────────────────────
        section("7. BOB TRIES TO DOUBLE-BOOK H-110 (should fail)");
        TimeSlot slot2 = new TimeSlot(day1_9am.plusMinutes(30), day1_9am.plusHours(3));
        try {
            system.bookRoom(bob, roomH110, slot2, 4);
        } catch (IllegalStateException e) {
            System.out.println("[Expected] " + e.getMessage());
        }

        // ── Bob books different room ──────────────────────────────────────────
        section("8. BOB BOOKS EV-1 (no conflict)");
        TimeSlot slot3 = new TimeSlot(day1_9am, day1_11am);
        Reservation res2 = system.bookRoom(bob, roomEV1, slot3, 5);
        System.out.println("Created: " + res2);

        // ── Alice checks in ───────────────────────────────────────────────────
        section("9. ALICE CHECKS IN");
        alice.checkIn(res1.getReservationID());
        System.out.println("Status: " + res1.getStatus());

        // ── Late cancellation (within 30 min) → strike ────────────────────────
        section("10. CAROL BOOKS THEN CANCELS LATE (gets a strike)");
        TimeSlot nearSlot = new TimeSlot(
                LocalDateTime.now().plusMinutes(10),
                LocalDateTime.now().plusMinutes(70));
        Reservation res3 = system.bookRoom(carol, roomLB2, nearSlot, 2);
        system.cancelReservation(carol, res3.getReservationID());
        System.out.println("Carol's strikes: " + carol.getStrikeCount());

        // ── Staff approves a reservation ──────────────────────────────────────
        section("11. STAFF APPROVES BOB'S RESERVATION");
        res2.setStatus(ReservationStatus.PENDING); // reset for demo
        lee.approveReservation(res2);

        // ── No-show ───────────────────────────────────────────────────────────
        section("12. BOB IS MARKED NO-SHOW");
        system.markNoShow(bob, res2.getReservationID());
        System.out.println("Bob's strikes: " + bob.getStrikeCount());

        // ── Admin configures policy ───────────────────────────────────────────
        section("13. ADMIN CONFIGURES POLICY");
        admin.configurePolicies(system.getPolicy(), 180, 2, 3);

        // ── Admin report ──────────────────────────────────────────────────────
        section("14. ADMIN USAGE REPORT");
        admin.generateUsageReport(system.getStudents());

        // ── Admin suspends carol ──────────────────────────────────────────────
        section("15. ADMIN SUSPENDS CAROL");
        admin.suspendStudent(carol, "Repeated violations");
        System.out.println("Carol suspended? " + carol.isSuspended());

        // ── Suspended student tries to book ───────────────────────────────────
        section("16. CAROL TRIES TO BOOK WHILE SUSPENDED (should fail)");
        TimeSlot futureSlot = new TimeSlot(
                LocalDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0),
                LocalDateTime.now().plusDays(3).withHour(12).withMinute(0).withSecond(0).withNano(0));
        try {
            system.bookRoom(carol, roomLB2, futureSlot, 2);
        } catch (IllegalStateException e) {
            System.out.println("[Expected] " + e.getMessage());
        }

        // ── Capacity violation ────────────────────────────────────────────────
        section("17. ALICE TRIES TO BOOK LB-2 WITH 10 PEOPLE (capacity = 4)");
        TimeSlot slot4 = new TimeSlot(
                LocalDateTime.now().plusDays(4).withHour(9).withMinute(0).withSecond(0).withNano(0),
                LocalDateTime.now().plusDays(4).withHour(10).withMinute(0).withSecond(0).withNano(0));
        try {
            system.bookRoom(alice, roomLB2, slot4, 10);
        } catch (IllegalStateException e) {
            System.out.println("[Expected] " + e.getMessage());
        }

        // ── Logout ────────────────────────────────────────────────────────────
        section("18. LOGOUT");
        alice.logout();
        bob.logout();
        carol.logout();
        lee.logout();
        admin.logout();

        line("Demo complete.");
    }

    private static void section(String title) {
        System.out.println("\n--- " + title + " " + "-".repeat(Math.max(0, 52 - title.length())));
    }

    private static void line(String msg) {
        System.out.println("=".repeat(55));
        System.out.println("  " + msg);
        System.out.println("=".repeat(55));
    }
}
