# CSEBS – Concordia Study Space & Equipment Booking System
### COEN 6312 – Milestone 3 | Winter 2026

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
│   ├── Demo.java
│   └── Main.java
│
└── tests/                ← Unit tests (no external libraries needed)
    ├── TestRunner.java
    ├── UserTests.java
    ├── RoomEquipmentTests.java
    ├── TimeSlotTests.java
    ├── ReservationTests.java
    ├── BookingSystemTests.java
    └── OCLConstraintTests.java   ← Milestone 3: OCL constraint tests
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

### Step 2 – Run the interactive app

```bash
java -cp out Main
```

Launches the interactive CLI where you can log in, book rooms, add equipment, check in, and more.

---

### Step 3 – Run the scripted demo

```bash
java -cp out Demo
```

Runs all scenarios automatically showing interactions between every class.

---

### Step 4 – Run the unit tests

```bash
java -cp out TestRunner
```

Runs all Milestone 2 tests plus the Milestone 3 OCL constraint tests.

---

## OCL Constraints Implemented (Milestone 3)

13 constraints are implemented across the codebase, marked with comments of the form `// OCL Constraint N`:

| Constraint | Description | Enforced In |
|---|---|---|
| 2  | Attendee count within room capacity | Reservation.validatePolicy() |
| 5  | Reservation duration within policy limit | Reservation.validatePolicy() |
| 10 | Equipment must be available | BookingSystem, Reservation |
| 13 | No overlapping reservations for same room | BookingSystem.checkDoubleBooking() |
| 14 | Daily reservation limit per student | BookingSystem.checkDailyLimit() |
| 15 | Suspended student cannot reserve | Student.createReservation() |
| 16 | Confirmed reservation must satisfy all rules | Reservation.validatePolicy() |
| 17 | No-show must generate strike and release room | Student.markNoShow() |
| 18 | Late cancellation results in a strike | Student.cancelReservation() |
| 19 | Cancelled or no-show reservation releases room | Student.cancelReservation(), markNoShow() |
| 21 | Equipment cannot be overbooked | BookingSystem, Equipment.reserve() |
| 22 | Equipment status consistency | Equipment.updateStatus() |
| 25 | Only pending reservations can be approved | Staff.approveReservation() |

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
