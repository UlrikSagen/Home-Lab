package systemstatus.gto;

import java.util.List;

public record IpDetailGto(
    String ip,
    String country,
    String countryCode,
    String city,
    Double latitude,
    Double longitude,
    long totalSessions,
    long totalLogins,
    long successfulLogins,
    long totalCommands,
    String firstSeen,
    String lastSeen,
    List<LoginAttemptGto> recentLogins,
    List<CommandGto> recentCommands,
    List<CredentialGto> topCredentials
) {}