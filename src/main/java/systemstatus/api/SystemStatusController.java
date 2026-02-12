package systemstatus.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import systemstatus.gto.*;
import systemstatus.service.SystemStatusService;

@RestController
public class SystemStatusController {

    private final SystemStatusService service;

    public SystemStatusController(SystemStatusService service){
        this.service = service;
    }

    @GetMapping("/status")
    public SystemStatusGto status() throws Exception {
        return new SystemStatusGto(service.getCpu(), service.getNvme(), service.getMemory(), service.getDisks(), service.getKernel());
    }

    @GetMapping("/cpu")
    public CpuStatusGto cpu() throws Exception {
        return service.getCpu();
    }

    @GetMapping("/nvme")
    public NvmeStatusGto getNvme() throws Exception{
        return service.getNvme();
    }

    @GetMapping("/memory")
    public MemoryStatusGto getMemory() throws Exception{
        return service.getMemory();
    }

    @GetMapping("/disks")
    public List<DiskStatusGto> getDisks() throws Exception{
        return service.getDisks();
    }

    @GetMapping("/kernel")
    public KernelStatusGto getKernel() throws Exception{
        return service.getKernel();
    }

    @GetMapping("/docker")
    public List<DockerStatusGto> getDockerContainers() throws Exception{
        return service.getDockerContainers();
    }
}
