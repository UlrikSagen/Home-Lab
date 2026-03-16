package systemstatus.gto;

public record TcpipEventGto(
    String timestamp,
    String sourceIp,
    String dstIp,
    int dstPort,
    String eventType,
    String data,
    String ja4h,
    String country
) {}