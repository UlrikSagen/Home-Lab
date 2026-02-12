package systemstatus.gto;

public record DiskStatusGto(
    String path,
    float diskTotalGb,
    float diskUsedGb,
    float diskUsedPercent
) {}
