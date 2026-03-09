import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Represents a time window for a reservation.
 *
 * UML: TimeSlot
 *   - startTime    : DateTime
 *   - endTime      : DateTime
 *   - isOverlapping: Boolean
 *   + checkOverlap(other: TimeSlot) : Boolean
 *   + isValidDuration()             : Boolean
 */
public class TimeSlot {

    public static final long MAX_DURATION_MINUTES = 240; // 4 hours

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public TimeSlot(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null)
            throw new IllegalArgumentException("Start and end times cannot be null.");
        if (!endTime.isAfter(startTime))
            throw new IllegalArgumentException("End time must be after start time.");
        this.startTime = startTime;
        this.endTime   = endTime;
    }

    /**
     * Returns true if this slot overlaps with the other slot.
     */
    public boolean checkOverlap(TimeSlot other) {
        return this.startTime.isBefore(other.endTime) &&
               other.startTime.isBefore(this.endTime);
    }

    /**
     * Returns true if the slot is in the future and within the max allowed duration.
     */
    public boolean isValidDuration() {
        long minutes = getDurationMinutes();
        return startTime.isAfter(LocalDateTime.now()) &&
               minutes > 0 &&
               minutes <= MAX_DURATION_MINUTES;
    }

    /** Minutes remaining until the start of this slot (negative if already started). */
    public long minutesUntilStart() {
        return ChronoUnit.MINUTES.between(LocalDateTime.now(), startTime);
    }

    /** Duration of this slot in minutes. */
    public long getDurationMinutes() {
        return ChronoUnit.MINUTES.between(startTime, endTime);
    }

    // ── Getters ────────────────────────────────────────────────────
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime()   { return endTime; }

    @Override
    public String toString() {
        return "TimeSlot{ " + startTime + " → " + endTime +
               " (" + getDurationMinutes() + " min) }";
    }
}
