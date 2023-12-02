package com.mibottle.ideoperators.customresource;

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;


@Data
@AllArgsConstructor
@Builder

//public class IdeConfigStatus extends ObservedGenerationAwareStatus {
public class IdeConfigStatus {

        private String message;
        private Boolean isReady;

        public IdeConfigStatus() {
            isReady = false;
            message = "Not ready";
        }

        @Override
        public String toString() {
            return "Ide execution environment is " + isReady + "\n" + "detailed status: " +  message;
        }
}
