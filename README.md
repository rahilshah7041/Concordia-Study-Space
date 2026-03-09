# CSEBS – Concordia Study Space & Equipment Booking System
### COEN 6312 – Milestone 2 | Winter 2026

---

## Project Structure

```
CSEBS/
├── src/                  ← All application source files (no packages)
│   ├── User.java
│   ├── Student.java
│   ├── Staff.java
│   ├── Admin.java
│   ├── Room.java
│   ├── Equipment.java
│   ├── EquipmentBooking.java
│   ├── TimeSlot.java
│   ├── Reservation.java
│   ├── ReservationStatus.java
│   ├── Notification.java
│   ├── Policy.java
│   ├── BookingSystem.java
│   └── Main.java
│
└── tests/                ← Unit tests (no external libraries needed)
    ├── TestRunner.java
    ├── UserTests.java
    ├── RoomEquipmentTests.java
    ├── TimeSlotTests.java
    ├── ReservationTests.java
    └── BookingSystemTests.java
```

---

## Requirements

- Java 11 or higher (Java 17 / 21 also works)
- No Maven, no external libraries

---

## How to Run

Open a terminal in the `CSEBS/` folder.

### Step 1 – Compile everything

**Windows (PowerShell):**
```powershell
javac -d out src/*.java tests/*.java
```

**Mac / Linux:**
```bash
javac -d out src/*.java tests/*.java
```

> This creates an `out/` folder with all compiled `.class` files.

---

### Step 2 – Run the demo

```bash
java -cp out Main
```

This runs all 18 demo scenarios showing interactions between every class.

---

### Step 3 – Run the unit tests

```bash
java -cp out TestRunner
```

Expected output ends with:
```
Results: 57 passed, 0 failed.
```

---

## Class Hierarchy (matches UML diagram)

```
User  (abstract)
 ├── Student        strikeCount, major
 └── Staff          staffID, department
      └── Admin     configurePolicies(), generateUsageReport()
```

## Key Associations

| Association | Type | Description |
|---|---|---|
| Student → Reservation | 1 to 0..* | A student owns their reservations |
| Reservation → Room | Many to 1 | Each reservation books one room |
| Reservation → TimeSlot | 1 to 1..* | Each reservation has a time window |
| Reservation → EquipmentBooking | 1 to 0..* | Optional equipment attached |
| EquipmentBooking → Equipment | Many to 1 | Links to the actual equipment item |
| Reservation → ReservationStatus | 1 to 1 | Current status of the booking |
| Reservation → Notification | 1 to 1 | Confirmation sent on booking |
| Admin → Policy | 1 to 1 | Admin configures global policy |
