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

    @Value("${jenkins.webhook.hmac-secret}")
    private String hmacSecret;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Sends the given ResultDTO to the specified Jenkins webhook URL with an
     * HMAC-SHA256 signature.
     *
     * @param resultDTO   the ResultDTO to send
     * @param callBackUrl the Jenkins webhook URL to send the ResultDTO to
     */
    public void sendResultToJenkinsWebhook(ResultDTO resultDTO, String callBackUrl) {
        LOGGER.info("Preparing to send ResultDTO to Jenkins webhook at URL: {}", callBackUrl);
        try {
            // Serialize ResultDTO to JSON — must use the exact bytes for HMAC
            ObjectMapper objectMapper = new ObjectMapper();
            byte[] payloadBytes = objectMapper.writeValueAsBytes(resultDTO);
            String payloadJson = new String(payloadBytes, StandardCharsets.UTF_8);
            LOGGER.debug("Serialized ResultDTO to JSON: {}", payloadJson);

            // Compute HMAC-SHA256 over the raw payload bytes
            if (hmacSecret == null || hmacSecret.isBlank()) {
                throw new IllegalStateException("jenkins.webhook.hmac-secret is not configured");
            }
            // Compute HMAC-SHA256 over the raw payload bytes
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(hmacSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] signatureBytes = mac.doFinal(payloadBytes);

            // Encode signature as lowercase hex: "sha256=<hex>"
            StringBuilder hexBuilder = new StringBuilder();
            for (byte b : signatureBytes) {
                hexBuilder.append(String.format("%02x", b));
            }
            String signatureHeader = "sha256=" + hexBuilder;

            // Build and send HTTP POST
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Signature-256", signatureHeader);

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
