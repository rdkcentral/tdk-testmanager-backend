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

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Model class for mapping script and test suite
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "script_test_suite")
public class ScriptTestSuite extends BaseEntity {

	/**
	 * The script of the test suite.
	 */
	@ManyToOne(optional = false)
	@JoinColumn(name = "script_id", nullable = false)
	private Script script;

	/**
	 * The test suite of the script.
	 */
	@ManyToOne(optional = false)
	@JoinColumn(name = "test_suite_id", nullable = false)
	private TestSuite testSuite;

	/**
	 * The order of the test suite.
	 */
	private Integer scriptOrder;

}
