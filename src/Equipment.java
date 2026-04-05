/**
 * Represents a shared equipment item (projector, HDMI cable, monitor, etc.).
 *
 * UML: Equipment
 *   - itemID            : String
 *   - type              : String
 *   - status            : String
 *   - quantityAvailable : int
 *   + updateStatus() : void
 */
public class Equipment {

    public static final String STATUS_AVAILABLE   = "available";
    public static final String STATUS_MAINTENANCE = "maintenance";
    public static final String STATUS_UNAVAILABLE = "unavailable";

    private String itemID;
    private String type;
    private String status;
    private int    quantityAvailable;
    private int    totalQuantity;

    public Equipment(String itemID, String type, int totalQuantity) {
        if (itemID == null || itemID.isEmpty())
            throw new IllegalArgumentException("itemID cannot be empty.");
        if (type == null || type.isEmpty())
            throw new IllegalArgumentException("type cannot be empty.");
        if (totalQuantity <= 0)
            throw new IllegalArgumentException("totalQuantity must be greater than 0.");
        this.itemID            = itemID;
        this.type              = type;
        this.totalQuantity     = totalQuantity;
        this.quantityAvailable = totalQuantity;
        // STATE TRANSITION (Equipment): [*] --> AVAILABLE
        this.status            = STATUS_AVAILABLE;
    }

    /**
     * Updates the status of this item. Setting maintenance/unavailable zeroes quantity.
     *
     * OCL Constraint 22 - Equipment Status Consistency:
     *   context Equipment inv StatusConsistency:
     *     self.status = 'maintenance' implies self.quantityAvailable = 0
     *
     * STATE TRANSITIONS (Equipment):
     *   AVAILABLE --> MAINTENANCE : updateStatus("maintenance")
     *   AVAILABLE --> UNAVAILABLE : updateStatus("unavailable")
     *   MAINTENANCE --> AVAILABLE : updateStatus("available")
     *   UNAVAILABLE --> AVAILABLE : updateStatus("available")
     *   MAINTENANCE --> UNAVAILABLE: updateStatus("unavailable")
     *   UNAVAILABLE --> MAINTENANCE: updateStatus("maintenance")
     */
    public void updateStatus(String newStatus) {
        this.status = newStatus;
        // OCL Constraint 22 - maintenance status must zero the available quantity
        if (!newStatus.equals(STATUS_AVAILABLE)) {
            this.quantityAvailable = 0;
        }
        System.out.println("[Equipment:" + itemID + "] Status → " + newStatus);
    }

    /**
     * Reserves a quantity of units. Returns false if not enough available.
     * STATE TRANSITION (Equipment): self-transition on AVAILABLE state
     *
     * OCL Constraint 21 - Equipment Cannot Be Overbooked:
     *   context Equipment inv NotOverbooked:
     *     self.quantityAvailable >= 0 and self.quantityAvailable <= self.totalQuantity
     */
    public boolean reserve(int qty) {
        if (qty <= 0) throw new IllegalArgumentException("Quantity must be > 0.");
        if (!status.equals(STATUS_AVAILABLE)) return false;
        // OCL Constraint 21 - cannot reserve more than available
        if (qty > quantityAvailable) return false;
        quantityAvailable -= qty;
        return true;
    }

    /**
     * Returns units back to the available pool.
     * STATE TRANSITION (Equipment): self-transition on AVAILABLE state
     */
    public void release(int qty) {
        if (qty <= 0) throw new IllegalArgumentException("Quantity must be > 0.");
        quantityAvailable = Math.min(totalQuantity, quantityAvailable + qty);
    }

    // ── Getters / Setters ─────────────────────────────────────────
    public String getItemID()            { return itemID; }
    public String getType()              { return type; }
    public String getStatus()            { return status; }
    public int    getQuantityAvailable() { return quantityAvailable; }
    public int    getTotalQuantity()     { return totalQuantity; }

    public void setStatus(String status)         { this.status = status; }
    public void setQuantityAvailable(int qty)    {
        if (qty < 0 || qty > totalQuantity)
            throw new IllegalArgumentException("Invalid quantity: " + qty);
        this.quantityAvailable = qty;
    }

    @Override
    public String toString() {
        return "Equipment{ itemID='" + itemID + "', type='" + type +
               "', status='" + status + "', available=" + quantityAvailable + "/" + totalQuantity + " }";
    }
}
