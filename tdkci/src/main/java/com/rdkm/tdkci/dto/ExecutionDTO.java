package com.rdkm.tdkci.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ExecutionDTO {

	@NotNull(message = "BuildName cannot be null")
	private String buildName;

	@NotNull(message = "Category cannot be null")
	private String category;

	private String appType;

	@NotNull(message = "DeviceName cannot be null")
	private String deviceName;

}
