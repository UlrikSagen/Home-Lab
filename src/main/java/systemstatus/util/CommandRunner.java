package systemstatus.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class CommandRunner {

    public record Result(int exitCode, String stdout, String stderr, boolean timedOut) {}

    public static Result run(List<String> command, Duration timeout) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);

        Process p = pb.start();

        // Les stdout/stderr parallelt for å unngå deadlock ved fulle buffere
        StringBuilder out = new StringBuilder();
        StringBuilder err = new StringBuilder();

        Thread tOut = new Thread(() -> readAll(p.getInputStream(), out));
        Thread tErr = new Thread(() -> readAll(p.getErrorStream(), err));
        tOut.start();
        tErr.start();

        boolean finished = p.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            p.destroyForcibly();
        }

        tOut.join();
        tErr.join();

        return new Result(finished ? p.exitValue() : -1, out.toString(), err.toString(), !finished);
    }

    private static void readAll(InputStream is, StringBuilder sb) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException ignored) {
        }
    }
}
