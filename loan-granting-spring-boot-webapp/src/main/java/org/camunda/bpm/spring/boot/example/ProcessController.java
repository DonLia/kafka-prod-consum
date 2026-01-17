package org.camunda.bpm.spring.boot.example;

import org.camunda.bpm.engine.RuntimeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class ProcessController {

    private final RuntimeService runtimeService;

    public ProcessController(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    @PostMapping("start-loan")
    public String startLoan(@RequestBody(required = false) Map<String, Object> vars) {
        if (vars == null) {
            vars = Map.of(); // empty map
        }
        runtimeService.startProcessInstanceByKey("loan_process", vars);
        return "credit checker started";
    }
}