package systemstatus.gto;

public record SystemStatusGto(
    CpuStatusGto cpu,
    NvmeStatusGto nvme
    //KernelStatusGto kernel,
    //MemoryStatusGto memory
){}