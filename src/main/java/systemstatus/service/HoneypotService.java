package systemstatus.service;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import systemstatus.gto.*;

@Service
public class HoneypotService {

    private final JdbcTemplate jdbc;

    public HoneypotService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public HoneypotDashboardGto getDashboard() {
        return new HoneypotDashboardGto(getSummary(), getActiveSessions(), getRecentLogins(20), getRecentCommands(20), getTopIps(20),
            getTopCredentials(20), getTopCommands(20), getHourlyActivity(48), getGeoData(), getRecentTcpip(20), getRecentFileTransfers(20), getMalware(), getDestinations());
    }

    public HoneypotSummaryGto getSummary() {
        return jdbc.queryForObject("""
            SELECT
                (SELECT COUNT(*) FROM cowrie_sessions),
                (SELECT COUNT(*) FROM cowrie_sessions WHERE ended_at IS NULL),
                (SELECT COUNT(*) FROM cowrie_logins),
                (SELECT COUNT(*) FROM cowrie_logins WHERE success = true),
                (SELECT COUNT(DISTINCT src_ip) FROM cowrie_sessions),
                (SELECT COUNT(*) FROM cowrie_commands)
            """,
            (rs, i) -> new HoneypotSummaryGto(
                rs.getLong(1), rs.getLong(2), rs.getLong(3),
                rs.getLong(4), rs.getLong(5), rs.getLong(6)
            ));
    }

