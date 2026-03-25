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

package org.marmotgraph.search.common.controller.translation.models;

import org.marmotgraph.search.common.model.source.SourceInstance;
import org.marmotgraph.search.common.model.target.TargetInstance;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record TranslatorModel(Class<? extends SourceInstance> sourceClass, Class<? extends TargetInstance> targetClass, Translator<? extends SourceInstance, ? extends TargetInstance> translator, boolean autoRelease, int bulkSize, boolean addToSitemap, List<String> semanticTypes, String category, int orderNumber) {

    public List<String> queryIds(){
        return semanticTypes().stream().map(this::queryId).toList();
    }

    public String semanticType(String queryId){
        return semanticTypes.stream().filter(s -> queryId(s).equals(queryId)).findFirst().orElse(null);
    }

    public String queryId(String semanticType) {
        String queryId = translator.getClass().getCanonicalName() + "/" + semanticType;
        return UUID.nameUUIDFromBytes(queryId.getBytes()).toString();
    }

    public static Comparator<TranslatorModel> COMPARATOR = Comparator.<TranslatorModel>comparingInt(c -> c.orderNumber).thenComparing(c -> c.category);

}
