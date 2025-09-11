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
package com.rdkm.tdkservice.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rdkm.tdkservice.model.PrimitiveTest;
import com.rdkm.tdkservice.model.PrimitiveTestParameter;

/*
 * The PrimitiveTestParameterRepository is a repository class to perform database operations on primitive test parameter.
 */
@Repository
public interface PrimitiveTestParameterRepository extends JpaRepository<PrimitiveTestParameter, UUID> {

	/*
	 * This method is used to get the primitive test parameter by primitive test.
	 * 
	 * @param primitivetTest
	 * 
	 * @return List<PrimitiveTestParameter>
	 */
	List<PrimitiveTestParameter> findByPrimitiveTest(PrimitiveTest primitivetTest);
	
	
	/**
	 * This method is used to get the primitive test parameter by created date or
	 * updated at.
	 * 
	 * @param createdDate
	 * @param updatedAt
	 * @return List<PrimitiveTestParameter>
	 */
	List<PrimitiveTestParameter> findByCreatedDateAfterOrUpdatedAtAfter(Instant createdDate, Instant updatedAt);

}
