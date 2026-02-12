package systemstatus.gto;

import java.util.List;

public record SystemStatusGto(
    CpuStatusGto cpu,
    NvmeStatusGto nvme,
    MemoryStatusGto memory,
    List<DiskStatusGto> disks,
    KernelStatusGto kernel
){}