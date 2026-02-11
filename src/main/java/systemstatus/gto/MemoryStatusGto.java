package systemstatus.gto;

public record MemoryStatusGto(
    int usedMB,
    int totalMB,
    int swapUsedMB
){}
