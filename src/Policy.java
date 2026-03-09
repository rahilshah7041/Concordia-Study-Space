/**
 * Holds the configurable booking policies for the CSEBS system.
 * Managed by Admin via configurePolicies().
 */
public class Policy {

    private int maxDurationMinutes;    // max booking length
    private int maxReservationsPerDay; // per-student daily limit
    private int strikeThreshold;       // strikes before suspension

    public Policy() {
        this.maxDurationMinutes    = 240; // 4 hours default
        this.maxReservationsPerDay = 3;
        this.strikeThreshold       = 3;
    }

    // ── Getters / Setters ─────────────────────────────────────────
    public int  getMaxDurationMinutes()          { return maxDurationMinutes; }
    public int  getMaxReservationsPerDay()       { return maxReservationsPerDay; }
    public int  getStrikeThreshold()             { return strikeThreshold; }

    public void setMaxDurationMinutes(int v)     { this.maxDurationMinutes    = v; }
    public void setMaxReservationsPerDay(int v)  { this.maxReservationsPerDay = v; }
    public void setStrikeThreshold(int v)        { this.strikeThreshold       = v; }

    @Override
    public String toString() {
        return "Policy{ maxDuration=" + maxDurationMinutes + "min, maxPerDay=" +
               maxReservationsPerDay + ", strikeThreshold=" + strikeThreshold + " }";
    }
}
