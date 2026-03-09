import java.time.LocalDateTime;

/**
 * Tests for the BookingSystem service class (integration-level).
 */
public class BookingSystemTests {

    public static void run() {
        System.out.println("\n[ BookingSystemTests ]");

        test_bookRoom_createsConfirmedReservation();
        test_bookRoom_marksRoomUnavailable();
        test_bookRoom_preventsDoubleBooking();
        test_bookRoom_allowsDifferentRoomsSameTime();
        test_bookRoom_enforcesDailyLimit();
        test_bookRoom_requiresLogin();
        test_bookRoom_blocksSuspendedStudent();
        test_bookRoom_failsCapacityViolation();
        test_addEquipment_reducesAvailableQty();
        test_addEquipment_failsIfInsufficient();
        test_cancelReservation_setsStatus();
        test_cancelReservation_releasesEquipment();
        test_markNoShow_issuesStrike();
        test_searchAvailableRooms_excludesBooked();
    }

    static void test_bookRoom_createsConfirmedReservation() {
        Setup s = new Setup();
        Reservation res = s.system.bookRoom(s.alice, s.roomH110, s.slot1, 5);
        TestRunner.assertEquals("bookRoom() creates CONFIRMED reservation",
                ReservationStatus.CONFIRMED, res.getStatus());
    }

    static void test_bookRoom_marksRoomUnavailable() {
        Setup s = new Setup();
        s.system.bookRoom(s.alice, s.roomH110, s.slot1, 5);
        TestRunner.assertTrue("bookRoom() marks room unavailable", !s.roomH110.isAvailable());
    }

    static void test_bookRoom_preventsDoubleBooking() {
        Setup s = new Setup();
        s.system.bookRoom(s.alice, s.roomH110, s.slot1, 5);
        // overlapping slot
        TimeSlot overlap = new TimeSlot(s.day1_9am.plusMinutes(30), s.day1_9am.plusHours(3));
        TestRunner.assertThrows("bookRoom() prevents double-booking same room",
                () -> s.system.bookRoom(s.bob, s.roomH110, overlap, 3));
    }

    static void test_bookRoom_allowsDifferentRoomsSameTime() {
        Setup s = new Setup();
        s.system.bookRoom(s.alice, s.roomH110, s.slot1, 5);
        // bob books a different room at the same time — should succeed
        try {
            s.system.bookRoom(s.bob, s.roomEV1, s.slot1, 3);
            TestRunner.assertTrue("bookRoom() allows different rooms at same time", true);
        } catch (Exception e) {
            TestRunner.assertTrue("bookRoom() allows different rooms at same time [FAIL]: " + e.getMessage(), false);
        }
    }

    static void test_bookRoom_enforcesDailyLimit() {
        Setup s = new Setup();
        s.system.getPolicy().setMaxReservationsPerDay(1);
        s.system.bookRoom(s.alice, s.roomH110, s.slot1, 5);
        // second booking same day
        TimeSlot slot2 = new TimeSlot(s.day1_9am.plusHours(3), s.day1_9am.plusHours(5));
        TestRunner.assertThrows("bookRoom() enforces daily reservation limit",
                () -> s.system.bookRoom(s.alice, s.roomEV1, slot2, 2));
    }

    static void test_bookRoom_requiresLogin() {
        Setup s = new Setup();
        s.alice.logout();
        TestRunner.assertThrows("bookRoom() requires student to be logged in",
                () -> s.system.bookRoom(s.alice, s.roomH110, s.slot1, 5));
    }

    static void test_bookRoom_blocksSuspendedStudent() {
        Setup s = new Setup();
        for (int i = 0; i < Student.MAX_STRIKES; i++) s.alice.addStrike("x");
        TestRunner.assertThrows("bookRoom() blocks suspended student",
                () -> s.system.bookRoom(s.alice, s.roomH110, s.slot1, 5));
    }

    static void test_bookRoom_failsCapacityViolation() {
        Setup s = new Setup();
        // roomH110 capacity = 10, requesting 20
        TestRunner.assertThrows("bookRoom() fails when attendees exceed room capacity",
                () -> s.system.bookRoom(s.alice, s.roomH110, s.slot1, 20));
    }

