package com.rdkm.tdkci.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceStatusWrapperDTO {
	
	private String message;
    private int statusCode;
    private DeviceStatusResponseDTO data;

}
