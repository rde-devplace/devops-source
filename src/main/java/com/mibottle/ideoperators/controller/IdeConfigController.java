package com.mibottle.ideoperators.controller;

import com.mibottle.ideoperators.customresource.IdeConfigSpec;
import com.mibottle.ideoperators.service.IdeConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/ideConfig")
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
}

