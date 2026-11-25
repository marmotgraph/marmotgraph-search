/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2024 EBRAINS AISBL
 * Copyright 2024 - 2026 ETH Zurich
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  This open source software code was developed in part or in whole in the
 *  Human Brain Project, funded from the European Union's Horizon 2020
 *  Framework Programme for Research and Innovation under
 *  Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 *  (Human Brain Project SGA1, SGA2 and SGA3).
 */

package org.marmotgraph.search.common.utils.queryGenerator;

import org.junit.jupiter.api.Test;
import org.marmotgraph.sdk.model.QuerySpecification;
import org.marmotgraph.sdk.model.ScopeSpecification;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryGeneratorTest {

    private static final String NS = "https://schema/";

    private final QueryGenerator queryGenerator = new QueryGenerator();

    @Test
    void toScopeKeepsOnlyPathsLeadingToFurtherInstances() {
        QuerySpecification query = QuerySpecification.of(NS + "Dataset",
                QuerySpecification.StructureItem.of("name", QuerySpecification.Path.outgoing(NS + "name")),
                QuerySpecification.StructureItem.of("author", QuerySpecification.Path.outgoing(NS + "author", NS + "Person")).withStructure(
                        QuerySpecification.StructureItem.of("name", QuerySpecification.Path.outgoing(NS + "name")),
                        QuerySpecification.StructureItem.of("affiliation", QuerySpecification.Path.outgoing(NS + "affiliation")).withStructure(
                                QuerySpecification.StructureItem.of("name", QuerySpecification.Path.outgoing(NS + "name"))
                        )
                ),
                QuerySpecification.StructureItem.of("versions", QuerySpecification.Path.incoming(NS + "isVersionOf")).withStructure(
                        QuerySpecification.StructureItem.of("id", QuerySpecification.Path.outgoing("@id"))
                )
        );

        ScopeSpecification scope = queryGenerator.toScope(query);

        assertEquals(ScopeSpecification.of(
                ScopeSpecification.Step.of(ScopeSpecification.Path.outgoing(NS + "author", NS + "Person"),
                        ScopeSpecification.Step.of(ScopeSpecification.Path.outgoing(NS + "affiliation"))),
                ScopeSpecification.Step.of(ScopeSpecification.Path.incoming(NS + "isVersionOf"))
        ), scope);
    }

    @Test
    void stepsFollowingTheSamePathAreMerged() {
        QuerySpecification query = QuerySpecification.of(NS + "Dataset",
                QuerySpecification.StructureItem.of("authorNames", QuerySpecification.Path.outgoing(NS + "author")).withStructure(
                        QuerySpecification.StructureItem.of("name", QuerySpecification.Path.outgoing(NS + "name"))
                ),
                QuerySpecification.StructureItem.of("authorAffiliations", QuerySpecification.Path.outgoing(NS + "author")).withStructure(
                        QuerySpecification.StructureItem.of("affiliation", QuerySpecification.Path.outgoing(NS + "affiliation")).withStructure()
                )
        );
        ScopeSpecification other = ScopeSpecification.of(ScopeSpecification.Step.of(ScopeSpecification.Path.outgoing(NS + "custodian")));

        ScopeSpecification merged = QueryGenerator.mergeScopes(List.of(queryGenerator.toScope(query), other));

        assertEquals(ScopeSpecification.of(
                ScopeSpecification.Step.of(ScopeSpecification.Path.outgoing(NS + "author"),
                        ScopeSpecification.Step.of(ScopeSpecification.Path.outgoing(NS + "affiliation"))),
                ScopeSpecification.Step.of(ScopeSpecification.Path.outgoing(NS + "custodian"))
        ), merged);
    }
}
