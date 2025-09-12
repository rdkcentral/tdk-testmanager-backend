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

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The PrimitiveTest class to store primitive test details.
 */
@Data
@Entity
@EqualsAndHashCode(callSuper = true)
@Table(name = "primitive_test")
public class PrimitiveTest extends BaseEntity {

	/**
	 * The primitive test name.
	 */
	@Column(name = "name", nullable = false, unique = true)
	private String name;

	/**
	 * The primitive test Module
	 */
	@ManyToOne(optional = false)
	@JoinColumn(name = "module_id", nullable = false)
	private Module module;

	/**
	 * The primitive test function
	 */
	@ManyToOne(optional = false)
	@JoinColumn(name = "function_id", nullable = false)
	private Function function;

	/**
	 * The primitive test user group
	 */
	@ManyToOne
	@JoinColumn(name = "userGroup_id")
	private UserGroup userGroup;

	/**
	 * The primitive test parameters list
	 */

	@OneToMany(mappedBy = "primitiveTest", cascade = CascadeType.ALL)
	private List<PrimitiveTestParameter> parameters;

}
