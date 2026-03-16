package systemstatus.gto;

public record HoneypotSummaryGto(
    long totalSessions,
    long activeSessions,
    long totalLogins,
    long successfulLogins,
    long uniqueIps,
    long totalCommands
) {}