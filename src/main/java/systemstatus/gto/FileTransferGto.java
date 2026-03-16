package systemstatus.gto;

public record FileTransferGto(
    String timestamp,
    String sourceIp,
    String eventType,
    String filename,
    String shasum,
    boolean duplicate,
    String country
) {}