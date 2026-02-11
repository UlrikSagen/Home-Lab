package systemstatus.gto;

public record NvmeStatusGto(
    double nvmeTempC,
    double presentageUsed,
    int criticalWarning
) {}
