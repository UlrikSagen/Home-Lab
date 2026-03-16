package systemstatus.gto;

import java.util.List;

public record HoneypotDashboardGto(
    HoneypotSummaryGto summary,
    List<ActiveSessionGto> activeSessions,
    List<LoginAttemptGto> recentLogins,
    List<CommandGto> recentCommands,
    List<SourceIpGto> topIps,
    List<CredentialGto> topCredentials,
    List<TopCommandGto> topCommands,
    List<ActivityGto> activity,
    List<SourceIpGto> geo,
    List<TcpipEventGto> recentTcpip,
    List<FileTransferGto> recentFiles,
    List<MalwareGto> malware,
    List<DestinationGto> destinations
) {}