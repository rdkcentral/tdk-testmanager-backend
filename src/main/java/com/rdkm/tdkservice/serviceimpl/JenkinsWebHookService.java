/*
* If not stated otherwise in this file or this component's LICENSE file the
* following copyright and licenses apply:
*
* Copyright 2026 RDK Management
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*
http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.rdkm.tdkservice.serviceimpl;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rdkm.tdkservice.dto.ResultDTO;

/**
 * Service class responsible for sending ResultDTO to Jenkins webhook with
 * HMAC-SHA256 signature.
 */
@Service
public class JenkinsWebHookService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JenkinsWebHookService.class);

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Sends the given ResultDTO to the specified Jenkins webhook URL.
     * Signs the payload with HMAC-SHA256 using the provided {@code secret}.
     * If {@code secret} is null or blank the request is sent without a
     * signature header and a warning is logged.
     *
     * @param resultDTO   the ResultDTO to send
     * @param callBackUrl the Jenkins webhook URL
     * @param secret      the HMAC-SHA256 secret for this CI app; may be null
     */
    public void sendResultToJenkinsWebhook(ResultDTO resultDTO, String callBackUrl, String secret) {
        LOGGER.info("Preparing to send ResultDTO to Jenkins webhook at URL: {}", callBackUrl);
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            byte[] payloadBytes = objectMapper.writeValueAsBytes(resultDTO);
            LOGGER.debug("Serialized ResultDTO to JSON: {}",
                    new String(payloadBytes, StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            if (secret != null && !secret.isBlank()) {
                Mac mac = Mac.getInstance(HMAC_ALGORITHM);
                mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
                byte[] signatureBytes = mac.doFinal(payloadBytes);

                StringBuilder hexBuilder = new StringBuilder();
                for (byte b : signatureBytes) {
                    hexBuilder.append(String.format("%02x", b));
                }
                headers.set("X-Signature-256", "sha256=" + hexBuilder);
                LOGGER.debug("HMAC-SHA256 signature added for URL: {}", callBackUrl);
            } else {
                LOGGER.warn("No HMAC secret for callbackUrl [{}] – sending without X-Signature-256 header",
                        callBackUrl);
            }

            HttpEntity<byte[]> request = new HttpEntity<>(payloadBytes, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    callBackUrl, HttpMethod.POST, request, String.class);

            LOGGER.info("Jenkins webhook response: status={}, body={}",
                    response.getStatusCode(), response.getBody());

        } catch (Exception e) {
            LOGGER.error("Failed to send ResultDTO to Jenkins webhook", e);
        }
    }
}
