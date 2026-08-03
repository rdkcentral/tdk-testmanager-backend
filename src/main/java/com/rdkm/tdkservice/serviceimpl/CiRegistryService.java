/*
* If not stated otherwise in this file or this component's LICENSE file the
* following copyright and licenses apply:
*
* Copyright 2024 RDK Management
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rdkm.tdkservice.config.AppConfig;
import com.rdkm.tdkservice.util.Constants;

/**
 * Resolves the per-CI-app HMAC secret from the {@code ci_app.*} registry
 * entries in {@code tm.config}.
 *
 * <p>
 * Registry format in tm.config:
 * 
 * <pre>
 *   ci_app.jenkins-prod.callbackUrl=http://jenkins-prod:8080/webhook/result
 *   ci_app.jenkins-prod.secret=secret-abc123
 * </pre>
 * 
 * Any number of named entries can be added. The name segment is arbitrary but
 * must be unique within the file.
 */
@Service
public class CiRegistryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CiRegistryService.class);

    /**
     * Returns the HMAC secret registered for the given {@code callbackUrl} by
     * scanning all {@code ci_app.*.callbackUrl} entries in tm.config, or
     * {@code null} if no matching entry is found.
     *
     * @param callbackUrl the callback URL to look up
     * @return the corresponding HMAC secret, or {@code null}
     */
    public String resolveSecretForCallbackUrl(String callbackUrl) {
        if (callbackUrl == null || callbackUrl.isBlank()) {
            return null;
        }

        String configFilePath = AppConfig.getBaselocation()
                + Constants.FILE_PATH_SEPERATOR
                + Constants.TM_CONFIG_FILE;

        Properties props = loadProperties(configFilePath);
        if (props == null) {
            return null;
        }

        for (String key : props.stringPropertyNames()) {
            if (key.startsWith(Constants.CI_APP_REGISTRY_PREFIX)
                    && key.endsWith(Constants.CI_APP_REGISTRY_CALLBACK_URL_SUFFIX)) {

                String registeredUrl = props.getProperty(key);
                if (callbackUrl.equals(registeredUrl)) {
                    // Derive secret key: ci_app.<name>.callbackUrl → ci_app.<name>.secret
                    String secretKey = key.substring(
                            0,
                            key.length() - Constants.CI_APP_REGISTRY_CALLBACK_URL_SUFFIX.length())
                            + Constants.CI_APP_REGISTRY_SECRET_SUFFIX;

                    String secret = props.getProperty(secretKey);
                    if (secret == null || secret.isBlank()) {
                        LOGGER.warn("Registry entry found for URL [{}] but secret is not configured (key={})",
                                callbackUrl, secretKey);
                    }
                    return secret;
                }
            }
        }

        LOGGER.warn("No registry entry found for callbackUrl: {} – HMAC signing will be skipped", callbackUrl);
        return null;
    }

    private Properties loadProperties(String filePath) {
        try (InputStream is = new FileInputStream(new File(filePath))) {
            Properties props = new Properties();
            props.load(is);
            return props;
        } catch (IOException e) {
            LOGGER.error("Failed to load config file at: {}", filePath, e);
            return null;
        }
    }
}
