/**
 * Represents a physical study room that can be booked by students.
 *
 * UML: Room
 *   - roomID      : String
 *   - building    : String
 *   - capacity    : int
 *   - isAvailable : Boolean
 *   + checkCapacity() : Boolean
 */
public class Room {

    private String  roomID;
    private String  building;
    private int     capacity;
    private boolean isAvailable;

    public Room(String roomID, String building, int capacity) {
        if (roomID == null || roomID.isEmpty())
            throw new IllegalArgumentException("roomID cannot be empty.");
        if (building == null || building.isEmpty())
            throw new IllegalArgumentException("building cannot be empty.");
        if (capacity <= 0)
            throw new IllegalArgumentException("capacity must be greater than 0.");
        this.roomID      = roomID;
        this.building    = building;
        this.capacity    = capacity;
        this.isAvailable = true;
    }

    /**
     * Returns true if the number of attendees fits within this room's capacity.
     */
    public boolean checkCapacity(int attendeeCount) {
        return attendeeCount > 0 && attendeeCount <= capacity;
    }

    // ── Getters / Setters ─────────────────────────────────────────
    public String  getRoomID()              { return roomID; }
    public String  getBuilding()            { return building; }
    public int     getCapacity()            { return capacity; }
    public boolean isAvailable()            { return isAvailable; }
    public void    setAvailable(boolean v)  { this.isAvailable = v; }

    @Override
    public String toString() {
        return "Room{ roomID='" + roomID + "', building='" + building +
               "', capacity=" + capacity + ", available=" + isAvailable + " }";
    }
}
