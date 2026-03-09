import java.util.List;

/**
 * Represents a staff member who manages rooms, equipment and reservations.
 *
 * UML: Staff  (inherits User)
 *   - staffID    : String
 *   - department : String
 *   + addRoom()             : void
 *   + updateEquipment()     : void
 *   + approveReservation()  : void
 */
public class Staff extends User {

    private String staffID;
    private String department;

    public Staff(String userID, String name, String email, String password,
                 String staffID, String department) {
        super(userID, name, email, password);
        if (staffID == null || staffID.isEmpty())
            throw new IllegalArgumentException("staffID cannot be empty.");
        if (department == null || department.isEmpty())
            throw new IllegalArgumentException("department cannot be empty.");
        this.staffID    = staffID;
        this.department = department;
    }

    // ── Domain methods ─────────────────────────────────────────────

    /**
     * Adds a new room to the system room catalogue.
     */
    public void addRoom(List<Room> rooms, Room newRoom) {
        requireLogin();
        for (Room r : rooms) {
            if (r.getRoomID().equals(newRoom.getRoomID()))
                throw new IllegalArgumentException("Room " + newRoom.getRoomID() + " already exists.");
        }
        rooms.add(newRoom);
        System.out.println("[Staff:" + getName() + "] Room " + newRoom.getRoomID() + " added.");
    }

    /**
     * Updates the status and available quantity of an equipment item.
     */
    public void updateEquipment(Equipment equipment, String newStatus, int newQty) {
        requireLogin();
        if (newQty < 0)
            throw new IllegalArgumentException("Quantity cannot be negative.");
        equipment.setStatus(newStatus);
        equipment.setQuantityAvailable(newQty);
        System.out.println("[Staff:" + getName() + "] Equipment " + equipment.getItemID() +
                " updated → status=" + newStatus + ", qty=" + newQty);
    }

    /**
     * Approves a PENDING reservation.
     */
    public void approveReservation(Reservation reservation) {
        requireLogin();
        if (reservation.getStatus() != ReservationStatus.PENDING)
            throw new IllegalStateException("Only PENDING reservations can be approved.");
        reservation.setStatus(ReservationStatus.CONFIRMED);
        System.out.println("[Staff:" + getName() + "] Reservation " +
                reservation.getReservationID() + " APPROVED.");
    }

    /**
     * Overrides (cancels) any reservation.
     */
    public void overrideReservation(Reservation reservation, String reason) {
        requireLogin();
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.getRoom().setAvailable(true);
        System.out.println("[Staff:" + getName() + "] Reservation " +
                reservation.getReservationID() + " OVERRIDDEN. Reason: " + reason);
    }

    // ── Helpers ────────────────────────────────────────────────────

    private void requireLogin() {
        if (!isLoggedIn())
            throw new IllegalStateException(getName() + " must be logged in.");
    }

    // ── Getters ────────────────────────────────────────────────────
    public String getStaffID()    { return staffID; }
    public String getDepartment() { return department; }

    @Override
    public String toString() {
        return "Staff{ userID='" + getUserID() + "', name='" + getName() +
               "', dept='" + department + "' }";
    }
}
