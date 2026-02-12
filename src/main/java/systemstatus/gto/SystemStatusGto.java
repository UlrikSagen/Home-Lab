package systemstatus.gto;

import java.util.List;

public record SystemStatusGto(
    CpuStatusGto cpu,
    NvmeStatusGto nvme,
    //KernelStatusGto kernel,
    MemoryStatusGto memory,
    List<DiskStatusGto> disks
){}