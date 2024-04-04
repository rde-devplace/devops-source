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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "IdeOperator API", description = "Operator에 직접 API 호출을 위한 기능")
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
    @Operation(summary = "새로운 IdeConfig 생성", description = "지정된 세부 사항으로 새로운 IdeConfig를 생성합니다.", responses = {
            @ApiResponse(description = "IdeConfig이(가) 성공적으로 생성되었습니다.", responseCode = "200", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(description = "내부 서버 오류", responseCode = "500", content = @Content)
    })
    @PostMapping("/custom-resource")
    public ResponseEntity<String> createIdeConfig(
            @Parameter(description = "IdeConfig의 이름") @RequestParam String name,
            @Parameter(description = "IdeConfig가 생성될 네임스페이스") @RequestParam String namespace,
            @Parameter(description = "패키지 유형, 제공되지 않으면 'basic'으로 기본 설정됨 (basic, ai, extensions)") @RequestParam(required = false, defaultValue = "basic") String packageType,
            @Parameter(description = "Proxy Domain 정보") @RequestParam(required = false, defaultValue = "env") String proxyDomain,
            @Parameter(description = "IdeConfig의 사양") @RequestBody IdeConfigSpec ideConfigSpec) {
        String ideConfigName = name;
        try {
            log.info("Controller createIdeConfig(): " + ideConfigSpec.toString());
            if(ideConfigSpec.getWsName() == null) {
                ideConfigSpec.setWsName("");
            }
            if(ideConfigSpec.getAppName() == null) {
                ideConfigSpec.setAppName("");
            }
            ideConfigService.createIdeConfig(namespace, ideConfigName, packageType, proxyDomain, ideConfigSpec);
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
    @Operation(summary = "IdeConfig 삭제", description = "지정된 이름과 네임스페이스를 기반으로 기존 IdeConfig를 삭제합니다.", responses = {
            @ApiResponse(description = "IdeConfig이(가) 성공적으로 삭제되었습니다.", responseCode = "200"),
            @ApiResponse(description = "내부 서버 오류", responseCode = "500")
    })
    @DeleteMapping("/custom-resource")
    public ResponseEntity<String> deleteIdeConfig(
            @Parameter(description = "삭제할 IdeConfig의 이름") @RequestParam String name,
            @Parameter(description = "IdeConfig가 생성된 네임스페이스") @RequestParam String namespace) {
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
    @Operation(summary = "IdeConfigs 조회", description = "지정된 네임스페이스 내의 모든 IdeConfigs를 검색합니다. 이름이 제공되면 해당 이름의 IdeConfig를 반환합니다.", responses = {
            @ApiResponse(description = "IdeConfigs 검색 성공", responseCode = "200"),
            @ApiResponse(description = "찾을 수 없음", responseCode = "404"),
            @ApiResponse(description = "내부 서버 오류", responseCode = "500")
    })
    @GetMapping("/custom-resource")
    public ResponseEntity<?> getIdeConfigs(
            @Parameter(description = "IdeConfigs의 네임스페이스") @RequestParam String namespace,
            @Parameter(description = "IdeConfig의 이름, 선택사항") @RequestParam(required = false) String name
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
    @Operation(summary = "사용자별 IdeConfig 목록 조회", description = "지정된 네임스페이스에 있는, 지정된 사용자가 생성한 IdeConfigs를 검색합니다. workspaceName과 appName으로 필터링할 수 있습니다.", responses = {
            @ApiResponse(description = "IdeConfigs 검색 성공", responseCode = "200"),
            @ApiResponse(description = "내부 서버 오류", responseCode = "500")
    })
    @GetMapping("/custom-resource/user")
    public ResponseEntity<?> getIdeConfigListWithUser(
            @Parameter(description = "IdeConfigs의 네임스페이스") @RequestParam String namespace,
            @Parameter(description = "RDE를 생성하고 사용할 권한이 있는 사용자 계정") @RequestParam String userName,
            @Parameter(description = "workspace의 이름, 선택사항") @RequestParam(required = false) String workspaceName,
            @Parameter(description = "동일 workspace 내 여러 RDE를 구분하기 위한 이름, 선택사항") @RequestParam(required = false) String appName) {
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
    @Operation(summary = "지정된 이름의 IdeConfigSpec 조회", description = "주어진 네임스페이스에서 지정된 IdeConfig 이름의 IdeConfigSpec를 검색합니다.", responses = {
            @ApiResponse(description = "IdeConfigSpec 검색 성공", responseCode = "200"),
            @ApiResponse(description = "내부 서버 오류", responseCode = "500")
    })
    @GetMapping("/custom-resource/spec")
    public ResponseEntity<?> getIdeConfigSpec(
            @Parameter(description = "IdeConfig의 네임스페이스") @RequestParam String namespace,
            @Parameter(description = "IdeConfig의 이름") @RequestParam String name) {
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
    @Operation(summary = "IdeConfig 업데이트", description = "지정된 이름과 네임스페이스를 가진 IdeConfig를 업데이트합니다.", responses = {
            @ApiResponse(description = "IdeConfig이(가) 성공적으로 업데이트되었습니다.", responseCode = "200"),
            @ApiResponse(description = "내부 서버 오류", responseCode = "500")
    })
    @PutMapping("/custom-resource")
    public ResponseEntity<String> updateIdeConfig(
            @Parameter(description = "업데이트할 IdeConfig의 이름") @RequestParam String name,
            @Parameter(description = "IdeConfig가 생성된 네임스페이스") @RequestParam String namespace,
            @Parameter(description = "IdeConfigSpec 객체") @RequestBody IdeConfigSpec ideConfigSpec) {
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

