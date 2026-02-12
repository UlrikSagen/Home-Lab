package systemstatus.gto;

public record DockerStatusGto(
    String name,
    String id,
    String image,
    String uptime,
    String runningSince
){}