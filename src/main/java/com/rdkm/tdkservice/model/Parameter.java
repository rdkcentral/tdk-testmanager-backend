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

import com.rdkm.tdkservice.enums.ParameterDataType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity representing a parameter type.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "parameter")
public class Parameter extends BaseEntity {

	/**
	 * The name of the parameter type.
	 */
	@Column(nullable = false)
	private String name;

	/**
	 * The enumeration of the parameter type.
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ParameterDataType parameterDataType;

	/**
	 * The range value of the parameter type.
	 */
	@Column(nullable = false)
	private String rangeVal;

	/**
	 * The function associated with the parameter type.
	 */
	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name = "function_id", nullable = false)
	private Function function;

	/**
	 * Returns a string representation of the parameter type.
	 *
	 * @return the name of the parameter type, or "NULL" if the name is null
	 */
	@Override
	public String toString() {
		return name != null ? name : "NULL";
	}
}