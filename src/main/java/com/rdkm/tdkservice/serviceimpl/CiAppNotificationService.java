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

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rdkm.tdkservice.config.AppConfig;
import com.rdkm.tdkservice.dto.DeviceFreeNotificationDTO;
import com.rdkm.tdkservice.enums.DeviceStatus;
import com.rdkm.tdkservice.model.Device;
import com.rdkm.tdkservice.model.Execution;
import com.rdkm.tdkservice.repository.DeviceRepositroy;
import com.rdkm.tdkservice.repository.ExecutionRepository;
import com.rdkm.tdkservice.service.utilservices.CommonService;
import com.rdkm.tdkservice.util.Constants;

/**
 * Sends a "device free" notification to the internal CI application after an
 * execution completes and the device is genuinely released (status == FREE),
 * so the CI app can dequeue and trigger the next pending execution.
 *
 * <p>
 * The notification is intentionally suppressed when the device is in any
 * non-FREE state (NOT_FOUND, HANG, BUSY, etc.) which happens when an execution
 * is paused mid-run because the device went down. In that case the CI app must
 * NOT pick up the next queued job — it waits until the execution is resumed and
 * fully completed, at which point the device returns to FREE and this service
 * will fire.
 * </p>
 */
@Service
public class CiAppNotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CiAppNotificationService.class);

    @Autowired
    private HttpService httpService;

    @Autowired
    private CommonService commonService;

    @Autowired
    private DeviceRepositroy deviceRepository;

    @Autowired
    private ExecutionRepository executionRepository;

    /**
     * Notifies the internal CI application that the given device is now free,
     * but only when the device's persisted status is {@link DeviceStatus#FREE}.
     *
     * <p>
     * Call this method <em>after</em>
     * {@code DeviceStatusService.fetchAndUpdateDeviceStatus()}
     * so the DB already holds the latest status.
     * </p>
     *
     * <ul>
     * <li>Execution completed normally → device is FREE → notification sent.</li>
     * <li>Device went down mid-run, execution PAUSED → device is NOT_FOUND/HANG →
     * skipped.</li>
     * <li>Execution restarted after device recovery and completed → device is FREE
     * → notification sent.</li>
     * <li>Execution aborted by user → device returns to FREE → notification
     * sent.</li>
     * </ul>
     *
     * If {@code ci_app_notify_url} is not configured in tm.config the call is
     * silently skipped so the execution flow is never disrupted.
     *
     * @param device        the device whose status was just refreshed in the DB
     * @param executionName the name of the execution that completed
     */
    public void notifyDeviceFree(Device device, String executionName) {

        // Re-read the device from DB to get the status that
        // fetchAndUpdateDeviceStatus()
        // just persisted — the in-memory 'device' object is NOT updated by that call.
        Device latestDevice = deviceRepository.findByName(device.getName());
        if (latestDevice == null) {
            LOGGER.warn("Device {} not found in DB – skipping CI app notification", device.getName());
            return;
        }

        DeviceStatus currentStatus = latestDevice.getDeviceStatus();
        if (currentStatus != DeviceStatus.FREE) {
            LOGGER.info("Skipping CI app notification for device: {} – status is {} (not FREE). "
                    + "Execution may be paused due to device going down.",
                    device.getName(), currentStatus);
            return;
        }

        String notifyUrl = getNotifyUrl();
        if (notifyUrl == null || notifyUrl.isBlank()) {
            LOGGER.debug("ci_app_notify_url not configured – skipping CI app notification for device: {}",
                    device.getName());
            return;
        }

        // Look up the execution to get the ciJobId and overall result for the CI app
        Execution execution = executionRepository.findByName(executionName);
        String ciJobId = (execution != null) ? execution.getCiJobId() : null;

        // Non-CI executions have no ciJobId – no point notifying the CI app
        if (ciJobId == null || ciJobId.isBlank()) {
            LOGGER.debug("Execution '{}' has no ciJobId – not a CI-triggered execution, skipping CI app notification",
                    executionName);
            return;
        }

        String overallResult = (execution != null && execution.getResult() != null)
                ? execution.getResult().name()
                : "UNKNOWN";
        String executionStatus = (execution != null && execution.getExecutionStatus() != null)
                ? execution.getExecutionStatus().name()
                : "UNKNOWN";

        DeviceFreeNotificationDTO payload = new DeviceFreeNotificationDTO(
                ciJobId,
                device.getName(),
                overallResult,
                executionStatus);

        try {
            LOGGER.info("Notifying CI app at [{}] – device: {} is FREE [ciJobId: {}, result: {}]",
                    notifyUrl, device.getName(), ciJobId, overallResult);
            httpService.sendCIAppNotification(notifyUrl, payload, null);
            LOGGER.info("CI app notified successfully for device: {}", device.getName());
        } catch (Exception e) {
            LOGGER.error("Failed to notify CI app for device: {} – error: {}", device.getName(), e.getMessage());
        }
    }

    private String getNotifyUrl() {
        String configFilePath = AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + Constants.TM_CONFIG_FILE;
        String baseUrl = commonService.getConfigProperty(new File(configFilePath), Constants.CI_APP_NOTIFY_URL);
        if (baseUrl == null || baseUrl.isBlank()) {
            return null;
        }
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base + Constants.CI_APP_NOTIFY_ENDPOINT;
    }

}
