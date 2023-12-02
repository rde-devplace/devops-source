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
