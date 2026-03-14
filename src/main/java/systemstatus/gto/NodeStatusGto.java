package systemstatus.gto;

public record NodeStatusGto(
    String ip,
    SystemStatusGto status,
    Boolean reachable
) {}
