import java.time.LocalDateTime;

/**
 * Tests for User, Student, Staff, and Admin classes.
 */
public class UserTests {

    public static void run() {
        System.out.println("\n[ UserTests ]");

        test_user_loginSuccess();
        test_user_loginFailure();
        test_user_logout();
        test_user_blankUserID_throws();
        test_user_invalidEmail_throws();

        test_student_initialStrikes_zero();
        test_student_notSuspendedInitially();
        test_student_suspendedAfterMaxStrikes();
        test_student_searchRooms_requiresLogin();
        test_student_createReservation_requiresLogin();
        test_student_suspendedCannotBook();
        test_student_checkIn_setsCompleted();
        test_student_cancelReservation_setsStatus();
        test_student_noShow_issuesStrike();

        test_staff_addRoom_success();
        test_staff_addDuplicateRoom_throws();
        test_staff_approveReservation_success();
        test_staff_approveNonPending_throws();
        test_staff_requiresLogin();

        test_admin_inheritsStaff();
        test_admin_configurePolicies();
        test_admin_configurePolicies_invalidValues_throws();
    }

    // ── User ──────────────────────────────────────────────────────

    static void test_user_loginSuccess() {
        Student s = new Student("S001", "Alice", "a@x.com", "pw", "CS");
        TestRunner.assertTrue("login() with correct password returns true", s.login("pw"));
        TestRunner.assertTrue("isLoggedIn() is true after login", s.isLoggedIn());
    }

    static void test_user_loginFailure() {
        Student s = new Student("S001", "Alice", "a@x.com", "pw", "CS");
        TestRunner.assertTrue("login() with wrong password returns false", !s.login("wrong"));
        TestRunner.assertTrue("isLoggedIn() is false after failed login", !s.isLoggedIn());
    }

    static void test_user_logout() {
        Student s = new Student("S001", "Alice", "a@x.com", "pw", "CS");
        s.login("pw");
        s.logout();
        TestRunner.assertTrue("isLoggedIn() is false after logout", !s.isLoggedIn());
    }

    static void test_user_blankUserID_throws() {
        TestRunner.assertThrows("Blank userID throws exception",
                () -> new Student("", "Alice", "a@x.com", "pw", "CS"));
    }

    static void test_user_invalidEmail_throws() {
        TestRunner.assertThrows("Invalid email throws exception",
                () -> new Student("S001", "Alice", "notanemail", "pw", "CS"));
    }

    // ── Student ───────────────────────────────────────────────────

    static void test_student_initialStrikes_zero() {
        Student s = new Student("S001", "Alice", "a@x.com", "pw", "CS");
        TestRunner.assertEquals("Student starts with 0 strikes", 0, s.getStrikeCount());
    }

    static void test_student_notSuspendedInitially() {
        Student s = new Student("S001", "Alice", "a@x.com", "pw", "CS");
        TestRunner.assertTrue("Student is not suspended initially", !s.isSuspended());
    }

    static void test_student_suspendedAfterMaxStrikes() {
        Student s = new Student("S001", "Alice", "a@x.com", "pw", "CS");
        for (int i = 0; i < Student.MAX_STRIKES; i++) s.addStrike("test");
        TestRunner.assertTrue("Student is suspended after " + Student.MAX_STRIKES + " strikes",
                s.isSuspended());
    }

    static void test_student_searchRooms_requiresLogin() {
        Student s = new Student("S001", "Alice", "a@x.com", "pw", "CS");
        TestRunner.assertThrows("searchRooms() requires login",
                () -> s.searchRooms(new java.util.ArrayList<>()));
    }

    static void test_student_createReservation_requiresLogin() {
        Student s = new Student("S001", "Alice", "a@x.com", "pw", "CS");
        TestRunner.assertThrows("createReservation() requires login",
                () -> s.createReservation(null));
    }

    static void test_student_suspendedCannotBook() {
        Student s = new Student("S001", "Alice", "a@x.com", "pw", "CS");
        s.login("pw");
        for (int i = 0; i < Student.MAX_STRIKES; i++) s.addStrike("x");
        Room r      = new Room("R1", "Hall", 10);
        Policy p    = new Policy();
        TimeSlot ts = new TimeSlot(LocalDateTime.now().plusDays(1),
                                   LocalDateTime.now().plusDays(1).plusHours(1));
        Reservation res = new Reservation("RES-001", s, r, ts, 2, p);
        TestRunner.assertThrows("Suspended student cannot create reservation",
                () -> s.createReservation(res));
    }

    static void test_student_checkIn_setsCompleted() {
        Student s = makeLoggedInStudent();
        Room r    = new Room("R1", "Hall", 10);
        Policy p  = new Policy();
        TimeSlot ts = new TimeSlot(LocalDateTime.now().plusDays(1),
                                   LocalDateTime.now().plusDays(1).plusHours(1));
        Reservation res = new Reservation("RES-001", s, r, ts, 2, p);
        s.createReservation(res);
        s.checkIn("RES-001");
        TestRunner.assertEquals("checkIn() sets status to COMPLETED",
                ReservationStatus.COMPLETED, res.getStatus());
    }