    static void test_addEquipment_reducesAvailableQty() {
        Setup s = new Setup();
        Reservation res = s.system.bookRoom(s.alice, s.roomH110, s.slot1, 5);
        s.system.addEquipmentToReservation(res, s.projector, 2);
        TestRunner.assertEquals("addEquipmentToReservation() reduces available qty",
                1, s.projector.getQuantityAvailable());
    }

    static void test_addEquipment_failsIfInsufficient() {
        Setup s = new Setup();
        Reservation res = s.system.bookRoom(s.alice, s.roomH110, s.slot1, 5);
        TestRunner.assertThrows("addEquipmentToReservation() fails when qty insufficient",
                () -> s.system.addEquipmentToReservation(res, s.projector, 100));
    }

    static void test_cancelReservation_setsStatus() {
        Setup s = new Setup();
        // Use far-future slot so no late-cancel strike
        TimeSlot farFuture = new TimeSlot(
                LocalDateTime.now().plusDays(10).withHour(9).withMinute(0).withSecond(0).withNano(0),
                LocalDateTime.now().plusDays(10).withHour(11).withMinute(0).withSecond(0).withNano(0));
        Reservation res = s.system.bookRoom(s.alice, s.roomH110, farFuture, 5);
        s.system.cancelReservation(s.alice, res.getReservationID());
        TestRunner.assertEquals("cancelReservation() sets status to CANCELLED",
                ReservationStatus.CANCELLED, res.getStatus());
    }

    static void test_cancelReservation_releasesEquipment() {
        Setup s = new Setup();
        TimeSlot farFuture = new TimeSlot(
                LocalDateTime.now().plusDays(10).withHour(9).withMinute(0).withSecond(0).withNano(0),
                LocalDateTime.now().plusDays(10).withHour(11).withMinute(0).withSecond(0).withNano(0));
        Reservation res = s.system.bookRoom(s.alice, s.roomH110, farFuture, 5);
        s.system.addEquipmentToReservation(res, s.projector, 2);
        TestRunner.assertEquals("Equipment qty reduced before cancel", 1, s.projector.getQuantityAvailable());
        s.system.cancelReservation(s.alice, res.getReservationID());
        TestRunner.assertEquals("cancelReservation() releases equipment back",
                3, s.projector.getQuantityAvailable());
    }

    static void test_markNoShow_issuesStrike() {
        Setup s = new Setup();
        Reservation res = s.system.bookRoom(s.bob, s.roomH110, s.slot1, 3);
        s.system.markNoShow(s.bob, res.getReservationID());
        TestRunner.assertEquals("markNoShow() issues 1 strike", 1, s.bob.getStrikeCount());
        TestRunner.assertEquals("markNoShow() sets status NO_SHOW",
                ReservationStatus.NO_SHOW, res.getStatus());
    }

    static void test_searchAvailableRooms_excludesBooked() {
        Setup s = new Setup();
        s.system.bookRoom(s.alice, s.roomH110, s.slot1, 5);
        java.util.List<Room> available = s.system.searchAvailableRooms(s.slot1);
        TestRunner.assertTrue("searchAvailableRooms() excludes booked room",
                !available.contains(s.roomH110));
        TestRunner.assertTrue("searchAvailableRooms() includes free room",
                available.contains(s.roomEV1));
    }

    // ── Inner setup helper ─────────────────────────────────────────

    static class Setup {
        BookingSystem system  = new BookingSystem();
        Room  roomH110        = new Room("H-110", "Hall", 10);
        Room  roomEV1         = new Room("EV-1",  "EV",    6);
        Equipment projector   = new Equipment("EQ-01", "Projector", 3);
        Student alice;
        Student bob;
        LocalDateTime day1_9am;
        TimeSlot slot1;

        Setup() {
            alice = new Student("S001", "Alice", "alice@x.com", "pw", "CS");
            bob   = new Student("S002", "Bob",   "bob@x.com",   "pw", "EE");
            alice.login("pw");
            bob.login("pw");
            system.registerStudent(alice);
            system.registerStudent(bob);
            system.addRoom(roomH110);
            system.addRoom(roomEV1);
            system.addEquipment(projector);
            day1_9am = LocalDateTime.now().plusDays(1)
                    .withHour(9).withMinute(0).withSecond(0).withNano(0);
            slot1 = new TimeSlot(day1_9am, day1_9am.plusHours(2));
        }
    }
}
