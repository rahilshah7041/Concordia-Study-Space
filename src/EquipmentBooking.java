/**
 * Represents the booking of a specific quantity of equipment within a reservation.
 *
 * Association: Reservation (1) ──◆ EquipmentBooking (0..*) ──> Equipment (1)
 */
public class EquipmentBooking {

    private Equipment equipment;
    private int       quantity;
    private boolean   released;

    public EquipmentBooking(Equipment equipment, int quantity) {
        if (equipment == null)
            throw new IllegalArgumentException("Equipment cannot be null.");
        if (quantity <= 0)
            throw new IllegalArgumentException("Quantity must be greater than 0.");
        if (!equipment.reserve(quantity))
            throw new IllegalStateException("Cannot reserve " + quantity +
                    " unit(s) of " + equipment.getItemID() +
                    ". Available: " + equipment.getQuantityAvailable());
        this.equipment = equipment;
        this.quantity  = quantity;
        this.released  = false;
    }

    /** Returns the reserved quantity back to the equipment pool. */
    public void release() {
        if (!released) {
            equipment.release(quantity);
            released = true;
        }
    }

    // ── Getters ────────────────────────────────────────────────────
    public Equipment getEquipment() { return equipment; }
    public int       getQuantity()  { return quantity; }
    public boolean   isReleased()   { return released; }

    @Override
    public String toString() {
        return "EquipmentBooking{ item='" + equipment.getItemID() +
               "', qty=" + quantity + " }";
    }
}
