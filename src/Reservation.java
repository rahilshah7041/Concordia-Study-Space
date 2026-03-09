import java.util.ArrayList;
import java.util.List;

/**
 * Represents a booking made by a Student for a Room over a TimeSlot,
 * optionally including Equipment items.
 *
 * UML: Reservation
 *   - reservationID : String
 *   - attendeeCount : int
 *   + calculateDuration() : long
 *   + validatePolicy()    : Boolean
 *
 * Associations:
 *   Reservation --> Room             (many-to-1)
 *   Reservation --> TimeSlot         (1..*)
 *   Reservation --> ReservationStatus(1)
 *   Reservation --> EquipmentBooking (0..*)
 *   Reservation --> Notification     (1)
 */
public class Reservation {

    private String            reservationID;
    private int               attendeeCount;
    private ReservationStatus status;
    private Room              room;
    private TimeSlot          timeSlot;
    private Student           student;
    private Policy            policy;
    private Notification      notification;

    private List<EquipmentBooking> equipmentBookings = new ArrayList<>();

    public Reservation(String reservationID, Student student, Room room,
                       TimeSlot timeSlot, int attendeeCount, Policy policy) {
        if (reservationID == null || reservationID.isEmpty())
            throw new IllegalArgumentException("reservationID cannot be empty.");
        if (student  == null) throw new IllegalArgumentException("Student cannot be null.");
        if (room     == null) throw new IllegalArgumentException("Room cannot be null.");
        if (timeSlot == null) throw new IllegalArgumentException("TimeSlot cannot be null.");
        if (policy   == null) throw new IllegalArgumentException("Policy cannot be null.");
        if (attendeeCount <= 0)
            throw new IllegalArgumentException("attendeeCount must be greater than 0.");

        this.reservationID = reservationID;
        this.student       = student;
        this.room          = room;
        this.timeSlot      = timeSlot;
        this.attendeeCount = attendeeCount;
        this.policy        = policy;
        this.status        = ReservationStatus.PENDING;
    }

    // ── Domain methods ─────────────────────────────────────────────

    /** Returns the duration of this reservation in minutes. */
    public long calculateDuration() {
        return timeSlot.getDurationMinutes();
    }

    /**
     * Validates the reservation against room and policy constraints.
     * Checks: future time slot, max duration, room availability, capacity.
     */
    public boolean validatePolicy() {
        if (!timeSlot.isValidDuration()) {
            System.out.println("[Reservation:" + reservationID +
                    "] INVALID - time slot is in the past or exceeds system max.");
            return false;
        }
        if (calculateDuration() > policy.getMaxDurationMinutes()) {
            System.out.println("[Reservation:" + reservationID +
                    "] INVALID - duration exceeds policy maximum of " +
                    policy.getMaxDurationMinutes() + " min.");
            return false;
        }
        if (!room.isAvailable()) {
            System.out.println("[Reservation:" + reservationID +
                    "] INVALID - room " + room.getRoomID() + " is not available.");
            return false;
        }
        if (!room.checkCapacity(attendeeCount)) {
            System.out.println("[Reservation:" + reservationID +
                    "] INVALID - " + attendeeCount + " attendees exceed room capacity of " +
                    room.getCapacity() + ".");
            return false;
        }
        return true;
    }

    /** Attaches an EquipmentBooking to this reservation. */
    public void addEquipmentBooking(EquipmentBooking eb) {
        if (eb == null) throw new IllegalArgumentException("EquipmentBooking cannot be null.");
        equipmentBookings.add(eb);
    }

    /** Releases all equipment associated with this reservation. */
    public void releaseAllEquipment() {
        for (EquipmentBooking eb : equipmentBookings) eb.release();
    }

    // ── Getters / Setters ─────────────────────────────────────────
    public String              getReservationID()    { return reservationID; }
    public int                 getAttendeeCount()    { return attendeeCount; }
    public ReservationStatus   getStatus()           { return status; }
    public Room                getRoom()             { return room; }
    public TimeSlot            getTimeSlot()         { return timeSlot; }
    public Student             getStudent()          { return student; }
    public Policy              getPolicy()           { return policy; }
    public Notification        getNotification()     { return notification; }
    public List<EquipmentBooking> getEquipmentBookings() { return equipmentBookings; }

    public void setStatus(ReservationStatus status)      { this.status = status; }
    public void setNotification(Notification n)          { this.notification = n; }

    @Override
    public String toString() {
        return "Reservation{ id='" + reservationID + "', student='" + student.getName() +
               "', room='" + room.getRoomID() + "', slot=" + timeSlot +
               ", attendees=" + attendeeCount + ", status=" + status + " }";
    }
}
