package systemstatus.gto;

public record MemoryStatusGto(
    long totalMb,    
    long usedMb,
    long swapFreeMb,
    long swapTotalMb
){}
