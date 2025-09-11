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
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The Script class is used to store script information or data.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "script")
public class Script extends BaseEntity {

	/**
	 * The name of the script.
	 */
	@Column(nullable = false, unique = true)
	private String name;

	/**
	 * The description of the script.
	 */
	@Column(nullable = false, columnDefinition = "TEXT")
	private String synopsis;

	/**
	 * The execution timeout of the script.
	 */
	private int executionTimeOut;

	/**
	 * Is the script long duration type or not.
	 */
	private boolean isLongDuration = false;

	/**
	 * The category of the script.
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Category category;

	/**
	 * The primitive test of the script.
	 */
	@ManyToOne(cascade = CascadeType.PERSIST, optional = false)
	@JoinColumn(name = "primitive_test_id")
	PrimitiveTest primitiveTest;

	/**
	 * The list of device type of the script.
	 */
	@ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
	@JoinTable(name = "script_device_type", joinColumns = @JoinColumn(name = "script_id"), inverseJoinColumns = @JoinColumn(name = "device_type_id"))
	private List<DeviceType> deviceTypes = new ArrayList<>();

	/**
	 * Represents the userGroup of the script.
	 */
	@ManyToOne
	@JoinColumn(name = "user_group_id")
	private UserGroup userGroup;

	/**
	 * The module associated with the script.
	 */
	@ManyToOne
	@JoinColumn(name = "module_id", nullable = false)
	private Module module;

	/**
	 * true if script needs to be skipped while executing test suite
	 */
	boolean skipExecution = false;

	/**
	 * Short description about the reason for skipping the script
	 */
	String skipRemarks;

	/**
	 * The location of the script in the file system or storage.
	 */
	String scriptLocation;

	/**
	 * The testID of the script,say CT_Aamp_39
	 */
	@Column(nullable = false)
	private String testId;

	/**
	 * Objective of the test case
	 */
	@Column(nullable = false, columnDefinition = "TEXT")
	private String objective;

	/**
	 * Prerequisites for the testcase
	 */
	@OneToMany(mappedBy = "script",cascade = CascadeType.ALL,orphanRemoval = true)
	@OrderBy("createdDate ASC")
	private List<PreCondition> preConditions;

	/**
	 * Automation Approach or steps
	 */
	@OneToMany(mappedBy = "script",cascade = CascadeType.ALL,orphanRemoval = true)
	@OrderBy("createdDate ASC")
	private List<TestStep> testSteps;

	/**
	 * Priority of the test case
	 */
	@Column(nullable = false)
	private String priority;

	/**
	 * Release version of the test
	 */
	private String releaseVersion;

	/**
	 * The list of script group of the script.
	 */
	@OneToMany(mappedBy = "script", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private List<ScriptTestSuite> scriptScriptGroup = new ArrayList<>();

}
