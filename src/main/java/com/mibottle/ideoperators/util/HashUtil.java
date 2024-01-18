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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

    /**
     * Generates a SHA-256 hash for the given object.
     *
     * @param object The object to be hashed.
     * @return The generated SHA-256 hash.
     */
    public static String generateSHA256Hash(Object object) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String objectAsString = objectMapper.writeValueAsString(object);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(objectAsString.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                hexString.append(Integer.toHexString(0xFF & b));
            }

            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate SHA-256 hash", e);
        }
    }
}