    public List<ActiveSessionGto> getActiveSessions(){
        return jdbc.query("""
            SELECT s.src_ip, s.started_at, g.country, g. country_code FROM cowrie_sessions s
            LEFT JOIN ip_geo g ON s.src_ip = g.ip 
            WHERE ended_at IS NULL
            """,
            (rs, i) -> new ActiveSessionGto(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
    }

    public List<LoginAttemptGto> getRecentLogins(int limit) {
        return jdbc.query("""
            SELECT l.timestamp, l.src_ip, l.username, l.password, l.success,
                   g.country, g.country_code, g.city
            FROM cowrie_logins l
            LEFT JOIN ip_geo g ON l.src_ip = g.ip
            ORDER BY l.timestamp DESC
            LIMIT ?
            """,
            (rs, i) -> new LoginAttemptGto(
                rs.getString(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getBoolean(5),
                rs.getString(6), rs.getString(7), rs.getString(8)
            ), limit);
    }

    public List<CredentialGto> getTopCredentials(int limit) {
        return jdbc.query("""
            SELECT username, password, COUNT(*) as count
            FROM cowrie_logins
            GROUP BY username, password
            ORDER BY count DESC
            LIMIT ?
            """,
            (rs, i) -> new CredentialGto(
                rs.getString(1), rs.getString(2), rs.getLong(3)
            ), limit);
    }

    public List<SourceIpGto> getTopIps(int limit) {
        return jdbc.query("""
            SELECT s.src_ip, COUNT(*) as count,
                   g.country, g.country_code, g.city, g.latitude, g.longitude
            FROM cowrie_sessions s
            LEFT JOIN ip_geo g ON s.src_ip = g.ip
            GROUP BY s.src_ip, g.country, g.country_code, g.city, g.latitude, g.longitude
            ORDER BY count DESC
            LIMIT ?
            """,
            (rs, i) -> new SourceIpGto(
                rs.getString(1), rs.getLong(2), rs.getString(3),
                rs.getString(4), rs.getString(5),
                rs.getObject(6, Double.class), rs.getObject(7, Double.class)
            ), limit);
    }

    public List<CommandGto> getRecentCommands(int limit) {
        return jdbc.query("""
            SELECT c.timestamp, c.src_ip, c.input, g.country
            FROM cowrie_commands c
            LEFT JOIN ip_geo g ON c.src_ip = g.ip
            ORDER BY c.timestamp DESC
            LIMIT ?
            """,
            (rs, i) -> new CommandGto(
                rs.getString(1), rs.getString(2),
                rs.getString(3), rs.getString(4)
            ), limit);
    }

    public List<TopCommandGto> getTopCommands(int limit) {
        return jdbc.query("""
            SELECT input, COUNT(*) as count
            FROM cowrie_commands
            GROUP BY input
            ORDER BY count DESC
            LIMIT ?
            """,
            (rs, i) -> new TopCommandGto(
                rs.getString(1), rs.getLong(2)
            ), limit);
    }

    public List<ActivityGto> getHourlyActivity(int hours) {
        return jdbc.query("""
            SELECT TO_CHAR(date_trunc('hour', timestamp), 'YYYY-MM-DD HH24:00') as period,
                   COUNT(*) as count
            FROM cowrie_logins
            WHERE timestamp > NOW() - MAKE_INTERVAL(hours => ?)
            GROUP BY period
            ORDER BY period
            """,
            (rs, i) -> new ActivityGto(
                rs.getString(1), rs.getLong(2)
            ), hours);
    }

    public List<SourceIpGto> getGeoData() {
        return jdbc.query("""
            SELECT g.ip, COUNT(s.id) as count,
                   g.country, g.country_code, g.city, g.latitude, g.longitude
            FROM ip_geo g
            JOIN cowrie_sessions s ON s.src_ip = g.ip
            WHERE g.latitude IS NOT NULL
            GROUP BY g.ip, g.country, g.country_code, g.city, g.latitude, g.longitude
            """,
            (rs, i) -> new SourceIpGto(
                rs.getString(1), rs.getLong(2), rs.getString(3),
                rs.getString(4), rs.getString(5),
                rs.getObject(6, Double.class), rs.getObject(7, Double.class)
            ));
    }

    public IpDetailGto getIpDetail(String ip) {
        // 1. Geo-data
        var geo = jdbc.query(
            "SELECT country, country_code, city, latitude, longitude FROM ip_geo WHERE ip = ?",
            (rs, i) -> new Object[]{rs.getString(1), rs.getString(2), rs.getString(3),
                rs.getObject(4, Double.class), rs.getObject(5, Double.class)}, ip);

        String country = null, countryCode = null, city = null;
        Double lat = null, lon = null;
        if (!geo.isEmpty()) {
            Object[] g = geo.get(0);
            country = (String) g[0]; countryCode = (String) g[1]; city = (String) g[2];
            lat = (Double) g[3]; lon = (Double) g[4];
        }

        // 2. Statistikk
        var stats = jdbc.queryForObject("""
            SELECT
                (SELECT COUNT(*) FROM cowrie_sessions WHERE src_ip = ?),
                (SELECT COUNT(*) FROM cowrie_logins WHERE src_ip = ?),
                (SELECT COUNT(*) FROM cowrie_logins WHERE src_ip = ? AND success = true),
                (SELECT COUNT(*) FROM cowrie_commands WHERE src_ip = ?)
            """,
            (rs, i) -> new long[]{rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4)},
            ip, ip, ip, ip);

        // 3. First/last seen
        var times = jdbc.queryForObject(
            "SELECT MIN(started_at), MAX(started_at) FROM cowrie_sessions WHERE src_ip = ?",
            (rs, i) -> new String[]{rs.getString(1), rs.getString(2)}, ip);

        // 4. Siste logins
        var logins = jdbc.query("""
            SELECT l.timestamp, l.src_ip, l.username, l.password, l.success,
                   g.country, g.country_code, g.city
            FROM cowrie_logins l LEFT JOIN ip_geo g ON l.src_ip = g.ip
            WHERE l.src_ip = ? ORDER BY l.timestamp DESC LIMIT 50
            """,
            (rs, i) -> new LoginAttemptGto(rs.getString(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getBoolean(5), rs.getString(6), rs.getString(7), rs.getString(8)),
            ip);

        // 5. Siste kommandoer
        var commands = jdbc.query("""
            SELECT c.timestamp, c.src_ip, c.input, g.country
            FROM cowrie_commands c LEFT JOIN ip_geo g ON c.src_ip = g.ip
            WHERE c.src_ip = ? ORDER BY c.timestamp DESC LIMIT 50
            """,
            (rs, i) -> new CommandGto(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)),
            ip);

        // 6. Topp credentials for denne IP-en
        var creds = jdbc.query("""
            SELECT username, password, COUNT(*) as count FROM cowrie_logins
            WHERE src_ip = ? GROUP BY username, password ORDER BY count DESC LIMIT 10
            """,
            (rs, i) -> new CredentialGto(rs.getString(1), rs.getString(2), rs.getLong(3)),
            ip);
        
        return new IpDetailGto(ip, country, countryCode, city, lat, lon,
            stats[0], stats[1], stats[2], stats[3],
            times[0], times[1],
            logins, commands, creds);
    }

    public List<TcpipEventGto> getRecentTcpip(int limit) {
        return jdbc.query("""
            SELECT t.timestamp, t.src_ip, t.dst_ip, t.dst_port, t.event_type,
                   t.data, t.ja4h, g.country
            FROM cowrie_tcpip t
            LEFT JOIN ip_geo g ON t.src_ip = g.ip
            ORDER BY t.timestamp DESC
            LIMIT ?
            """,
            (rs, i) -> new TcpipEventGto(
                rs.getString(1), rs.getString(2), rs.getString(3),
                rs.getInt(4), rs.getString(5), rs.getString(6),
                rs.getString(7), rs.getString(8)
            ), limit);
    }

    public List<FileTransferGto> getRecentFileTransfers(int limit) {
        return jdbc.query("""
            SELECT f.timestamp, f.src_ip, f.event_type, f.filename,
                   f.shasum, f.duplicate, g.country
            FROM cowrie_file_transfers f
            LEFT JOIN ip_geo g ON f.src_ip = g.ip
            ORDER BY f.timestamp DESC
            LIMIT ?
            """,
            (rs, i) -> new FileTransferGto(
                rs.getString(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getBoolean(6),
                rs.getString(7)
            ), limit);
    }

    public List<MalwareGto> getMalware() {
        return jdbc.query("""
            SELECT shasum, MIN(filename), COUNT(*) as count,
                   COUNT(DISTINCT src_ip) as unique_ips
            FROM cowrie_file_transfers
            WHERE shasum IS NOT NULL
            GROUP BY shasum
            ORDER BY count DESC
            """,
            (rs, i) -> new MalwareGto(
                rs.getString(1), rs.getString(2),
                rs.getLong(3), rs.getLong(4)
            ));
    }

    public List<DestinationGto> getDestinations() {
        return jdbc.query("""
            SELECT dst_ip, dst_port, COUNT(*) as count
            FROM cowrie_tcpip
            WHERE event_type = 'request'
            GROUP BY dst_ip, dst_port
            ORDER BY count DESC
            """,
            (rs, i) -> new DestinationGto(
                rs.getString(1), rs.getInt(2), rs.getLong(3)
            ));
    }
}