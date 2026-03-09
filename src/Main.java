import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Interactive CLI entry point for the CSEBS system.
 *
 * Compile:  javac -d out src/*.java tests/*.java
 * Run:      java -cp out Main
 */
public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static final BookingSystem system = new BookingSystem();
    private static User currentUser = null;

    public static void main(String[] args) {
        seedSystem();
        printBanner();

        while (true) {
            if (currentUser == null) {
                showGuestMenu();
            } else if (currentUser instanceof Admin) {
                showAdminMenu();
            } else if (currentUser instanceof Staff) {
                showStaffMenu();
            } else {
                showStudentMenu();
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  MENUS
    // ═══════════════════════════════════════════════════════════════

    private static void showGuestMenu() {
        System.out.println("\n╔══════════════════════════════════════╗");
        System.out.println("║          CSEBS  –  Main Menu          ║");
        System.out.println("╚══════════════════════════════════════╝");
        System.out.println("  1. Login");
        System.out.println("  2. Register as Student");
        System.out.println("  0. Exit");
        switch (promptInt("Choose")) {
            case 1  -> doLogin();
            case 2  -> doRegisterStudent();
            case 0  -> exit();
            default -> System.out.println("  Invalid option.");
        }
    }

    private static void showStudentMenu() {
        Student s = (Student) currentUser;
        System.out.println("\n┌─────────────────────────────────────────┐");
        System.out.printf ("│  Student: %-20s           │%n", s.getName());
        System.out.printf ("│  Strikes: %d/%d  %s│%n",
                s.getStrikeCount(), Student.MAX_STRIKES,
                s.isSuspended() ? "*** SUSPENDED ***     " : "                      ");
        System.out.println("└─────────────────────────────────────────┘");
        System.out.println("  1. Search available rooms");
        System.out.println("  2. Book a room");
        System.out.println("  3. Add equipment to a reservation");
        System.out.println("  4. View my reservations");
        System.out.println("  5. Check in to a reservation");
        System.out.println("  6. Cancel a reservation");
        System.out.println("  0. Logout");
        switch (promptInt("Choose")) {
            case 1  -> doSearchRooms();
            case 2  -> doBookRoom(s);
            case 3  -> doAddEquipment(s);
            case 4  -> doViewMyReservations(s);
            case 5  -> doCheckIn(s);
            case 6  -> doCancelReservation(s);
            case 0  -> doLogout();
            default -> System.out.println("  Invalid option.");
        }
    }

    private static void showStaffMenu() {
        Staff st = (Staff) currentUser;
        System.out.println("\n┌─────────────────────────────────────────┐");
        System.out.printf ("│  Staff: %-23s           │%n", st.getName());
        System.out.println("└─────────────────────────────────────────┘");
        System.out.println("  1. View all reservations");
        System.out.println("  2. Approve a reservation");
        System.out.println("  3. Override (cancel) a reservation");
        System.out.println("  4. Add a room");
        System.out.println("  5. Update equipment");
        System.out.println("  6. Mark student as no-show");
        System.out.println("  0. Logout");
        switch (promptInt("Choose")) {
            case 1  -> doViewAllReservations();
            case 2  -> doApproveReservation(st);
            case 3  -> doOverrideReservation(st);
            case 4  -> doAddRoom(st);
            case 5  -> doUpdateEquipment(st);
            case 6  -> doMarkNoShow();
            case 0  -> doLogout();
            default -> System.out.println("  Invalid option.");
        }
    }

    private static void showAdminMenu() {
        Admin a = (Admin) currentUser;
        System.out.println("\n┌─────────────────────────────────────────┐");
        System.out.printf ("│  Admin: %-23s           │%n", a.getName());
        System.out.println("└─────────────────────────────────────────┘");
        System.out.println("  1.  View all reservations");
        System.out.println("  2.  Approve a reservation");
        System.out.println("  3.  Override (cancel) a reservation");
        System.out.println("  4.  Add a room");
        System.out.println("  5.  Update equipment");
        System.out.println("  6.  Mark student as no-show");
        System.out.println("  7.  Configure system policies");
        System.out.println("  8.  View current policies");
        System.out.println("  9.  Generate usage report");
        System.out.println("  10. Suspend a student");
        System.out.println("  0.  Logout");
        switch (promptInt("Choose")) {
            case 1  -> doViewAllReservations();
            case 2  -> doApproveReservation(a);
            case 3  -> doOverrideReservation(a);
            case 4  -> doAddRoom(a);
            case 5  -> doUpdateEquipment(a);
            case 6  -> doMarkNoShow();
            case 7  -> doConfigurePolicies(a);
            case 8  -> System.out.println("  " + system.getPolicy());
            case 9  -> a.generateUsageReport(system.getStudents());
            case 10 -> doSuspendStudent(a);
            case 0  -> doLogout();
            default -> System.out.println("  Invalid option.");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  ACTIONS – Auth
    // ═══════════════════════════════════════════════════════════════

    private static void doLogin() {
        System.out.println("\n── Login ──────────────────────────────────");
        String id = prompt("User ID");
        String pw = prompt("Password");

        User found = null;
        for (Student s : system.getStudents())
            if (s.getUserID().equals(id)) { found = s; break; }
        if (found == null)
            for (Staff st : system.getStaffList())
                if (st.getUserID().equals(id)) { found = st; break; }

        if (found == null) {
            System.out.println("  No user found with ID: " + id);
            return;
        }
        if (found.login(pw)) currentUser = found;
    }

    private static void doRegisterStudent() {
        System.out.println("\n── Register ───────────────────────────────");
        try {
            Student s = new Student(
                prompt("Choose a User ID"),
                prompt("Full name"),
                prompt("Email"),
                prompt("Password"),
                prompt("Major")
            );
            system.registerStudent(s);
            System.out.println("  ✔ Registered! You can now log in.");
        } catch (Exception e) {
            System.out.println("  ✘ " + e.getMessage());
        }
    }

    private static void doLogout() {
        currentUser.logout();
        currentUser = null;
    }

    // ═══════════════════════════════════════════════════════════════
    //  ACTIONS – Student
    // ═══════════════════════════════════════════════════════════════

    private static void doSearchRooms() {
        System.out.println("\n── Search Available Rooms ─────────────────");
        TimeSlot ts = promptTimeSlot();
        if (ts == null) return;
        List<Room> available = system.searchAvailableRooms(ts);
        if (available.isEmpty()) {
            System.out.println("  No rooms available for that time slot.");
        } else {
            System.out.println("  Available rooms:");
            for (Room r : available) System.out.println("    → " + r);
        }
    }

    private static void doBookRoom(Student student) {
        System.out.println("\n── Book a Room ────────────────────────────");
        System.out.println("  All rooms:");
        for (Room r : system.getRooms()) System.out.println("    → " + r);

        Room room = findRoom(prompt("Room ID to book"));
        if (room == null) { System.out.println("  Room not found."); return; }

        TimeSlot ts = promptTimeSlot();
        if (ts == null) return;

        int attendees = promptInt("Number of attendees");

        try {
            Reservation res = system.bookRoom(student, room, ts, attendees);
            System.out.println("\n  ✔ Reservation created!");
            System.out.println("  " + res);
            System.out.println("  Reservation ID: " + res.getReservationID() + "  (save this!)");
        } catch (Exception e) {
            System.out.println("  ✘ " + e.getMessage());
        }
    }

    private static void doAddEquipment(Student student) {
        System.out.println("\n── Add Equipment to Reservation ───────────");
        if (student.getReservations().isEmpty()) {
            System.out.println("  You have no reservations."); return;
        }
        doViewMyReservations(student);
        String resID = prompt("Reservation ID");

        Reservation res;
        try {
            res = student.findReservation(resID);
        } catch (Exception e) {
            System.out.println("  ✘ " + e.getMessage()); return;
        }

        System.out.println("  Available equipment:");
        for (Equipment eq : system.getEquipment()) System.out.println("    → " + eq);

        Equipment item = findEquipment(prompt("Equipment ID"));
        if (item == null) { System.out.println("  Equipment not found."); return; }

        int qty = promptInt("Quantity");
        try {
            system.addEquipmentToReservation(res, item, qty);
            System.out.println("  ✔ Equipment added.");
        } catch (Exception e) {
            System.out.println("  ✘ " + e.getMessage());
        }
    }

    private static void doViewMyReservations(Student student) {
        System.out.println("\n── My Reservations ────────────────────────");
        List<Reservation> list = student.getReservations();
        if (list.isEmpty()) { System.out.println("  No reservations."); return; }
        for (Reservation r : list) {
            System.out.println("  → " + r);
            if (!r.getEquipmentBookings().isEmpty())
                System.out.println("     Equipment: " + r.getEquipmentBookings());
        }
    }

    private static void doCheckIn(Student student) {
        System.out.println("\n── Check In ───────────────────────────────");
        doViewMyReservations(student);
        try {
            student.checkIn(prompt("Reservation ID to check in"));
            System.out.println("  ✔ Checked in successfully.");
        } catch (Exception e) {
            System.out.println("  ✘ " + e.getMessage());
        }
    }

    private static void doCancelReservation(Student student) {
        System.out.println("\n── Cancel Reservation ─────────────────────");
        doViewMyReservations(student);
        try {
            system.cancelReservation(student, prompt("Reservation ID to cancel"));
            System.out.println("  ✔ Reservation cancelled.");
        } catch (Exception e) {
            System.out.println("  ✘ " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  ACTIONS – Staff / Admin
    // ═══════════════════════════════════════════════════════════════

    private static void doViewAllReservations() {
        System.out.println("\n── All Reservations ───────────────────────");
        List<Reservation> list = system.getReservations();
        if (list.isEmpty()) { System.out.println("  No reservations."); return; }
        for (Reservation r : list) System.out.println("  → " + r);
    }

    private static void doApproveReservation(Staff staff) {
        System.out.println("\n── Approve Reservation ────────────────────");
        doViewAllReservations();
        try {
            Reservation res = system.findReservation(prompt("Reservation ID to approve"));
            staff.approveReservation(res);
            System.out.println("  ✔ Approved.");
        } catch (Exception e) {
            System.out.println("  ✘ " + e.getMessage());
        }
    }

    private static void doOverrideReservation(Staff staff) {
        System.out.println("\n── Override Reservation ───────────────────");
        doViewAllReservations();
        try {
            Reservation res = system.findReservation(prompt("Reservation ID to override"));
            staff.overrideReservation(res, prompt("Reason"));
            System.out.println("  ✔ Reservation overridden.");
        } catch (Exception e) {
            System.out.println("  ✘ " + e.getMessage());
        }
    }

    private static void doAddRoom(Staff staff) {
        System.out.println("\n── Add Room ───────────────────────────────");
        try {
            Room r = new Room(prompt("Room ID (e.g. H-110)"),
                              prompt("Building name"),
                              promptInt("Capacity"));
            staff.addRoom(system.getRooms(), r);
            System.out.println("  ✔ Room added.");
        } catch (Exception e) {
            System.out.println("  ✘ " + e.getMessage());
        }
    }

    private static void doUpdateEquipment(Staff staff) {
        System.out.println("\n── Update Equipment ───────────────────────");
        for (Equipment eq : system.getEquipment()) System.out.println("  → " + eq);
        Equipment item = findEquipment(prompt("Equipment ID"));
        if (item == null) { System.out.println("  Equipment not found."); return; }
        System.out.println("  Status options: available | maintenance | unavailable");
        try {
            staff.updateEquipment(item, prompt("New status"), promptInt("New available quantity"));
            System.out.println("  ✔ Equipment updated.");
        } catch (Exception e) {
            System.out.println("  ✘ " + e.getMessage());
        }
    }

    private static void doMarkNoShow() {
        System.out.println("\n── Mark No-Show ───────────────────────────");
        doViewAllReservations();
        try {
            String resID = prompt("Reservation ID");
            Reservation res = system.findReservation(resID);
            system.markNoShow(res.getStudent(), resID);
            System.out.println("  ✔ Marked as no-show. Strike issued.");
        } catch (Exception e) {
            System.out.println("  ✘ " + e.getMessage());
        }
    }

    private static void doConfigurePolicies(Admin admin) {
        System.out.println("\n── Configure Policies ─────────────────────");
        System.out.println("  Current: " + system.getPolicy());
        try {
            admin.configurePolicies(system.getPolicy(),
                promptInt("Max booking duration (minutes)"),
                promptInt("Max reservations per student per day"),
                promptInt("Strike threshold before suspension"));
            System.out.println("  ✔ Policies updated.");
        } catch (Exception e) {
            System.out.println("  ✘ " + e.getMessage());
        }
    }

    private static void doSuspendStudent(Admin admin) {
        System.out.println("\n── Suspend Student ────────────────────────");
        for (Student s : system.getStudents())
            System.out.printf("  → %-8s %-20s strikes: %d%n",
                    s.getUserID(), s.getName(), s.getStrikeCount());
        Student target = findStudent(prompt("Student User ID"));
        if (target == null) { System.out.println("  Student not found."); return; }
        admin.suspendStudent(target, prompt("Reason"));
        System.out.println("  ✔ Student suspended.");
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════

    private static TimeSlot promptTimeSlot() {
        System.out.println("  Date format: yyyy-MM-dd  |  Time format: HH:mm");
        try {
            LocalDate date  = LocalDate.parse(
                    prompt("  Date       (e.g. " + LocalDate.now().plusDays(1) + ")"),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalTime start = LocalTime.parse(prompt("  Start time (e.g. 09:00)"),
                    DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime end   = LocalTime.parse(prompt("  End time   (e.g. 11:00)"),
                    DateTimeFormatter.ofPattern("HH:mm"));
            return new TimeSlot(LocalDateTime.of(date, start), LocalDateTime.of(date, end));
        } catch (DateTimeParseException e) {
            System.out.println("  Invalid date/time format."); return null;
        } catch (IllegalArgumentException e) {
            System.out.println("  " + e.getMessage()); return null;
        }
    }

    private static String prompt(String label) {
        System.out.print("  " + label + ": ");
        return scanner.nextLine().trim();
    }

    private static int promptInt(String label) {
        while (true) {
            try {
                System.out.print("  " + label + ": ");
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a valid number.");
            }
        }
    }

    private static Room      findRoom(String id)  {
        for (Room r : system.getRooms())         if (r.getRoomID().equalsIgnoreCase(id))  return r; return null; }
    private static Equipment findEquipment(String id) {
        for (Equipment e : system.getEquipment()) if (e.getItemID().equalsIgnoreCase(id)) return e; return null; }
    private static Student   findStudent(String id)   {
        for (Student s : system.getStudents())   if (s.getUserID().equals(id))            return s; return null; }

    private static void exit() { System.out.println("\n  Goodbye!\n"); System.exit(0); }

    // ═══════════════════════════════════════════════════════════════
    //  BANNER & SEED DATA
    // ═══════════════════════════════════════════════════════════════

    private static void printBanner() {
        System.out.println("\n" + "=".repeat(52));
        System.out.println("   Concordia Study Space & Equipment Booking System");
        System.out.println("   COEN 6312 – Winter 2026");
        System.out.println("=".repeat(52));
        System.out.println("   Pre-loaded demo accounts:");
        System.out.println("   Student  →  S001 / alice123");
        System.out.println("   Student  →  S002 / bob456");
        System.out.println("   Staff    →  ST01 / staff001");
        System.out.println("   Admin    →  AD01 / admin999");
        System.out.println("=".repeat(52));
    }

    private static void seedSystem() {
        // Rooms
        system.addRoom(new Room("H-110", "Hall Building", 10));
        system.addRoom(new Room("H-820", "Hall Building",  6));
        system.addRoom(new Room("EV-1",  "EV Building",    8));
        system.addRoom(new Room("EV-3",  "EV Building",   12));
        system.addRoom(new Room("LB-2",  "Library",        4));
        system.addRoom(new Room("LB-5",  "Library",        6));

        // Equipment
        system.addEquipment(new Equipment("EQ-01", "Projector",    3));
        system.addEquipment(new Equipment("EQ-02", "HDMI Cable",   8));
        system.addEquipment(new Equipment("EQ-03", "Monitor",      5));
        system.addEquipment(new Equipment("EQ-04", "Whiteboard",   4));
        system.addEquipment(new Equipment("EQ-05", "Laptop Stand", 6));

        // Students
        system.registerStudent(new Student("S001", "Alice Martin", "alice@concordia.ca", "alice123", "Computer Engineering"));
        system.registerStudent(new Student("S002", "Bob Nguyen",   "bob@concordia.ca",   "bob456",   "Electrical Engineering"));

        // Staff
        system.registerStaff(new Staff("ST01", "Dr. Lee", "lee@concordia.ca", "staff001", "STF-100", "Library"));

        // Admin
        system.registerStaff(new Admin("AD01", "Admin", "admin@concordia.ca", "admin999", "STF-000", "IT"));
    }
}
