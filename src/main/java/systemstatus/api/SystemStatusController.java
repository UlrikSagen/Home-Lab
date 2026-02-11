package systemstatus.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import systemstatus.gto.CpuStatusGto;
import systemstatus.gto.NvmeStatusGto;
import systemstatus.gto.SystemStatusGto;
import systemstatus.service.SystemStatusService;

@RestController
public class SystemStatusController {

    private final SystemStatusService service;

    public SystemStatusController(SystemStatusService service){
        this.service = service;
    }

    @GetMapping("/status")
    public SystemStatusGto status() throws Exception {
        return new SystemStatusGto(service.getCpu(), service.getNvme());
    }

    @GetMapping("/cpu")
    public CpuStatusGto cpu() throws Exception {
        return service.getCpu();
    }

    @GetMapping("/storage")
    public NvmeStatusGto getNvme() throws Exception{
        return service.getNvme();
    }
}
