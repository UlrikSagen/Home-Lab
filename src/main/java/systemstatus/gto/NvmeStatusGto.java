package systemstatus.gto;

public record NvmeStatusGto(
    double nvmeTempC,
    double percentageWear,
    int criticalWarning
) {}
