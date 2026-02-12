/*
* If not stated otherwise in this file or this component's LICENSE file the
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
package com.rdkm.tdkci.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity representing Xconf configuration details.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "xconf_config")
public class XconfConfig extends BaseEntity {

	/**
	 * The unique and non-null name of the configuration.
	 * This field is mapped to a database column with constraints:
	 * - Cannot be null.
	 * - Must be unique across all records.
	 */
	@Column(nullable = false, unique = true)
	private String name;

	/**
	 * Unique identifier for the Xconf configuration.
	 * <p>
	 * This field is mapped to a database column that is non-nullable and must be
	 * unique.
	 */
	@Column(nullable = false, unique = true)
	private String xconfigId;

	/**
	 * Description of the Xconf configuration.
	 * This field is mandatory and cannot be null.
	 */
	@Column(nullable = false)
	private String xconfigDescription;

	/**
	 * The name of the Xconf configuration.
	 * <p>
	 * This field is mapped to a database column that is non-nullable and must be
	 * unique.
	 */
	@Column(nullable = false, unique = true)
	private String xconfigName;

}
