/**
 * Tests for Room and Equipment classes.
 */
public class RoomEquipmentTests {

    public static void run() {
        System.out.println("\n[ RoomEquipmentTests ]");

        test_room_availableByDefault();
        test_room_checkCapacity_fits();
        test_room_checkCapacity_exceeds();
        test_room_invalidCapacity_throws();
        test_room_blankBuilding_throws();
        test_room_toggleAvailability();

        test_equipment_initiallyFullyAvailable();
        test_equipment_reserve_reducesQty();
        test_equipment_reserve_insufficient();
        test_equipment_release_returnsQty();
        test_equipment_release_capsAtTotal();
        test_equipment_maintenance_zerosQty();
        test_equipment_cannotReserveWhenMaintenance();
        test_equipment_zeroQty_throws();
    }

    // ── Room ──────────────────────────────────────────────────────

    static void test_room_availableByDefault() {
        Room r = new Room("R1", "Hall", 10);
        TestRunner.assertTrue("Room is available by default", r.isAvailable());
    }

    static void test_room_checkCapacity_fits() {
        Room r = new Room("R1", "Hall", 10);
        TestRunner.assertTrue("checkCapacity() true when attendees <= capacity", r.checkCapacity(10));
        TestRunner.assertTrue("checkCapacity() true when attendees < capacity",  r.checkCapacity(1));
    }

    static void test_room_checkCapacity_exceeds() {
        Room r = new Room("R1", "Hall", 5);
        TestRunner.assertTrue("checkCapacity() false when attendees exceed capacity",
                !r.checkCapacity(6));
    }

    static void test_room_invalidCapacity_throws() {
        TestRunner.assertThrows("Room rejects zero capacity",
                () -> new Room("R1", "H", 0));
        TestRunner.assertThrows("Room rejects negative capacity",
                () -> new Room("R1", "H", -3));
    }

    static void test_room_blankBuilding_throws() {
        TestRunner.assertThrows("Room rejects blank building",
                () -> new Room("R1", " ", 5));
    }

    static void test_room_toggleAvailability() {
        Room r = new Room("R1", "Hall", 10);
        r.setAvailable(false);
        TestRunner.assertTrue("setAvailable(false) works", !r.isAvailable());
        r.setAvailable(true);
        TestRunner.assertTrue("setAvailable(true) works", r.isAvailable());
    }

    // ── Equipment ─────────────────────────────────────────────────

    static void test_equipment_initiallyFullyAvailable() {
        Equipment e = new Equipment("EQ-01", "Projector", 3);
        TestRunner.assertEquals("Equipment starts fully available", 3, e.getQuantityAvailable());
        TestRunner.assertEquals("Equipment status is available",
                Equipment.STATUS_AVAILABLE, e.getStatus());
    }

    static void test_equipment_reserve_reducesQty() {
        Equipment e = new Equipment("EQ-01", "Projector", 3);
        boolean ok = e.reserve(2);
        TestRunner.assertTrue("reserve() returns true on success", ok);
        TestRunner.assertEquals("reserve() reduces available qty", 1, e.getQuantityAvailable());
    }

    static void test_equipment_reserve_insufficient() {
        Equipment e = new Equipment("EQ-01", "Projector", 2);
        boolean ok = e.reserve(5);
        TestRunner.assertTrue("reserve() returns false when insufficient", !ok);
        TestRunner.assertEquals("qty unchanged after failed reserve", 2, e.getQuantityAvailable());
    }

    static void test_equipment_release_returnsQty() {
        Equipment e = new Equipment("EQ-01", "Projector", 4);
        e.reserve(3);
        e.release(2);
        TestRunner.assertEquals("release() returns units to pool", 3, e.getQuantityAvailable());
    }

    static void test_equipment_release_capsAtTotal() {
        Equipment e = new Equipment("EQ-01", "Projector", 3);
        e.release(10);
        TestRunner.assertEquals("release() does not exceed total", 3, e.getQuantityAvailable());
    }

    static void test_equipment_maintenance_zerosQty() {
        Equipment e = new Equipment("EQ-01", "Projector", 3);
        e.updateStatus(Equipment.STATUS_MAINTENANCE);
        TestRunner.assertEquals("Maintenance status zeroes available qty", 0, e.getQuantityAvailable());
    }

    static void test_equipment_cannotReserveWhenMaintenance() {
        Equipment e = new Equipment("EQ-01", "Projector", 3);
        e.updateStatus(Equipment.STATUS_MAINTENANCE);
        boolean ok = e.reserve(1);
        TestRunner.assertTrue("Cannot reserve when in maintenance", !ok);
    }

    static void test_equipment_zeroQty_throws() {
        TestRunner.assertThrows("Equipment rejects zero totalQuantity",
                () -> new Equipment("EQ-01", "Proj", 0));
    }
}
