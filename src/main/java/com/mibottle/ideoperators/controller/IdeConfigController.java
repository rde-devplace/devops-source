package com.mibottle.ideoperators.controller;

import com.mibottle.ideoperators.customresource.IdeConfigSpec;
import com.mibottle.ideoperators.service.IdeConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/ideconfig")
public class IdeConfigController {

    @Autowired
    private IdeConfigService ideConfigService;

    @PostMapping("/ide")
    public ResponseEntity<String> createIdeConfig(@RequestParam String name, @RequestParam String namespace, @RequestBody IdeConfigSpec ideConfigSpec) {
        try {
            log.info("Controller createIdeConfig(): " + ideConfigSpec.toString());
            ideConfigService.createIdeConfig(namespace, name + "-vscode-server", ideConfigSpec);
            return new ResponseEntity<>("IdeConfig created successfully.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/ide")
    public ResponseEntity<String> deleteIdeConfig(@RequestParam String name, @RequestParam String namespace) {
        try {
            ideConfigService.deleteIdeConfig(namespace, name + "-vscode-server");
            return new ResponseEntity<>("IdeConfig deleted successfully.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/ide")
    public ResponseEntity<?> getIdeConfigs(@RequestParam String namespace, @RequestParam(required = false) String name) {
        try {
            if (name != null && !name.isEmpty()) {
                log.info("Controller getIdeConfig(): Fetching IdeConfig with name: " + name + " in namespace: " + namespace);
                List<IdeConfigSpec> matchingConfigs = ideConfigService.getIdeConfig(namespace, name);

                if (matchingConfigs.isEmpty()) {
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }

                return new ResponseEntity<>(matchingConfigs.get(0), HttpStatus.OK);
            } else {
                log.info("Controller getIdeConfigs(): Fetching IdeConfigs in namespace: " + namespace);
                List<IdeConfigSpec> configs = ideConfigService.getIdeConfigs(namespace);
                return new ResponseEntity<>(configs, HttpStatus.OK);
            }
        } catch (Exception e) {
            log.error("Error fetching IdeConfigs", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

