package com.mibottle.ideoperators.controller;

import com.mibottle.ideoperators.customresource.IdeConfig;
import com.mibottle.ideoperators.customresource.IdeConfigSpec;
import com.mibottle.ideoperators.model.IdeCommon;
import com.mibottle.ideoperators.service.IdeConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/ide-configs")
public class IdeConfigController {

    @Autowired
    private IdeConfigService ideConfigService;

    @PostMapping("/custom-resource")
    public ResponseEntity<String> createIdeConfig(@RequestParam String name, @RequestParam String namespace, @RequestBody IdeConfigSpec ideConfigSpec) {
        try {
            log.info("Controller createIdeConfig(): " + ideConfigSpec.toString());
            ideConfigService.createIdeConfig(namespace, name + IdeCommon.IDECONFIG_POSTFIX, ideConfigSpec);
            return new ResponseEntity<>("IdeConfig created successfully.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/custom-resource")
    public ResponseEntity<String> deleteIdeConfig(@RequestParam String name, @RequestParam String namespace) {
        try {
            ideConfigService.deleteIdeConfig(namespace, name + "-vscode-server");
            return new ResponseEntity<>("IdeConfig deleted successfully.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/custom-resource")
    public ResponseEntity<?> getIdeConfigs(@RequestParam String namespace, @RequestParam(required = false) String name) {
        try {
            if (name != null && !name.isEmpty()) {
                log.debug("Controller getIdeConfig(): Fetching IdeConfig with name: " + name + " in namespace: " + namespace);
                List<IdeConfig> matchingConfigs = ideConfigService.getIdeConfig(namespace, name);

                if (matchingConfigs.isEmpty()) {
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }

                //return new ResponseEntity<>(matchingConfigs.get(0), HttpStatus.OK);
                return new ResponseEntity<>(matchingConfigs, HttpStatus.OK);
            } else {
                log.debug("Controller getIdeConfigs(): Fetching IdeConfigs in namespace: " + namespace);
                List<IdeConfig> configs = ideConfigService.getIdeConfigs(namespace);
                return new ResponseEntity<>(configs, HttpStatus.OK);
            }
        } catch (Exception e) {
            log.error("Error fetching IdeConfigs", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/custom-resource/spec")
    public ResponseEntity<?> getIdeConfigSpec(@RequestParam String namespace, @RequestParam String name) {
        try {
            log.debug("Controller getIdeConfigSpec(): Fetching IdeConfigSpec with name: " + name + " in namespace: " + namespace);
            List<IdeConfig> ideConfigs = ideConfigService.getIdeConfig(namespace, name);

            // 모든 ideConfigs 요소에 대해 getSpec()을 호출하여 specList에 추가
            List<IdeConfigSpec> specList = ideConfigs.stream()
                    .map(IdeConfig::getSpec)
                    .collect(Collectors.toList());

            return new ResponseEntity<>(specList, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error fetching IdeConfigSpec", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping("/custom-resource")
    public ResponseEntity<String> updateIdeConfig(
            @RequestParam String name,
            @RequestParam String namespace,
            @RequestBody IdeConfigSpec ideConfigSpec) {
        try {
            log.info("Controller updateIdeConfig(): Updating IdeConfig with name: " + name + " in namespace: " + namespace);
            ideConfigService.updateIdeConfig(namespace, name + IdeCommon.IDECONFIG_POSTFIX, ideConfigSpec);
            return new ResponseEntity<>("IdeConfig updated successfully.", HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error updating IdeConfig", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

