package systemstatus.gto;

public record CpuStatusGto(
    double cpuTempC,
    double user,
    double system,
    double idle,
    boolean throttled
){}