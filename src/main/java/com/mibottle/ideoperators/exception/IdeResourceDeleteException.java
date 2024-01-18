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
package com.mibottle.ideoperators.exception;

import com.mibottle.ideoperators.customresource.IdeConfig;

public class IdeResourceDeleteException extends Exception {
    private final IdeConfig resource;
    private final String message;

    public IdeResourceDeleteException(IdeConfig resource, String message, Throwable cause) {
        super(message, cause);
        this.resource = resource;
        this.message = message;
    }

    public IdeConfig getResource() {
        return resource;
    }

    @Override
    public String getMessage() {
        return message + "; Resource Info: " + resource.toString();
    }
}

