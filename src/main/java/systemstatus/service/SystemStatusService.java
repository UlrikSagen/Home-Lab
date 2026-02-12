package systemstatus.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import systemstatus.gto.CpuStatusGto;
import systemstatus.gto.DiskStatusGto;
import systemstatus.gto.MemoryStatusGto;
import systemstatus.gto.NvmeStatusGto;
import systemstatus.util.CommandRunner;

@Service
public class SystemStatusService {

    private static final Path TEMP_PATH = Path.of("/sys/class/thermal/thermal_zone0/temp");
    private static final Path MEM_PATH = Path.of("/proc/meminfo");

    private static final Pattern NVME_TEMP_PATTERN = Pattern.compile("temperature\\s*:\\s*([\\d.]+)");
    private static final Pattern NVME_PERCENTAGE_PATTERN = Pattern.compile("percentage_used\\s*:\\s*([\\d.]+)%?");
    private static final Pattern NVME_WARNING_PATTERN = Pattern.compile("critical_warning\\s*:\\s*(\\d+)");

    private static final int USR_INDEX = 2;
    private static final int SYS_INDEX = 4;
    private static final int IDLE_INDEX = 11;

    private static final List<String> MOUNT_POINTS = List.of("/", "/srv");

    public double getTemp() throws IOException, FileNotFoundException{
        String temp = Files.readString(TEMP_PATH).trim();
        double milliTemp = Double.parseDouble(temp);
        return milliTemp/1000;
    }

    public boolean isThrottled() throws IOException, InterruptedException{
        var res = CommandRunner.run(List.of("vcgencmd", "get_throttled"), Duration.ofSeconds(5));
        if (res.timedOut()) throw new RuntimeException("vcgencmd timed out");
        if (res.exitCode() != 0) throw new RuntimeException("vcgencmd failed: " + res.stderr());

        boolean isThrottled = false;
        String hexString = res.stdout();
        if (hexString.startsWith("throttled=0x") || hexString.startsWith("0X")) {
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
        double percentageWear = 0;
        int criticalWarning = 0;
        for (String line : res.stdout().lines().filter(l -> l.contains("temperature") || l.contains("percentage_used") || l.contains("critical_warning")).toList()){
            Matcher percentMatcher = NVME_PERCENTAGE_PATTERN.matcher(line);
            if (percentMatcher.find()){
                try{
                    percentageWear = Double.parseDouble(percentMatcher.group(1));
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
        return new NvmeStatusGto(nvmeTempC, percentageWear, criticalWarning);
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
        boolean throttled = isThrottled();
        return new CpuStatusGto(cpuTempC, usr, sys, idle, throttled);
    }

    public MemoryStatusGto getMemory() throws IOException, FileNotFoundException{
        List<String> memoryLines = Files.readAllLines(MEM_PATH);
        long memTotalMb = 0, memAvailableMb = 0, memUsedMb = 0, swapTotalMb = 0, swapFreeMb = 0;
        for(String line : memoryLines){
            String[] parts = line.trim().split("\\s+");

            if (line.startsWith("MemTotal:")){
                memTotalMb = Long.parseLong(parts[1]);
            } else if ( line.startsWith("MemAvailable:")){
                memAvailableMb = Long.parseLong(parts[1]);
            } else if ( line.startsWith("SwapTotal:")){
                swapTotalMb = Long.parseLong(parts[1]);
            } else if ( line.startsWith("SwapFree:")){
                swapFreeMb = Long.parseLong(parts[1]);
            }
        }

        memUsedMb = memTotalMb - memAvailableMb;
        return new MemoryStatusGto(memTotalMb, memUsedMb, swapTotalMb, swapFreeMb);
    }

    public List<DiskStatusGto> getDisks() throws IOException{
        List<DiskStatusGto> disks = new ArrayList<>();

        for(String path : MOUNT_POINTS){
            try{
                FileStore store = Files.getFileStore(Path.of(path));

                float total = store.getTotalSpace();
                float usable = store.getUsableSpace();
                float used = total - usable;

                disks.add(new DiskStatusGto(path, total / (1024*1024*1024),used / (1024*1024*1024),(int)((used * 100.0) / total)
            ));
            } catch(IOException e){
                //Log error
            }
        }
        return disks;
    }
}
