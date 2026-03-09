/**
 * Abstract base class for all users in the CSEBS system.
 *
 * UML: User
 *   - userID  : String
 *   - name    : String
 *   - email   : String
 *   + login() : Boolean
 *   + logout(): void
 */
public abstract class User {

    private String  userID;
    private String  name;
    private String  email;
    private String  password;
    private boolean loggedIn;

    public User(String userID, String name, String email, String password) {
        if (userID == null || userID.isEmpty())
            throw new IllegalArgumentException("userID cannot be empty.");
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("name cannot be empty.");
        if (email == null || !email.contains("@"))
            throw new IllegalArgumentException("Invalid email address.");
        if (password == null || password.isEmpty())
            throw new IllegalArgumentException("Password cannot be empty.");

        this.userID   = userID;
        this.name     = name;
        this.email    = email;
        this.password = password;
        this.loggedIn = false;
    }

    /**
     * Logs the user in if the supplied password matches.
     * @return true on success, false on wrong password
     */
    public boolean login(String password) {
        if (loggedIn) {
            System.out.println("[" + name + "] Already logged in.");
            return true;
        }
        if (this.password.equals(password)) {
            loggedIn = true;
            System.out.println("[" + name + "] Logged in successfully.");
            return true;
        }
        System.out.println("[" + name + "] Login failed - incorrect password.");
        return false;
    }

    /** Logs the user out. */
    public void logout() {
        if (!loggedIn) {
            System.out.println("[" + name + "] Not currently logged in.");
            return;
        }
        loggedIn = false;
        System.out.println("[" + name + "] Logged out.");
    }

    // ── Getters ───────────────────────────────────────────────────
    public String  getUserID()   { return userID; }
    public String  getName()     { return name; }
    public String  getEmail()    { return email; }
    public boolean isLoggedIn()  { return loggedIn; }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
               "{ userID='" + userID + "', name='" + name + "', email='" + email + "' }";
    }
}