    static void test_student_cancelReservation_setsStatus() {
        Student s = makeLoggedInStudent();
        Room r    = new Room("R1", "Hall", 10);
        Policy p  = new Policy();
        // Far future so no late-cancel strike
        TimeSlot ts = new TimeSlot(LocalDateTime.now().plusDays(5),
                                   LocalDateTime.now().plusDays(5).plusHours(1));
        Reservation res = new Reservation("RES-001", s, r, ts, 2, p);
        s.createReservation(res);
        s.cancelReservation("RES-001");
        TestRunner.assertEquals("cancelReservation() sets status to CANCELLED",
                ReservationStatus.CANCELLED, res.getStatus());
    }

    static void test_student_noShow_issuesStrike() {
        Student s = makeLoggedInStudent();
        Room r    = new Room("R1", "Hall", 10);
        Policy p  = new Policy();
        TimeSlot ts = new TimeSlot(LocalDateTime.now().plusDays(1),
                                   LocalDateTime.now().plusDays(1).plusHours(1));
        Reservation res = new Reservation("RES-001", s, r, ts, 2, p);
        s.createReservation(res);
        s.markNoShow("RES-001");
        TestRunner.assertEquals("markNoShow() issues 1 strike", 1, s.getStrikeCount());
        TestRunner.assertEquals("markNoShow() sets status to NO_SHOW",
                ReservationStatus.NO_SHOW, res.getStatus());
    }

    // ── Staff ─────────────────────────────────────────────────────

    static void test_staff_addRoom_success() {
        Staff st = makeLoggedInStaff();
        java.util.List<Room> rooms = new java.util.ArrayList<>();
        st.addRoom(rooms, new Room("R1", "Hall", 5));
        TestRunner.assertEquals("Staff.addRoom() adds a room", 1, rooms.size());
    }

    static void test_staff_addDuplicateRoom_throws() {
        Staff st = makeLoggedInStaff();
        java.util.List<Room> rooms = new java.util.ArrayList<>();
        rooms.add(new Room("R1", "Hall", 5));
        TestRunner.assertThrows("Staff cannot add duplicate room",
                () -> st.addRoom(rooms, new Room("R1", "EV", 3)));
    }

    static void test_staff_approveReservation_success() {
        Staff st  = makeLoggedInStaff();
        Student s = makeLoggedInStudent();
        Room r    = new Room("R1", "Hall", 10);
        Policy p  = new Policy();
        TimeSlot ts = new TimeSlot(LocalDateTime.now().plusDays(1),
                                   LocalDateTime.now().plusDays(1).plusHours(1));
        Reservation res = new Reservation("RES-001", s, r, ts, 3, p);
        st.approveReservation(res);
        TestRunner.assertEquals("Staff.approveReservation() sets CONFIRMED",
                ReservationStatus.CONFIRMED, res.getStatus());
    }

    static void test_staff_approveNonPending_throws() {
        Staff st  = makeLoggedInStaff();
        Student s = makeLoggedInStudent();
        Room r    = new Room("R1", "Hall", 10);
        Policy p  = new Policy();
        TimeSlot ts = new TimeSlot(LocalDateTime.now().plusDays(1),
                                   LocalDateTime.now().plusDays(1).plusHours(1));
        Reservation res = new Reservation("RES-001", s, r, ts, 3, p);
        res.setStatus(ReservationStatus.CONFIRMED);
        TestRunner.assertThrows("Staff cannot approve non-PENDING reservation",
                () -> st.approveReservation(res));
    }

    static void test_staff_requiresLogin() {
        Staff st = new Staff("ST01", "Lee", "lee@x.com", "pw", "S01", "Lib");
        TestRunner.assertThrows("Staff.addRoom() requires login",
                () -> st.addRoom(new java.util.ArrayList<>(), new Room("R1", "H", 5)));
    }

    // ── Admin ─────────────────────────────────────────────────────

    static void test_admin_inheritsStaff() {
        Admin a = new Admin("AD01", "Admin", "ad@x.com", "pw", "A01", "IT");
        TestRunner.assertTrue("Admin is an instance of Staff", a instanceof Staff);
        TestRunner.assertTrue("Admin is an instance of User",  a instanceof User);
    }

    static void test_admin_configurePolicies() {
        Admin a = new Admin("AD01", "Admin", "ad@x.com", "pw", "A01", "IT");
        a.login("pw");
        Policy p = new Policy();
        a.configurePolicies(p, 120, 2, 2);
        TestRunner.assertEquals("Admin sets maxDuration", 120, p.getMaxDurationMinutes());
        TestRunner.assertEquals("Admin sets maxPerDay", 2, p.getMaxReservationsPerDay());
        TestRunner.assertEquals("Admin sets strikeThreshold", 2, p.getStrikeThreshold());
    }

    static void test_admin_configurePolicies_invalidValues_throws() {
        Admin a = new Admin("AD01", "Admin", "ad@x.com", "pw", "A01", "IT");
        a.login("pw");
        Policy p = new Policy();
        TestRunner.assertThrows("Admin.configurePolicies() rejects zero duration",
                () -> a.configurePolicies(p, 0, 2, 3));
    }

    // ── Factory helpers ───────────────────────────────────────────

    static Student makeLoggedInStudent() {
        Student s = new Student("S001", "Alice", "a@x.com", "pw", "CS");
        s.login("pw");
        return s;
    }

    static Staff makeLoggedInStaff() {
        Staff st = new Staff("ST01", "Lee", "lee@x.com", "pw", "S01", "Lib");
        st.login("pw");
        return st;
    }
}
