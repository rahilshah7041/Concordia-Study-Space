import java.util.ArrayList;
import java.util.List;

/**
 * Represents a student user who can search rooms and make reservations.
 *
 * UML: Student  (inherits User)
 *   - strikeCount : int
 *   - major       : String
 *   + searchRooms()       : List<Room>
 *   + createReservation() : void
 *   + checkIn()           : void
 *   + cancelReservation() : void
 */
public class Student extends User {

    public static final int MAX_STRIKES = 3;

    private int    strikeCount;
    private String major;

    private List<Reservation> reservations = new ArrayList<>();

    public Student(String userID, String name, String email, String password, String major) {
        super(userID, name, email, password);
        if (major == null || major.isEmpty())
            throw new IllegalArgumentException("Major cannot be empty.");
        this.major       = major;
        this.strikeCount = 0;
    }

    // ── Domain methods ─────────────────────────────────────────────

    /**
     * Returns all rooms that are currently marked as available.
     */
    public List<Room> searchRooms(List<Room> allRooms) {
        requireLogin();
        List<Room> available = new ArrayList<>();
        for (Room r : allRooms) {
            if (r.isAvailable()) available.add(r);
        }
        System.out.println("[" + getName() + "] Found " + available.size() + " available room(s).");
        return available;
    }

    /**
     * Confirms and stores a reservation for this student.
     * Checks suspension and policy before confirming.
     */
    public void createReservation(Reservation reservation) {
        requireLogin();
        if (isSuspended())
            throw new IllegalStateException(getName() + " is suspended and cannot make reservations.");
        if (!reservation.validatePolicy())
            throw new IllegalStateException("Reservation failed policy validation.");

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservations.add(reservation);
        System.out.println("[" + getName() + "] Reservation " +
                reservation.getReservationID() + " CONFIRMED.");
    }

    /**
     * Checks the student into a reservation, marking it COMPLETED.
     */
    public void checkIn(String reservationID) {
        requireLogin();
        Reservation res = findReservation(reservationID);
        if (res.getStatus() != ReservationStatus.CONFIRMED)
            throw new IllegalStateException("Can only check in to a CONFIRMED reservation.");
        res.setStatus(ReservationStatus.COMPLETED);
        System.out.println("[" + getName() + "] Checked in to reservation " + reservationID + ".");
    }

    /**
     * Cancels a reservation. Issues a strike if cancelled within 30 min of start.
     */
    public void cancelReservation(String reservationID) {
        requireLogin();
        Reservation res = findReservation(reservationID);

        if (res.getStatus() == ReservationStatus.CANCELLED)
            throw new IllegalStateException("Reservation is already cancelled.");
        if (res.getStatus() == ReservationStatus.COMPLETED)
            throw new IllegalStateException("Cannot cancel a completed reservation.");

        // Late cancellation strike (within 30 minutes of start)
        if (res.getTimeSlot().minutesUntilStart() < 30 &&
            res.getTimeSlot().minutesUntilStart() >= 0) {
            addStrike("Late cancellation for reservation " + reservationID);
        }

        res.setStatus(ReservationStatus.CANCELLED);
        res.getRoom().setAvailable(true);
        System.out.println("[" + getName() + "] Reservation " + reservationID + " CANCELLED.");
    }

    /** Adds a strike to the student. Prints suspension notice if threshold reached. */
    public void addStrike(String reason) {
        strikeCount++;
        System.out.println("[" + getName() + "] Strike #" + strikeCount + " - Reason: " + reason);
        if (isSuspended())
            System.out.println("[" + getName() + "] *** SUSPENDED - max strikes reached. ***");
    }

    /** Marks reservation as NO_SHOW and adds a strike. */
    public void markNoShow(String reservationID) {
        Reservation res = findReservation(reservationID);
        res.setStatus(ReservationStatus.NO_SHOW);
        res.getRoom().setAvailable(true);
        addStrike("No-show for reservation " + reservationID);
    }

    // ── Helpers ────────────────────────────────────────────────────

    private void requireLogin() {
        if (!isLoggedIn())
            throw new IllegalStateException(getName() + " must be logged in.");
    }

    public Reservation findReservation(String id) {
        for (Reservation r : reservations) {
            if (r.getReservationID().equals(id)) return r;
        }
        throw new IllegalArgumentException("Reservation not found: " + id);
    }

    public boolean isSuspended() { return strikeCount >= MAX_STRIKES; }

    // ── Getters ────────────────────────────────────────────────────
    public int              getStrikeCount()  { return strikeCount; }
    public String           getMajor()        { return major; }
    public List<Reservation> getReservations(){ return reservations; }

    @Override
    public String toString() {
        return "Student{ userID='" + getUserID() + "', name='" + getName() +
               "', major='" + major + "', strikes=" + strikeCount + " }";
    }
}
