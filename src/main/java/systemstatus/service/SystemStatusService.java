package systemstatus.service;

import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

import javax.management.RuntimeErrorException;

import java.util.regex.Matcher;

import systemstatus.gto.CpuStatusGto;
import systemstatus.gto.NvmeStatusGto;
import systemstatus.util.CommandRunner;

@Service
public class SystemStatusService {

    private static final Path TEMP_PATH = Path.of("/sys/class/thermal/thermal_zone0/temp");

    private static final Pattern NVME_TEMP_PATTERN = Pattern.compile("temperature\\s*:\\s*([\\d.]+)");
    private static final Pattern NVME_PERCENTAGE_PATTERN = Pattern.compile("percentage_used\\s*:\\s*([\\d.]+)%?");
    private static final Pattern NVME_WARNING_PATTERN = Pattern.compile("critical_warning\\s*:\\s*(\\d+)");

    private static final int USR_INDEX = 2;
    private static final int SYS_INDEX = 4;
    private static final int IDLE_INDEX = 11;

    public double getTemp() throws IOException{
        String temp = Files.readString(TEMP_PATH).trim();
        double milliTemp = Double.parseDouble(temp);
        return milliTemp;
    }

    public boolean isThrottled() throws IOException, InterruptedException{
        var res = CommandRunner.run(List.of("vcgencmd", "get_throttled"), Duration.ofSeconds(5));
        if (res.timedOut()) throw new RuntimeException("vcgencmd timed out");
        if (res.exitCode() != 0) throw new RuntimeException("vcgencmd failed: " + res.stderr());

        boolean isThrottled = false;
        String hexString = res.stdout();
        if (hexString.startsWith("0x") || hexString.startsWith("0X")) {
            hexString = hexString.substring(2);
        }
        try{
            int decimalValue = Integer.parseInt(hexString);
            if (decimalValue == 0){
                isThrottled = false;
            }
            else{
                isThrottled = true;
            }
        } catch(NumberFormatException e){
            //Log error
        }
        return isThrottled;
    }


    public NvmeStatusGto getNvme()throws IOException, InterruptedException{
        var res = CommandRunner.run(List.of("sudo", "-n", "/usr/sbin/nvme", "smart-log", "/dev/nvme0n1"), Duration.ofSeconds(5));
        if (res.timedOut()) throw new RuntimeException("nvme timed out");
        if (res.exitCode() != 0) {
            throw new RuntimeException("nvme failed: " + res.stderr());
        }
        double nvmeTempC = 0;
        double percentageUsed = 0;
        int criticalWarning = 0;
        for (String line : res.stdout().lines().filter(l -> l.contains("temperature") || l.contains("percentage_used") || l.contains("critical_warning")).toList()){
            Matcher percentMatcher = NVME_PERCENTAGE_PATTERN.matcher(line);
            if (percentMatcher.find()){
                try{
                    percentageUsed = Double.parseDouble(percentMatcher.group(1));
                } catch(NumberFormatException e){
                    //Log error
                }
            }
            Matcher warningMatcher = NVME_WARNING_PATTERN.matcher(line);
            if (warningMatcher.find()){
                try{
                    criticalWarning = Integer.parseInt(warningMatcher.group(1));                
                } catch(NumberFormatException e){
                    //Log error
                }
            }
            Matcher tempMatcher = NVME_TEMP_PATTERN.matcher(line);
            if (tempMatcher.find()){
                try{
                    nvmeTempC = Double.parseDouble(tempMatcher.group(1));                
                } catch(NumberFormatException e){
                    //Log error
                }
            }
        }
        return new NvmeStatusGto(nvmeTempC, percentageUsed, criticalWarning);
    }

    public CpuStatusGto getCpu() throws IOException, InterruptedException{
        var res = CommandRunner.run(List.of("mpstat", "1", "1"), Duration.ofSeconds(5));
        if (res.timedOut()) throw new RuntimeException("mpstat timed out");
        if (res.exitCode() != 0) {
            throw new RuntimeException("mpstat failed. " + res.stderr());
        }
        double usr = 0, sys = 0, idle = 0;
        for(String line : res.stdout().lines().filter(l -> l.contains("all")).toList()){
            String[] parts = line.trim().split("\\s+");
            if (parts.length > IDLE_INDEX){
                try{
                    usr = Double.parseDouble(parts[USR_INDEX]);
                    sys = Double.parseDouble(parts[SYS_INDEX]);
                    idle = Double.parseDouble(parts[IDLE_INDEX]);
                } catch(NumberFormatException e){
                    //Log error
                }
            }
        }
        double cpuTempC = getTemp();
        //Need to implement isThrottled().
        boolean throttled = false;
        return new CpuStatusGto(cpuTempC, usr, sys, idle,false);
    }
}
