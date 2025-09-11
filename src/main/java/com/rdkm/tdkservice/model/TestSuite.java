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

import java.util.ArrayList;
import java.util.List;

import com.rdkm.tdkservice.enums.Category;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The TestSuiteclass is used to store test suite information or data.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "test_suite")
public class TestSuite extends BaseEntity {

	/**
	 * The name of the test suite.
	 */
	@Column(nullable = false, unique = true)
	private String name;

	/*
	 * The description for test suite
	 */
	@Column
	private String description;

	/**
	 * The description of the test suite.
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Category category;

	/**
	 * The user group of the test suite.
	 */
	@ManyToOne
	@JoinColumn(name = "user_group_id")
	private UserGroup userGroup;

	/**
	 * The scripts of the test suite.
	 */
	@OneToMany(mappedBy = "testSuite", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("scriptOrder ASC")
	private List<ScriptTestSuite> scriptTestSuite = new ArrayList<>();

}
