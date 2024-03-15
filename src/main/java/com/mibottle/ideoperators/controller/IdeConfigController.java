/*
 * Copyright (c) 2023 himang10@gmail.com, Yi Yongwoo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mibottle.ideoperators.controller;

import com.mibottle.ideoperators.customresource.IdeConfig;
import com.mibottle.ideoperators.customresource.IdeConfigSpec;
import com.mibottle.ideoperators.model.IdeCommon;
import com.mibottle.ideoperators.service.IdeConfigService;
import com.mibottle.ideoperators.util.SVCPathGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/ide-configs")
public class IdeConfigController {

    @Autowired
    private IdeConfigService ideConfigService;

    /**
     * IdeConfig를 생성한다.
     * @param name IdeConfig의 이름
     * @param namespace IdeConfig가 생성될 namespace
     * @param ideConfigSpec IdeConfigSpec 객체
     * @return ResponseEntity
     */
    @PostMapping("/custom-resource")
    public ResponseEntity<String> createIdeConfig(@RequestParam String name, @RequestParam String namespace, @RequestBody IdeConfigSpec ideConfigSpec) {
        String ideConfigName = name;
        try {
            log.info("Controller createIdeConfig(): " + ideConfigSpec.toString());
            ideConfigService.createIdeConfig(namespace, ideConfigName, ideConfigSpec);
            return new ResponseEntity<>("IdeConfig created successfully.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * IdeConfig를 삭제한다.
     * @param name IdeConfig의 이름
     * @param namespace IdeConfig가 생성된 namespace
     * @return
     */
    @DeleteMapping("/custom-resource")
    public ResponseEntity<String> deleteIdeConfig(@RequestParam String name, @RequestParam String namespace) {
        String ideConfigName = name;
        try {
            ideConfigService.deleteIdeConfig(namespace, ideConfigName);
            return new ResponseEntity<>("IdeConfig deleted successfully.", HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 동일 namespace 내에 모든 IdeConfig를 조회한다.
     * name은 IdeConfig의 이름이며, name을 지정하면 해당하는 IdeConfig를 조회한다.
     * @param namespace IdeConfig가 생성된 namespace
     * @param name IdeConfig의 이름
     * @return
     */
    @GetMapping("/custom-resource")
    public ResponseEntity<?> getIdeConfigs(
            @RequestParam String namespace,
            @RequestParam(required = false) String name
) {
        String ideConfigName = name;
        try {
            if (ideConfigName != null && !ideConfigName.isEmpty()) {
                log.debug("Controller getIdeConfig(): Fetching IdeConfig with name: " + ideConfigName + " in namespace: " + namespace);
                List<IdeConfig> matchingConfigs = new ArrayList<>();
                IdeConfig ideConfig = ideConfigService.getIdeConfig(namespace, ideConfigName);

                if (ideConfig == null) {
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                }

                return new ResponseEntity<>(matchingConfigs.add(ideConfig), HttpStatus.OK);
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

    /**
     * 동일 namespace 내에 userName으로 생성된 모든 IdeConfig를 조회한다.
     * 조회 시 workspaceName과 appName을 지정할 수 있다.
     * workspaceName과 appName을 지정하지 않으면 해당하는 모든 IdeConfig를 조회하며,
     * workspaceName과 appName 중 한개만 지정하여 조회할 수 있다
     * @param namespace IdeConfig가 생성된 namespace
     * @param userName RDE를 생성하고 사용할 수 있는 권한을 가진 사용자 계정
     * @param workspaceName IdeConfig가 생성될 workspace의 이름
     * @param appName 동일 workspace 내에 두 개 이상의 RDE 생성 시 구분할 수 있는 이름
     * @return
     */
    @GetMapping("/custom-resource/user")
    public ResponseEntity<?> getIdeConfigListWithUser(
            @RequestParam String namespace,
            @RequestParam String userName,
            @RequestParam(required = false) String workspaceName,
            @RequestParam(required = false) String appName) {
        try {
            log.debug("Controller getIdeConfigs(): Fetching IdeConfigs in namespace: " + namespace);
            List<IdeConfig> ideConfigs = ideConfigService.getIdeConfigs(namespace, userName, workspaceName, appName);
            return new ResponseEntity<>(ideConfigs, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error fetching IdeConfigs", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * 동일 namespace 내에 지정된 IdeConfig Name에 해당하는 IdeConfigSpec를 조회한다.
     * @param namespace IdeConfig가 생성된 namespace
     * @param name IdeConfig의 이름
     * @return IdeConfigSpec
     */
    @GetMapping("/custom-resource/spec")
    public ResponseEntity<?> getIdeConfigSpec(@RequestParam String namespace, @RequestParam String name) {
        String ideConfigName = name;
        try {
            log.debug("Controller getIdeConfigSpec(): Fetching IdeConfigSpec with name: " + name + " in namespace: " + namespace);
            IdeConfig ideConfig = ideConfigService.getIdeConfig(namespace, ideConfigName);

            return new ResponseEntity<>(ideConfig.getSpec(), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error fetching IdeConfigSpec", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * IdeConfig를 업데이트한다.
     * @param name IdeConfig의 이름
     * @param namespace IdeConfig가 생성된 namespace
     * @param ideConfigSpec IdeConfigSpec 객체
     * @return ResponseEntity
     */
    @PutMapping("/custom-resource")
    public ResponseEntity<String> updateIdeConfig(
            @RequestParam String name,
            @RequestParam String namespace,
            @RequestBody IdeConfigSpec ideConfigSpec) {
        String ideConfigName = name;
        try {
            log.info("Controller updateIdeConfig(): Updating IdeConfig with name: " + name + " in namespace: " + namespace);
            ideConfigService.updateIdeConfig(namespace, ideConfigName, ideConfigSpec);
            return new ResponseEntity<>("IdeConfig updated successfully.", HttpStatus.OK);
        } catch (Exception e) {
            log.error("Error updating IdeConfig", e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

