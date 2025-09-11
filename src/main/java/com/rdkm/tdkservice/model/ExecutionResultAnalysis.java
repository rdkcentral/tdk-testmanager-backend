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
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package com.rdkm.tdkservice.model;

import com.rdkm.tdkservice.enums.AnalysisDefectType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity representing the analysis of an execution result.
 * This class extends BaseEntity and uses Lombok annotations for boilerplate code generation.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class ExecutionResultAnalysis extends BaseEntity {

    /** User who performed the analysis */
    String analysisUser;

    /** Type of defect identified during the analysis */
    @Enumerated(EnumType.STRING)
    AnalysisDefectType analysisDefectType;

    /** Ticket ID associated with the analysis */
    String analysisTicketID;

    /** Remarks or comments about the analysis */
    String analysisRemark;

    /** The execution result that was analyzed */
    @OneToOne
    @NotNull
    private ExecutionResult executionResult;

}