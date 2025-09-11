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

import java.util.HashSet;
import java.util.Set;

import com.rdkm.tdkservice.enums.Category;
import com.rdkm.tdkservice.enums.TestGroup;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity representing a module.
 */
@EqualsAndHashCode(callSuper = true, exclude = "functions")
@Data
@Entity
@Table(name = "module")
public class Module extends BaseEntity {

	/**
	 * The name of the module.
	 */
	@Column(name = "name", unique = true, nullable = false)
	private String name;

	/**
	 * The test group associated with the module.
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TestGroup testGroup;

	/**
	 * The execution time of the module.
	 */
	@Column(name = "execution_time")
	private Integer executionTime;

	/**
	 * The user group associated with the module.
	 */
	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name = "user_group_id")
	private UserGroup userGroup;

	/**
	 * The set of log file paths associated with the module.
	 */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "module_log_file_paths", joinColumns = @JoinColumn(name = "module_id"))
	@Column(name = "log_file_path")
	private Set<String> logFileNames;

	/**
	 * The set of crash log file paths associated with the module.
	 */
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "module_crash_log_paths", joinColumns = @JoinColumn(name = "module_id"))
	@Column(name = "crash_log_path")
	private Set<String> crashLogFiles;

	/**
	 * The category of the module.
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Category category;

	/**
	 * The functions associated with the module.
	 */
	@OneToMany(mappedBy = "module", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<Function> functions = new HashSet<>();
}