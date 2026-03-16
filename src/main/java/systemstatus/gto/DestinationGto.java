package systemstatus.gto;

public record DestinationGto (
    String dstIp,
    int dstPort,
    long count
) {}