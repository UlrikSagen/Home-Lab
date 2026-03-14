package systemstatus.gto;

import java.util.List;

public record ClusterStatusGto(
    List<NodeStatusGto> nodes
) {}
