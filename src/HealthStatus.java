public enum HealthStatus {
    
    HEALTHY(0, "Healthy", "Operating normally"),
    DAMAGED(1, "Damaged", "Experiencing partial failure"),
    DEAD(2, "Unresponsive", "No heartbeat detected"),
    RECOVERING(3, "Recovering", "Service is restarting or reconnecting");

    private final int level;
    private final String name;
    private final String description;

    private HealthStatus(int level, String name, String description) {
        this.level = level;
        this.name = name;
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHealthy() {
        return this == HEALTHY || this == RECOVERING;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", name, description);
    }
}
