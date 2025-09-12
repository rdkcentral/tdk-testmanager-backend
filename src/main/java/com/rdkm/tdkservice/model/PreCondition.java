/*
* If not stated otherwise in this file or this component's Licenses.txt file the
* following copyright and licenses apply:
*
* Copyright 2025 RDK Management
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
package com.rdkm.tdkservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity class representing a PreCondition in the system.
 */

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class PreCondition extends BaseEntity {

	/**
	 * The name of the PreCondition.
	 */
	@Column(nullable = false, columnDefinition = "TEXT")
	private String preConditionDescription;

	/**
	 * The script associated with this PreCondition.
	 */
	@ManyToOne
	@JoinColumn(name = "script_Id")
	private Script script;

}
