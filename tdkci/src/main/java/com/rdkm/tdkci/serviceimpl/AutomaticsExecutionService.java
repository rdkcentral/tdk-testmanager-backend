package com.rdkm.tdkci.serviceimpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.rdkm.tdkci.dto.ExecutionDTO;
import com.rdkm.tdkci.response.Response;
import com.rdkm.tdkci.service.IExecutionService;

@Service
@Qualifier("automatics")
public class AutomaticsExecutionService implements IExecutionService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AutomaticsExecutionService.class);

	@Override
	public Response upgradeDeviceAndTriggerCIExecution(ExecutionDTO execTriggerDTO) {
		LOGGER.info("Executing Automatics upgrade and CI trigger for buildName: {}", execTriggerDTO.getBuildName());
		return null;
	}
}
