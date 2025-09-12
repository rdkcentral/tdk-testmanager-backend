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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The PrimitiveTestParameter class to store primitive test parameter details.
 */

@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@Table(name = "primitive_test_parameter")
public class PrimitiveTestParameter extends BaseEntity {

	/**
	 * The primitive test.
	 */
	@ManyToOne
	@JoinColumn(name = "primitivetest_id", nullable = false)
	private PrimitiveTest primitiveTest;

	/**
	 * The parameter name
	 */
	@Column(name = "parametername", nullable = false)
	private String parameterName;

	/**
	 * The parameter type
	 */
	@Column(name = "parametertype", nullable = false)
	private String parameterType;

	/*
	 * The parameter Range
	 */
	@Column(name = "parameterrange", nullable = false)
	private String parameterRange;

	/**
	 * The value.
	 */

	@Column(name = "parametervalue")
	private String parameterValue;

}
