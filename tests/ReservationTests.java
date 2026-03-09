import java.time.LocalDateTime;

/**
 * Tests for the Reservation class.
 */
public class ReservationTests {

    public static void run() {
        System.out.println("\n[ ReservationTests ]");

        test_reservation_initialStatus_isPending();
        test_reservation_calculateDuration();
        test_reservation_validatePolicy_valid();
        test_reservation_validatePolicy_roomUnavailable();
        test_reservation_validatePolicy_capacityExceeded();
        test_reservation_validatePolicy_durationExceedsMax();
        test_reservation_validatePolicy_pastSlot();
        test_reservation_nullRoom_throws();
        test_reservation_zeroAttendees_throws();
        test_reservation_addEquipment();
        test_reservation_releaseAllEquipment();
    }

    static void test_reservation_initialStatus_isPending() {
        Reservation r = makeReservation(1, 1, 10, 5);
        TestRunner.assertEquals("Reservation initial status is PENDING",
                ReservationStatus.PENDING, r.getStatus());
    }

    static void test_reservation_calculateDuration() {
        Reservation r = makeReservation(1, 2, 10, 5); // 2-hour slot = 120 min
        TestRunner.assertEquals("calculateDuration() returns correct minutes", 120L, r.calculateDuration());
    }

    static void test_reservation_validatePolicy_valid() {
        Reservation r = makeReservation(1, 2, 10, 5);
        TestRunner.assertTrue("validatePolicy() passes for valid reservation", r.validatePolicy());
    }

    static void test_reservation_validatePolicy_roomUnavailable() {
        Room room = new Room("R1", "Hall", 10);
        room.setAvailable(false);
        Student s   = makeStudent();
        Policy p    = new Policy();
        TimeSlot ts = futureSlot(1, 2);
        Reservation r = new Reservation("RES-001", s, room, ts, 5, p);
        TestRunner.assertTrue("validatePolicy() fails when room unavailable", !r.validatePolicy());
    }

    static void test_reservation_validatePolicy_capacityExceeded() {
        Room room = new Room("R1", "Hall", 4);
        Student s   = makeStudent();
        Policy p    = new Policy();
        TimeSlot ts = futureSlot(1, 2);
        Reservation r = new Reservation("RES-001", s, room, ts, 10, p); // 10 > 4
        TestRunner.assertTrue("validatePolicy() fails when attendees exceed capacity",
                !r.validatePolicy());
    }

    static void test_reservation_validatePolicy_durationExceedsMax() {
        Room room = new Room("R1", "Hall", 10);
        Student s   = makeStudent();
        Policy p    = new Policy();
        p.setMaxDurationMinutes(60);
        TimeSlot ts = futureSlot(1, 3); // 3 hours > 1 hour policy max
        Reservation r = new Reservation("RES-001", s, room, ts, 5, p);
        TestRunner.assertTrue("validatePolicy() fails when duration exceeds policy max",
                !r.validatePolicy());
    }

    static void test_reservation_validatePolicy_pastSlot() {
        Room room = new Room("R1", "Hall", 10);
        Student s   = makeStudent();
        Policy p    = new Policy();
        LocalDateTime past = LocalDateTime.now().minusHours(3);
        TimeSlot ts = new TimeSlot(past, past.plusHours(1));
        Reservation r = new Reservation("RES-001", s, room, ts, 5, p);
        TestRunner.assertTrue("validatePolicy() fails for past time slot", !r.validatePolicy());
    }

    static void test_reservation_nullRoom_throws() {
        Student s = makeStudent();
        TimeSlot ts = futureSlot(1, 1);
        Policy p  = new Policy();
        TestRunner.assertThrows("Reservation rejects null room",
                () -> new Reservation("RES-001", s, null, ts, 5, p));
    }

    static void test_reservation_zeroAttendees_throws() {
        Student s  = makeStudent();
        Room room  = new Room("R1", "Hall", 10);
        TimeSlot ts = futureSlot(1, 1);
        Policy p   = new Policy();
        TestRunner.assertThrows("Reservation rejects zero attendees",
                () -> new Reservation("RES-001", s, room, ts, 0, p));
    }

    static void test_reservation_addEquipment() {
        Reservation res = makeReservation(1, 2, 10, 5);
        Equipment e = new Equipment("EQ-01", "Projector", 3);
        EquipmentBooking eb = new EquipmentBooking(e, 2);
        res.addEquipmentBooking(eb);
        TestRunner.assertEquals("addEquipmentBooking() adds to list", 1,
                res.getEquipmentBookings().size());
    }

    static void test_reservation_releaseAllEquipment() {
        Reservation res = makeReservation(1, 2, 10, 5);
        Equipment e = new Equipment("EQ-01", "Projector", 3);
        EquipmentBooking eb = new EquipmentBooking(e, 2);
        res.addEquipmentBooking(eb);
        res.releaseAllEquipment();
        TestRunner.assertEquals("releaseAllEquipment() restores qty", 3, e.getQuantityAvailable());
    }

    // ── Helpers ───────────────────────────────────────────────────

    static Reservation makeReservation(int plusDays, int durationHours, int capacity, int attendees) {
        Student s = makeStudent();
        Room room = new Room("R1", "Hall", capacity);
        Policy p  = new Policy();
        TimeSlot ts = futureSlot(plusDays, durationHours);
        return new Reservation("RES-001", s, room, ts, attendees, p);
    }

    static Student makeStudent() {
        return new Student("S001", "Alice", "a@x.com", "pw", "CS");
    }

    static TimeSlot futureSlot(int plusDays, int durationHours) {
        LocalDateTime start = LocalDateTime.now().plusDays(plusDays)
                .withHour(9).withMinute(0).withSecond(0).withNano(0);
        return new TimeSlot(start, start.plusHours(durationHours));
    }
}
