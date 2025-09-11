/*
 * If not stated otherwise in this file or this component's Licenses.txt file the
 * following copyright and licenses apply:
 *
 * Copyright 2024 RDK Management
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.rdkm.tdkservice.service.utilservices;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.rdkm.tdkservice.config.AppConfig;
import com.rdkm.tdkservice.util.Constants;

/**
 * The PythonFrameworkScriptExecutorService class provides the methods to
 * execute Python framework related scripts eg : callResetAgent.py etc and other
 * framework Python scripts.
 */
@Service
public class PythonLibraryScriptExecutorService {

	@Autowired
	private CommonService commonService;

	@Autowired
	private ScriptExecutorService scriptExecutorService;

	public static final Logger LOGGER = LoggerFactory.getLogger(PythonLibraryScriptExecutorService.class);

	/**
	 * This method runs the resetAgent Script. This library uses “resetAgent” python
	 * library to reset the TDK application running in the DUT.
	 * 
	 * @param deviceIP   - IP of the device
	 * @param devicePort - TDK Agent port number
	 * @param enableReset - true - To restart agent, false - To reset device state to FREE
	 */
	public void resetAgentForTDKDevices(String deviceIP, String devicePort , String enableReset) {
		try {
			LOGGER.info("Going to run resetAgent Script for device: {} with port: {}", deviceIP, devicePort);
			File resetAgentScriptFile = new File(
					AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR + Constants.REST_AGENT_SCRIPT);
			String resetAgentScriptFilePath = resetAgentScriptFile.getAbsolutePath();
			String[] cmd = new String[] { commonService.getPythonCommandFromConfig(), resetAgentScriptFilePath,
					deviceIP, devicePort, enableReset };
			String outputOfScriptExecution = scriptExecutorService.executeScript(cmd, 30);
			this.callRebootOnAgentResetFailure(outputOfScriptExecution, deviceIP, devicePort);

		} catch (Exception e) {
			LOGGER.error("Error running resetAgent Script: {}", e.getMessage(), e);
		}

	}

	/**
	 * Method to check whether the agent reset failed. If the agent reset failed it
	 * will request to reboot the box.
	 * 
	 * @param output
	 * @param device
	 * @return
	 */
	private void callRebootOnAgentResetFailure(String outputOfResetAgent, String deviceIP, String devicePort) {
		if (outputOfResetAgent.contains("Failed to reset agent")
				|| outputOfResetAgent.contains("Unable to reach agent")) {
			LOGGER.info("Agent reset failed for device: {} with port: {}, So going with reboot of the device", deviceIP,
					devicePort);
			rebootDevice(deviceIP, devicePort);
		}
	}

	/**
	 * Method to reboot the TDK box
	 * 
	 * @param deviceIP   - IP of the device
	 * @param devicePort - TDK Agent port number
	 */
	public void rebootDevice(String deviceIP, String devicePort) {
		try {
			File rebootScriptFile = new File(AppConfig.getBaselocation() + Constants.FILE_PATH_SEPERATOR
					+ Constants.REBOOT_ON_CRASH_SCRIPT_FILE);
			String rebootScriptFilePath = rebootScriptFile.getAbsolutePath();
			String[] cmd = new String[] { commonService.getPythonCommandFromConfig(), rebootScriptFilePath, deviceIP,
					devicePort };
			String outputOfScriptExecution = scriptExecutorService.executeScript(cmd, 30);
			Thread.sleep(15000);
			LOGGER.info("Output of rebootDevice Script: {}", outputOfScriptExecution);
		} catch (Exception e) {
			LOGGER.error("Error running rebootDevice Script: {}", e.getMessage(), e);
		}

	}

}
