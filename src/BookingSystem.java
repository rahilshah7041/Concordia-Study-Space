import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Central service class that orchestrates all CSEBS operations.
 * Acts as the "System" actor from the use-case diagrams.
 *
 * Responsibilities:
 *  - Stores rooms, equipment, users, and reservations
 *  - Enforces double-booking prevention
 *  - Enforces daily reservation limits
 *  - Sends notifications on booking events
 */
public class BookingSystem {

    private List<Room>        rooms        = new ArrayList<>();
    private List<Equipment>   equipment    = new ArrayList<>();
    private List<Student>     students     = new ArrayList<>();
    private List<Staff>       staffList    = new ArrayList<>();
    private List<Reservation> reservations = new ArrayList<>();
    private Policy            policy       = new Policy();
    private int               idCounter    = 1;

    // ── Registration ───────────────────────────────────────────────

    public void registerStudent(Student student) {
        students.add(student);
        System.out.println("[System] Student registered: " + student.getName());
    }

    public void registerStaff(Staff staff) {
        staffList.add(staff);
        System.out.println("[System] Staff registered: " + staff.getName());
    }

    public void addRoom(Room room)           { rooms.add(room); }
    public void addEquipment(Equipment item) { equipment.add(item); }

    // ── Core booking ───────────────────────────────────────────────

    /**
     * Books a room for a student. Runs all system-level checks before confirming.
     *
     * Checks performed:
     *  1. Student is logged in
     *  2. Student is not suspended
     *  3. No overlapping reservation exists for the same room
     *  4. Student has not exceeded the daily reservation limit
     *  5. Reservation passes policy validation (capacity, duration, future time)
     */
    public Reservation bookRoom(Student student, Room room,
                                 TimeSlot timeSlot, int attendeeCount) {
        // 1. Login
        if (!student.isLoggedIn())
            throw new IllegalStateException(student.getName() + " must be logged in.");

        // 2. Suspension
        if (student.isSuspended())
            throw new IllegalStateException(student.getName() + " is suspended.");

        // 3. Double-booking
        checkDoubleBooking(room, timeSlot);

        // 4. Daily limit
        checkDailyLimit(student, timeSlot);

        // 5. Build and validate
        String id = "RES-" + String.format("%04d", idCounter++);
        Reservation res = new Reservation(id, student, room, timeSlot, attendeeCount, policy);

        student.createReservation(res); // sets CONFIRMED inside

        room.setAvailable(false);
        reservations.add(res);

        // Notification
        Notification notif = new Notification(student,
                "Reservation " + id + " for room " + room.getRoomID() +
                " at " + timeSlot.getStartTime() + " is CONFIRMED.");
        notif.sendEmail();
        res.setNotification(notif);

        return res;
    }

    /**
     * Adds equipment to an existing confirmed reservation.
     */
    public void addEquipmentToReservation(Reservation reservation,
                                           Equipment item, int quantity) {
        if (reservation.getStatus() != ReservationStatus.CONFIRMED &&
            reservation.getStatus() != ReservationStatus.PENDING)
            throw new IllegalStateException("Can only add equipment to PENDING/CONFIRMED reservations.");

        if (item.getQuantityAvailable() < quantity)
            throw new IllegalStateException("Not enough equipment. Requested: " + quantity +
                    ", Available: " + item.getQuantityAvailable());

        EquipmentBooking eb = new EquipmentBooking(item, quantity);
        reservation.addEquipmentBooking(eb);
        System.out.println("[System] Added " + item.getType() + " x" + quantity +
                " to reservation " + reservation.getReservationID());
    }

    /**
     * Cancels a reservation and releases its equipment.
     */
    public void cancelReservation(Student student, String reservationID) {
        student.cancelReservation(reservationID);
        Reservation res = findReservation(reservationID);
        res.releaseAllEquipment();

        Notification notif = new Notification(student,
                "Reservation " + reservationID + " has been CANCELLED.");
        notif.sendEmail();
    }

    /**
     * Marks a student as no-show, issues a strike, and frees the room.
     */
    public void markNoShow(Student student, String reservationID) {
        student.markNoShow(reservationID);
        Reservation res = findReservation(reservationID);
        res.releaseAllEquipment();

        Notification notif = new Notification(student,
                "You were marked NO_SHOW for reservation " + reservationID + ". Strike issued.");
        notif.sendEmail();
    }

    /**
     * Returns rooms with no confirmed/pending booking that overlaps the requested slot.
     */
    public List<Room> searchAvailableRooms(TimeSlot requestedSlot) {
        List<Room> result = new ArrayList<>();
        for (Room r : rooms) {
            boolean conflict = false;
            for (Reservation res : reservations) {
                if (res.getRoom() == r &&
                    (res.getStatus() == ReservationStatus.CONFIRMED ||
                     res.getStatus() == ReservationStatus.PENDING) &&
                    res.getTimeSlot().checkOverlap(requestedSlot)) {
                    conflict = true;
                    break;
                }
            }
            if (!conflict) result.add(r);
        }
        return result;
    }

    // ── Internal validation ────────────────────────────────────────

    private void checkDoubleBooking(Room room, TimeSlot timeSlot) {
        for (Reservation res : reservations) {
            if (res.getRoom() == room &&
                (res.getStatus() == ReservationStatus.CONFIRMED ||
                 res.getStatus() == ReservationStatus.PENDING) &&
                res.getTimeSlot().checkOverlap(timeSlot)) {
                throw new IllegalStateException(
                        "Room " + room.getRoomID() + " is already booked for that time slot.");
            }
        }
    }

    private void checkDailyLimit(Student student, TimeSlot timeSlot) {
        LocalDate date = timeSlot.getStartTime().toLocalDate();
        int count = 0;
        for (Reservation res : reservations) {
            if (res.getStudent() == student &&
                (res.getStatus() == ReservationStatus.CONFIRMED ||
                 res.getStatus() == ReservationStatus.PENDING) &&
                res.getTimeSlot().getStartTime().toLocalDate().equals(date)) {
                count++;
            }
        }
        if (count >= policy.getMaxReservationsPerDay())
            throw new IllegalStateException(student.getName() +
                    " has reached the daily reservation limit of " +
                    policy.getMaxReservationsPerDay() + ".");
    }

    public Reservation findReservation(String id) {
        for (Reservation r : reservations) {
            if (r.getReservationID().equals(id)) return r;
        }
        throw new IllegalArgumentException("Reservation not found: " + id);
    }

    // ── Getters ────────────────────────────────────────────────────
    public List<Room>        getRooms()        { return rooms; }
    public List<Equipment>   getEquipment()    { return equipment; }
    public List<Student>     getStudents()     { return students; }
    public List<Staff>       getStaffList()    { return staffList; }
    public List<Reservation> getReservations() { return reservations; }
    public Policy            getPolicy()       { return policy; }
}
