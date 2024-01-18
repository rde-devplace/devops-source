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
package com.mibottle.ideoperators.util;

import com.mibottle.ideoperators.customresource.IdeConfig;
import com.mibottle.ideoperators.customresource.IdeConfigStatus;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;

public class UpdateStatusHandler {
    private UpdateStatusHandler() {}

    public static IdeConfigStatus createStatus(String message, Boolean isReady) {
        return IdeConfigStatus.builder()
                .message(message)
                .isReady(isReady)
                .build();
    }

    public static IdeConfig updateStatus(IdeConfig ideConfig, String message, Boolean isReady) {
        if (ideConfig.getStatus() == null ) {
            ideConfig.setStatus(new IdeConfigStatus());
        }
        ideConfig.getStatus().setMessage(message);
        ideConfig.getStatus().setIsReady(isReady);

        return ideConfig;
    }

    public static ErrorStatusUpdateControl<IdeConfig> handleError(IdeConfig resource, Exception e) {
        resource.getStatus().setMessage("Error: " + e.getMessage());
        resource.getStatus().setIsReady(false);
        return ErrorStatusUpdateControl.updateStatus(resource);
    }


}
