package com.rdkm.tdkservice.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScriptDetailsResponse {

	/**
	 * The id of the Script.
	 */
	UUID id;

	/**
	 * The name of the Script.
	 */
	String scriptName;
}
