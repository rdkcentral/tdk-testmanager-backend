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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity representing a function.
 */
@EqualsAndHashCode(callSuper = true, exclude = "parameters")
@Data
@Entity
@Table(name = "functions")
public class Function extends BaseEntity {

	/**
	 * The name of the function.
	 */
	@Column(nullable = false, unique = true)
	private String name;

	/**
	 * The module to which the function belongs.
	 */
	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name = "module_id", nullable = false)
	private Module module;

	/**
	 * The category of the function.
	 */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Category category = Category.RDKV;

	/**
	 * The parameters associated with the function.
	 */
	@OneToMany(mappedBy = "function", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<Parameter> parameters = new HashSet<>();
}