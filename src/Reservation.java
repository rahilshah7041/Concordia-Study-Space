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
        // STATE TRANSITION (Reservation): [*] --> PENDING
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
     *
     * OCL Constraint 4  - Future Reservation Only:
     *   context TimeSlot inv FutureReservation: self.startTime > currentTime
     * OCL Constraint 5  - Reservation Duration Within Policy Limit:
     *   context Reservation inv MaxDuration: self.calculateDuration() <= policy.maxDurationMinutes
     * OCL Constraint 2  - Attendee Count Within Room Capacity:
     *   context Reservation inv WithinCapacity: self.attendeeCount <= self.room.capacity
     * OCL Constraint 16 - Confirmed Reservation Must Satisfy All Rules:
     *   context Reservation inv ValidBeforeConfirm:
     *     self.status = CONFIRMED implies self.attendeeCount > 0 and
     *     self.attendeeCount <= self.room.capacity
     */
    public boolean validatePolicy() {
        // OCL Constraint 4 - Future Reservation Only
        // OCL Constraint 5 - Reservation Duration Within Policy Limit
        if (!timeSlot.isValidDuration()) {
            System.out.println("[Reservation:" + reservationID +
                    "] INVALID - time slot is in the past or exceeds system max.");
            return false;
        }

        // OCL Constraint 5 - Reservation Duration Within Policy Limit
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

        // OCL Constraint 2  - Attendee Count Within Room Capacity
        // OCL Constraint 16 - Confirmed Reservation Must Satisfy All Rules
        if (!room.checkCapacity(attendeeCount)) {
            System.out.println("[Reservation:" + reservationID +
                    "] INVALID - " + attendeeCount + " attendees exceed room capacity of " +
                    room.getCapacity() + ".");
            return false;
        }

        // OCL Constraint 16 - attendeeCount must be > 0 before confirmation
        if (attendeeCount <= 0) {
            System.out.println("[Reservation:" + reservationID +
                    "] INVALID - attendee count must be greater than 0.");
            return false;
        }

        return true;
    }

    /**
     * Attaches an EquipmentBooking to this reservation.
     *
     * OCL Constraint 10 - Equipment Must Be Available:
     *   context Reservation inv EquipmentAvailable:
     *     self.equipment->forAll(e | e.status = 'available' and e.quantityAvailable > 0)
     * OCL Constraint 20 - Equipment Cannot Be Added to Inactive Reservations:
     *   context Reservation inv EquipmentOnlyActive:
     *     self.status = CONFIRMED implies self.equipment->forAll(e | e.status = 'available')
     */
    public void addEquipmentBooking(EquipmentBooking eb) {
        if (eb == null) throw new IllegalArgumentException("EquipmentBooking cannot be null.");

        // OCL Constraint 20 - equipment can only be added to active (PENDING/CONFIRMED) reservations
        if (status == ReservationStatus.CANCELLED ||
            status == ReservationStatus.COMPLETED ||
            status == ReservationStatus.NO_SHOW) {
            throw new IllegalStateException(
                "OCL Constraint 20: Equipment cannot be added to an inactive reservation (status: " + status + ").");
        }

        // OCL Constraint 10 - equipment must be available
        if (!eb.getEquipment().getStatus().equals(Equipment.STATUS_AVAILABLE)) {
            throw new IllegalStateException(
                "OCL Constraint 10: Equipment " + eb.getEquipment().getItemID() +
                " is not available (status: " + eb.getEquipment().getStatus() + ").");
        }

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

    public void setStatus(ReservationStatus status) {
        // STATE TRANSITION (Reservation): current --> status
        // Valid transitions:
        //   PENDING    --> CONFIRMED  : createReservation() / approveReservation()
        //   PENDING    --> CANCELLED  : overrideReservation()
        //   CONFIRMED  --> COMPLETED  : checkIn()
        //   CONFIRMED  --> CANCELLED  : cancelReservation() / overrideReservation()
        //   CONFIRMED  --> NO_SHOW    : markNoShow()
        this.status = status;
    }
    public void setNotification(Notification n)          { this.notification = n; }

    @Override
    public String toString() {
        return "Reservation{ id='" + reservationID + "', student='" + student.getName() +
               "', room='" + room.getRoomID() + "', slot=" + timeSlot +
               ", attendees=" + attendeeCount + ", status=" + status + " }";
    }
}
