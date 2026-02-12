package com.rdkm.tdkci.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.rdkm.tdkci.dto.ExecutionDTO;
import com.rdkm.tdkci.response.Response;

@Service
public class ExecutionServiceRegistery {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecutionServiceRegistery.class);

	@Autowired
	@Qualifier("automatics")
	private IExecutionService automaticsExecutionService;

	@Autowired
	@Qualifier("tdk")
	private IExecutionService tdkExecutionService;

	public IExecutionService getService(String appType) {
		LOGGER.info("Fetching execution service for appType: {}", appType);
		if (appType == null) {
			return tdkExecutionService;
		}
		switch (appType.toLowerCase()) {
		case "automatics":
			return automaticsExecutionService;
		case "tdk":
			return tdkExecutionService;
		default:
			return tdkExecutionService;
		}
	}

	public Response upgradeDeviceAndTriggerCIExecution(ExecutionDTO execTriggerDTO) {
		LOGGER.info("Starting upgrade and CI execution for buildName: {}", execTriggerDTO.getBuildName());
		IExecutionService service = getService(execTriggerDTO.getAppType());
		return service.upgradeDeviceAndTriggerCIExecution(execTriggerDTO);
	}

}
