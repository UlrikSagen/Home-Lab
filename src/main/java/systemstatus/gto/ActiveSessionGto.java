package systemstatus.gto;

public record ActiveSessionGto(
    String sourceIp,
    String startedAt,
    String country,
    String countryCode
) {}