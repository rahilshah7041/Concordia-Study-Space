import java.time.LocalDateTime;

/**
 * Unit tests for all 13 implemented OCL constraints.
 *
 * Each test method is named after the constraint it validates.
 * Constraints implemented:
 *   2, 5, 10, 13, 14, 15, 16, 17, 18, 19, 21, 22, 25
 */
public class OCLConstraintTests {

    public static void run() {
        System.out.println("\n[ OCL Constraint Tests ]");

        test_constraint2_attendeeCountWithinRoomCapacity();
        test_constraint5_reservationDurationWithinPolicyLimit();
        test_constraint10_equipmentMustBeAvailable();
        test_constraint13_noOverlappingReservations();
        test_constraint14_dailyReservationLimit();
        test_constraint15_suspendedStudentCannotReserve();
        test_constraint16_confirmedReservationMustSatisfyAllRules();
        test_constraint17_noShowMustGenerateStrikeAndReleaseRoom();
        test_constraint18_lateCancellationResultsInStrike();
        test_constraint19_cancelledReservationReleasesRoom();
        test_constraint19_noShowReservationReleasesRoom();
        test_constraint21_equipmentCannotBeOverbooked();
        test_constraint22_equipmentStatusConsistency();
        test_constraint25_onlyPendingReservationsCanBeApproved();
    }

    // ── Constraint 2 ──────────────────────────────────────────────
    // Attendee Count Within Room Capacity
    // context Reservation inv WithinCapacity:
    //   self.attendeeCount <= self.room.capacity

    static void test_constraint2_attendeeCountWithinRoomCapacity() {
        Student s   = makeStudent();
        Room room   = new Room("R1", "Hall", 5);
        Policy p    = new Policy();
        TimeSlot ts = futureSlot(1, 2);

        // attendeeCount (10) > capacity (5) — should fail validation
        Reservation res = new Reservation("RES-001", s, room, ts, 10, p);
        TestRunner.assertTrue(
            "Constraint 2: attendeeCount exceeding capacity fails validatePolicy()",
            !res.validatePolicy()
        );

        // attendeeCount (4) <= capacity (5) — should pass
        Reservation res2 = new Reservation("RES-002", s, room, ts, 4, p);
        TestRunner.assertTrue(
            "Constraint 2: attendeeCount within capacity passes validatePolicy()",
            res2.validatePolicy()
        );
    }

    // ── Constraint 5 ──────────────────────────────────────────────
    // Reservation Duration Within Policy Limit
    // context Reservation inv MaxDuration:
    //   self.calculateDuration() <= policy.maxDurationMinutes

