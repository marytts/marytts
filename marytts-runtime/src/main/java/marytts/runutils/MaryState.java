package marytts.runutils;

public enum MaryState {
    STARTING       ("STARTING"),
    RUNNING        ("RUNNING"),
    SHUTTING_DOWN  ("SHUTTING_DOWN"),
    OFF            ("OFF")
    ;

    private final String state_label;

    MaryState(String state_label) {
        this.state_label = state_label;
    }

    @Override
    public String toString() {
        return state_label;
    }
}
