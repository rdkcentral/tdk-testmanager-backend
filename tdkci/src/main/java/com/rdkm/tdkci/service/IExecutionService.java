package com.rdkm.tdkci.service;

import com.rdkm.tdkci.dto.ExecutionDTO;
import com.rdkm.tdkci.response.Response;

public interface IExecutionService {

	Response upgradeDeviceAndTriggerCIExecution(ExecutionDTO execTriggerDTO);

}
