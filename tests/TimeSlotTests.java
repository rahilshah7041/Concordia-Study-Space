import java.time.LocalDateTime;

/**
 * Tests for TimeSlot and EquipmentBooking classes.
 */
public class TimeSlotTests {

    public static void run() {
        System.out.println("\n[ TimeSlotTests ]");

        test_timeSlot_endBeforeStart_throws();
        test_timeSlot_overlap_detected();
        test_timeSlot_noOverlap();
        test_timeSlot_adjacentSlots_noOverlap();
        test_timeSlot_isValid_future();
        test_timeSlot_isValid_pastStart();
        test_timeSlot_isValid_exceedsMax();
        test_timeSlot_getDuration();

        test_equipmentBooking_reservesOnCreation();
        test_equipmentBooking_insufficient_throws();
        test_equipmentBooking_release_returnsQty();
        test_equipmentBooking_release_isIdempotent();
    }

    // ── TimeSlot ──────────────────────────────────────────────────

    static void test_timeSlot_endBeforeStart_throws() {
        LocalDateTime now = LocalDateTime.now().plusDays(1);
        TestRunner.assertThrows("TimeSlot rejects end before start",
                () -> new TimeSlot(now.plusHours(2), now.plusHours(1)));
    }

    static void test_timeSlot_overlap_detected() {
        LocalDateTime base = LocalDateTime.now().plusDays(1);
        TimeSlot a = new TimeSlot(base,              base.plusHours(2));
        TimeSlot b = new TimeSlot(base.plusHours(1), base.plusHours(3));
        TestRunner.assertTrue("checkOverlap() detects overlap (a vs b)",  a.checkOverlap(b));
        TestRunner.assertTrue("checkOverlap() detects overlap (b vs a)",  b.checkOverlap(a));
    }

    static void test_timeSlot_noOverlap() {
        LocalDateTime base = LocalDateTime.now().plusDays(1);
        TimeSlot a = new TimeSlot(base,              base.plusHours(2));
        TimeSlot b = new TimeSlot(base.plusHours(3), base.plusHours(5));
        TestRunner.assertTrue("checkOverlap() false for non-overlapping slots", !a.checkOverlap(b));
    }

    static void test_timeSlot_adjacentSlots_noOverlap() {
        LocalDateTime base = LocalDateTime.now().plusDays(1);
        TimeSlot a = new TimeSlot(base,              base.plusHours(2));
        TimeSlot b = new TimeSlot(base.plusHours(2), base.plusHours(4));
        TestRunner.assertTrue("Adjacent slots do not overlap", !a.checkOverlap(b));
    }

    static void test_timeSlot_isValid_future() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        TimeSlot ts = new TimeSlot(start, start.plusHours(2));
        TestRunner.assertTrue("isValidDuration() true for future 2-hour slot", ts.isValidDuration());
    }

    static void test_timeSlot_isValid_pastStart() {
        LocalDateTime past = LocalDateTime.now().minusHours(2);
        TimeSlot ts = new TimeSlot(past, past.plusHours(1));
        TestRunner.assertTrue("isValidDuration() false for past start", !ts.isValidDuration());
    }

    static void test_timeSlot_isValid_exceedsMax() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        TimeSlot ts = new TimeSlot(start, start.plusHours(5)); // 5h > 4h max
        TestRunner.assertTrue("isValidDuration() false when exceeds max duration",
                !ts.isValidDuration());
    }

    static void test_timeSlot_getDuration() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        TimeSlot ts = new TimeSlot(start, start.plusMinutes(90));
        TestRunner.assertEquals("getDurationMinutes() returns correct value", 90L,
                ts.getDurationMinutes());
    }

    // ── EquipmentBooking ──────────────────────────────────────────

    static void test_equipmentBooking_reservesOnCreation() {
        Equipment e  = new Equipment("EQ-01", "Projector", 3);
        EquipmentBooking eb = new EquipmentBooking(e, 2);
        TestRunner.assertEquals("EquipmentBooking reserves qty on creation", 1, e.getQuantityAvailable());
        TestRunner.assertTrue("EquipmentBooking is not released initially", !eb.isReleased());
    }

    static void test_equipmentBooking_insufficient_throws() {
        Equipment e = new Equipment("EQ-01", "Projector", 1);
        TestRunner.assertThrows("EquipmentBooking throws if qty exceeds available",
                () -> new EquipmentBooking(e, 5));
    }

    static void test_equipmentBooking_release_returnsQty() {
        Equipment e  = new Equipment("EQ-01", "Projector", 3);
        EquipmentBooking eb = new EquipmentBooking(e, 2);
        eb.release();
        TestRunner.assertTrue("release() sets released flag", eb.isReleased());
        TestRunner.assertEquals("release() restores qty to pool", 3, e.getQuantityAvailable());
    }

    static void test_equipmentBooking_release_isIdempotent() {
        Equipment e  = new Equipment("EQ-01", "Projector", 3);
        EquipmentBooking eb = new EquipmentBooking(e, 2);
        eb.release();
        eb.release(); // should not double-release
        TestRunner.assertEquals("Double release does not over-restore qty", 3, e.getQuantityAvailable());
    }
}