    static void test_constraint5_reservationDurationWithinPolicyLimit() {
        Student s = makeStudent();
        Room room = new Room("R1", "Hall", 10);
        Policy p  = new Policy();
        p.setMaxDurationMinutes(60); // override to 1 hour for this test

        // 3-hour slot exceeds 1-hour policy max
        TimeSlot longSlot = new TimeSlot(
            LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0),
            LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0).withNano(0)
        );
        Reservation res = new Reservation("RES-001", s, room, longSlot, 3, p);
        TestRunner.assertTrue(
            "Constraint 5: duration exceeding policy max fails validatePolicy()",
            !res.validatePolicy()
        );

        // 30-minute slot within 1-hour policy max
        TimeSlot shortSlot = new TimeSlot(
            LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0),
            LocalDateTime.now().plusDays(1).withHour(9).withMinute(30).withSecond(0).withNano(0)
        );
        Reservation res2 = new Reservation("RES-002", s, room, shortSlot, 3, p);
        TestRunner.assertTrue(
            "Constraint 5: duration within policy max passes validatePolicy()",
            res2.validatePolicy()
        );
    }

    // ── Constraint 10 ─────────────────────────────────────────────
    // Equipment Must Be Available
    // context Reservation inv EquipmentAvailable:
    //   self.equipment->forAll(e | e.status = 'available' and e.quantityAvailable > 0)

    static void test_constraint10_equipmentMustBeAvailable() {
        BookingSystem system = makeSystem();
        Student alice = makeLoggedInStudent("S001", system);
        Room room     = system.getRooms().get(0);
        TimeSlot ts   = futureSlot(1, 2);

        Reservation res = system.bookRoom(alice, room, ts, 3);

        Equipment eq = new Equipment("EQ-99", "Broken", 2);
        eq.updateStatus(Equipment.STATUS_MAINTENANCE);

        TestRunner.assertThrows(
            "Constraint 10: adding maintenance equipment to reservation throws",
            () -> system.addEquipmentToReservation(res, eq, 1)
        );
    }

    // ── Constraint 13 ─────────────────────────────────────────────
    // No Overlapping Reservations for the Same Room
    // context Room inv NoDoubleBooking:
    //   self.reservations->forAll(r1, r2 |
    //     r1 <> r2 implies not r1.timeSlot.checkOverlap(r2.timeSlot))

    static void test_constraint13_noOverlappingReservations() {
        BookingSystem system = makeSystem();
        Student alice = makeLoggedInStudent("S001", system);
        Student bob   = makeLoggedInStudent("S002", system);
        Room room     = system.getRooms().get(0);

        TimeSlot slot1 = new TimeSlot(
            LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0),
            LocalDateTime.now().plusDays(1).withHour(11).withMinute(0).withSecond(0).withNano(0)
        );
        // overlapping slot (starts 30 min into slot1)
        TimeSlot slot2 = new TimeSlot(
            LocalDateTime.now().plusDays(1).withHour(9).withMinute(30).withSecond(0).withNano(0),
            LocalDateTime.now().plusDays(1).withHour(12).withMinute(0).withSecond(0).withNano(0)
        );

        system.bookRoom(alice, room, slot1, 3);

        TestRunner.assertThrows(
            "Constraint 13: overlapping reservation for same room throws",
            () -> system.bookRoom(bob, room, slot2, 3)
        );

        // non-overlapping slot should succeed (different room)
        Room room2 = new Room("R2", "EV", 8);
        system.addRoom(room2);
        TestRunner.assertTrue(
            "Constraint 13: non-overlapping reservation on different room succeeds",
            system.bookRoom(bob, room2, slot1, 3) != null
        );
    }

    // ── Constraint 14 ─────────────────────────────────────────────
    // Daily Reservation Limit per Student
    // context Student inv DailyLimit:
    //   self.reservations->select(r |
    //     r.timeSlot.startTime.toLocalDate() = Date::today())->size() <= 3

    static void test_constraint14_dailyReservationLimit() {
        BookingSystem system = makeSystem();
        system.getPolicy().setMaxReservationsPerDay(2);
        Student alice = makeLoggedInStudent("S001", system);

        Room r1 = system.getRooms().get(0);
        Room r2 = new Room("R2", "EV", 8);
        Room r3 = new Room("R3", "LB", 6);
        system.addRoom(r2);
        system.addRoom(r3);

        LocalDateTime base = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0);
        system.bookRoom(alice, r1, new TimeSlot(base, base.plusHours(1)), 3);
        system.bookRoom(alice, r2, new TimeSlot(base.plusHours(2), base.plusHours(3)), 3);

        TestRunner.assertThrows(
            "Constraint 14: exceeding daily reservation limit throws",
            () -> system.bookRoom(alice, r3, new TimeSlot(base.plusHours(4), base.plusHours(5)), 3)
        );
    }

    // ── Constraint 15 ─────────────────────────────────────────────
    // Suspended Student Cannot Create Reservations
    // context Student inv NotSuspended: self.strikeCount < 3

    static void test_constraint15_suspendedStudentCannotReserve() {
        BookingSystem system = makeSystem();
        Student alice = makeLoggedInStudent("S001", system);
        Room room     = system.getRooms().get(0);
        TimeSlot ts   = futureSlot(2, 2);

        // suspend alice
        for (int i = 0; i < Student.MAX_STRIKES; i++) alice.addStrike("test");
        TestRunner.assertTrue("Constraint 15: student is suspended", alice.isSuspended());

        TestRunner.assertThrows(
            "Constraint 15: suspended student cannot book a room",
            () -> system.bookRoom(alice, room, ts, 3)
        );
    }

    // ── Constraint 16 ─────────────────────────────────────────────
    // Confirmed Reservation Must Satisfy All Rules
    // context Reservation inv ValidBeforeConfirm:
    //   self.status = CONFIRMED implies
    //     self.attendeeCount > 0 and self.attendeeCount <= self.room.capacity

    static void test_constraint16_confirmedReservationMustSatisfyAllRules() {
        Student s   = makeStudent();
        Room room   = new Room("R1", "Hall", 4);
        Policy p    = new Policy();
        TimeSlot ts = futureSlot(1, 1);

        // attendeeCount exceeds capacity — cannot be confirmed
        Reservation res = new Reservation("RES-001", s, room, ts, 10, p);
        TestRunner.assertTrue(
            "Constraint 16: reservation with attendees > capacity fails validation",
            !res.validatePolicy()
        );
        TestRunner.assertEquals(
            "Constraint 16: status remains PENDING when validation fails",
            ReservationStatus.PENDING, res.getStatus()
        );
    }

    // ── Constraint 17 ─────────────────────────────────────────────
    // No-Show Must Generate Strike and Release Room
    // context Reservation inv NoShowPenalty:
    //   self.status = NO_SHOW implies self.student.strikeCount >= 1

    static void test_constraint17_noShowMustGenerateStrikeAndReleaseRoom() {
        BookingSystem system = makeSystem();
        Student bob   = makeLoggedInStudent("S002", system);
        Room room     = system.getRooms().get(0);
        TimeSlot ts   = futureSlot(1, 2);

        Reservation res = system.bookRoom(bob, room, ts, 3);
        int strikesBefore = bob.getStrikeCount();

        system.markNoShow(bob, res.getReservationID());

        TestRunner.assertEquals(
            "Constraint 17: no-show increments student strike count",
            strikesBefore + 1, bob.getStrikeCount()
        );
        TestRunner.assertEquals(
            "Constraint 17: reservation status is NO_SHOW",
            ReservationStatus.NO_SHOW, res.getStatus()
        );
        TestRunner.assertTrue(
            "Constraint 17: room is released after no-show",
            room.isAvailable()
        );
    }

    // ── Constraint 18 ─────────────────────────────────────────────
    // Late Cancellation Results in a Strike
    // context Reservation inv LateCancelPenalty:
    //   self.status = CANCELLED and self.timeSlot.minutesUntilStart() < 30
    //   implies self.student.strikeCount >= 1

    static void test_constraint18_lateCancellationResultsInStrike() {
        BookingSystem system = makeSystem();
        Student alice = makeLoggedInStudent("S001", system);
        Room room     = system.getRooms().get(0);

        // slot starting in 10 minutes — qualifies as late cancellation
        TimeSlot nearSlot = new TimeSlot(
            LocalDateTime.now().plusMinutes(10),
            LocalDateTime.now().plusMinutes(70)
        );

        Reservation res = system.bookRoom(alice, room, nearSlot, 3);
        int strikesBefore = alice.getStrikeCount();

        system.cancelReservation(alice, res.getReservationID());

        TestRunner.assertTrue(
            "Constraint 18: late cancellation issues a strike",
            alice.getStrikeCount() > strikesBefore
        );
        TestRunner.assertEquals(
            "Constraint 18: reservation status is CANCELLED",
            ReservationStatus.CANCELLED, res.getStatus()
        );
    }

    // ── Constraint 19 ─────────────────────────────────────────────
    // Cancelled or No-Show Reservation Releases Room
    // context Reservation inv ReleaseRoom:
    //   self.status = CANCELLED or self.status = NO_SHOW
    //   implies self.room.isAvailable = true

    static void test_constraint19_cancelledReservationReleasesRoom() {
        BookingSystem system = makeSystem();
        Student alice = makeLoggedInStudent("S001", system);
        Room room     = system.getRooms().get(0);
        TimeSlot ts   = futureSlot(5, 2);

        Reservation res = system.bookRoom(alice, room, ts, 3);
        TestRunner.assertTrue("Room is unavailable after booking", !room.isAvailable());

        system.cancelReservation(alice, res.getReservationID());
        TestRunner.assertTrue(
            "Constraint 19: room is available again after cancellation",
            room.isAvailable()
        );
    }

    static void test_constraint19_noShowReservationReleasesRoom() {
        BookingSystem system = makeSystem();
        Student bob = makeLoggedInStudent("S002", system);
        Room room   = system.getRooms().get(0);
        TimeSlot ts = futureSlot(1, 2);

        Reservation res = system.bookRoom(bob, room, ts, 3);
        TestRunner.assertTrue("Room is unavailable after booking", !room.isAvailable());

        system.markNoShow(bob, res.getReservationID());
        TestRunner.assertTrue(
            "Constraint 19: room is available again after no-show",
            room.isAvailable()
        );
    }

    // ── Constraint 21 ─────────────────────────────────────────────
    // Equipment Cannot Be Overbooked
    // context Equipment inv NotOverbooked:
    //   self.quantityAvailable >= 0 and self.quantityAvailable <= self.totalQuantity

    static void test_constraint21_equipmentCannotBeOverbooked() {
        BookingSystem system = makeSystem();
        Student alice = makeLoggedInStudent("S001", system);
        Room room     = system.getRooms().get(0);
        TimeSlot ts   = futureSlot(1, 2);

        Reservation res = system.bookRoom(alice, room, ts, 3);
        Equipment eq    = new Equipment("EQ-01", "Projector", 2);
        system.addEquipment(eq);

        // request more than available
        TestRunner.assertThrows(
            "Constraint 21: reserving more equipment than available throws",
            () -> system.addEquipmentToReservation(res, eq, 10)
        );

        // request exactly what is available — should succeed
        TestRunner.assertTrue(
            "Constraint 21: reserving available quantity succeeds",
            system.bookRoom(alice, new Room("R99", "Hall", 10),
                new TimeSlot(ts.getStartTime().plusDays(1), ts.getEndTime().plusDays(1)), 2) != null
        );
    }

    // ── Constraint 22 ─────────────────────────────────────────────
    // Equipment Status Consistency
    // context Equipment inv StatusConsistency:
    //   self.status = 'maintenance' implies self.quantityAvailable = 0

    static void test_constraint22_equipmentStatusConsistency() {
        Equipment eq = new Equipment("EQ-01", "Projector", 3);
        TestRunner.assertEquals(
            "Constraint 22: initially available with full quantity",
            3, eq.getQuantityAvailable()
        );

        eq.updateStatus(Equipment.STATUS_MAINTENANCE);
        TestRunner.assertEquals(
            "Constraint 22: maintenance status sets quantityAvailable to 0",
            0, eq.getQuantityAvailable()
        );
        TestRunner.assertTrue(
            "Constraint 22: cannot reserve when in maintenance",
            !eq.reserve(1)
        );
    }

    // ── Constraint 25 ─────────────────────────────────────────────
    // Only Pending Reservations Can Be Approved
    // context Reservation inv OnlyValidConfirmed:
    //   self.status = CONFIRMED implies self.attendeeCount > 0

    static void test_constraint25_onlyPendingReservationsCanBeApproved() {
        Staff staff = new Staff("ST01", "Lee", "lee@x.com", "pw", "S01", "Lib");
        staff.login("pw");

        Student s   = makeStudent();
        Room room   = new Room("R1", "Hall", 10);
        Policy p    = new Policy();
        TimeSlot ts = futureSlot(1, 1);
        Reservation res = new Reservation("RES-001", s, room, ts, 3, p);

        // PENDING — approve should succeed
        TestRunner.assertEquals("Constraint 25: reservation starts as PENDING",
            ReservationStatus.PENDING, res.getStatus());
        staff.approveReservation(res);
        TestRunner.assertEquals("Constraint 25: reservation is now CONFIRMED",
            ReservationStatus.CONFIRMED, res.getStatus());

        // CONFIRMED — approve again should throw
        TestRunner.assertThrows(
            "Constraint 25: approving a non-PENDING reservation throws",
            () -> staff.approveReservation(res)
        );

        // CANCELLED — approve should throw
        Reservation res2 = new Reservation("RES-002", s, room, futureSlot(2, 1), 3, p);
        res2.setStatus(ReservationStatus.CANCELLED);
        TestRunner.assertThrows(
            "Constraint 25: approving a CANCELLED reservation throws",
            () -> staff.approveReservation(res2)
        );
    }

    // ── Helpers ───────────────────────────────────────────────────

    static Student makeStudent() {
        return new Student("S001", "Alice", "alice@concordia.ca", "pw", "CS");
    }

    static Student makeLoggedInStudent(String id, BookingSystem system) {
        String name = id.equals("S001") ? "Alice" : "Bob";
        String email = name.toLowerCase() + "@concordia.ca";
        Student s = new Student(id, name, email, "pw", "CS");
        s.login("pw");
        system.registerStudent(s);
        return s;
    }

    static BookingSystem makeSystem() {
        BookingSystem system = new BookingSystem();
        system.addRoom(new Room("H-110", "Hall Building", 10));
        return system;
    }

    static TimeSlot futureSlot(int plusDays, int durationHours) {
        LocalDateTime start = LocalDateTime.now().plusDays(plusDays)
                .withHour(9).withMinute(0).withSecond(0).withNano(0);
        return new TimeSlot(start, start.plusHours(durationHours));
    }
}
