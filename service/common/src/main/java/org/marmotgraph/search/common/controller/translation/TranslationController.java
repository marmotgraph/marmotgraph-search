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

package org.marmotgraph.search.common.controller.translation;

import lombok.AllArgsConstructor;
import org.marmotgraph.search.common.controller.kg.KG;
import org.marmotgraph.search.common.controller.translation.models.Stats;
import org.marmotgraph.search.common.controller.translation.models.TargetInstancesResult;
import org.marmotgraph.search.common.controller.translation.models.Translator;
import org.marmotgraph.search.common.controller.translation.models.TranslatorModel;
import org.marmotgraph.search.common.model.DataStage;
import org.marmotgraph.search.common.model.ErrorReport;
import org.marmotgraph.search.common.model.source.ResultsOfKG;
import org.marmotgraph.search.common.model.source.SourceInstance;
import org.marmotgraph.search.common.model.target.TargetInstance;
import org.marmotgraph.search.common.services.DOICitationFormatter;
import org.marmotgraph.search.common.services.ESServiceClient;
import org.marmotgraph.search.common.utils.ESHelper;
import org.marmotgraph.search.common.utils.IdUtils;
import org.marmotgraph.search.common.utils.TranslationException;
import org.marmotgraph.search.common.utils.TranslatorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.marmotgraph.search.common.controller.translation.utils.TranslationUtils.getStats;

@AllArgsConstructor
@Component
public class TranslationController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final DOICitationFormatter doiCitationFormatter;
    private final ESServiceClient esServiceClient;
    private final ESHelper esHelper;

    public TargetInstancesResult translateToTargetInstances(KG kg, TranslatorModel translatorModel, String queryId, DataStage dataStage, int from, int size, Integer trendingThreshold, Map<String, Object> translationContext) {
        Translator<? extends SourceInstance, ? extends TargetInstance> translator = translatorModel.translator();
        String semanticType = translatorModel.semanticType(queryId);
        logger.info("Starting to query {} {} from {}", size, semanticType, from);
        final ResultsOfKG<? extends SourceInstance> instanceResults = kg.executeQuery(translatorModel.sourceClass(), dataStage, queryId, semanticType, from, size);
        TargetInstancesResult result = new TargetInstancesResult();
        if (instanceResults == null) {
            logger.info("Was not able to read results for {} from index {} of size {}", semanticType, from, size);
            result.setTargetInstances(Collections.emptyList());
            result.setFrom(from);
            result.setSize(size);
        } else {
            Stats stats = getStats(instanceResults, from);
            logger.info("Queried {} {} ({})", stats.getPageSize(), translatorModel.sourceClass().getSimpleName(), stats.getInfo());
            if(instanceResults.getErrors() ==null){
                instanceResults.setErrors(new ErrorReport());
            }
            List<TargetInstance> instances = instanceResults.getData().stream().filter(Objects::nonNull).map(s -> {
                try {
                    List<String> errors = new ArrayList<>();
                    final TargetInstance r = translator.translate(s, dataStage, translatorModel.category(), translatorModel.targetClass(), false, new TranslatorUtils(doiCitationFormatter, esServiceClient, trendingThreshold, translationContext, errors, esHelper));
                    if(!CollectionUtils.isEmpty(errors) && r != null) {
                        String id = IdUtils.getUUID(r.getId());
                        if (instanceResults.getErrors().get(id) != null) {
                            instanceResults.getErrors().get(id).addAll(errors);
                        } else {
                            instanceResults.getErrors().put(id, errors);
                        }
                    }
                    return r;
                } catch (TranslationException e) {
                    String id = IdUtils.getUUID(e.getIdentifier());
                    List<String> errors = instanceResults.getErrors().computeIfAbsent(id, k -> new ArrayList<>());
                    errors.add(e.getMessage());
                    return null;
                } catch (Exception e) {
                    String id =  IdUtils.getUUID(s.getId());
                    List<String> errors = instanceResults.getErrors().computeIfAbsent(id, k -> new ArrayList<>());
                    errors.add(String.format("Unexpected exception: %s", e.getMessage()));
                    logger.error(String.format("Unexpected exception for instance %s in translation", id), e);
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
            result.setTargetInstances(instances);
            result.setFrom(instanceResults.getFrom());
            result.setSize(instanceResults.getSize());
            result.setTotal(instanceResults.getTotal());
            if (instanceResults.getErrors() != null && !instanceResults.getErrors().isEmpty()) {
                result.setErrors(instanceResults.getErrors());
            }
        }
        return result;
    }


    public TargetInstance translateToTargetInstanceForLiveMode(KG kg, TranslatorModel translatorModel, String queryId, DataStage dataStage, String id) throws TranslationException {
        String semanticType = translatorModel.semanticType(queryId);
        logger.info("Starting to query id {} from {} for live mode", id, translatorModel.sourceClass().getSimpleName());
        SourceInstance source = kg.executeQueryForInstance(translatorModel.sourceClass(), dataStage, queryId, semanticType, id, false);
        logger.info("Done querying id {} from {} for live mode", id, translatorModel.sourceClass().getSimpleName());
        if (source == null) {
            return null;
        }
        Translator<? extends SourceInstance, ? extends TargetInstance> translator = translatorModel.translator();
        return translator.translate(source, dataStage, translatorModel.category(), translatorModel.targetClass(), true, new TranslatorUtils(doiCitationFormatter, esServiceClient, null, Collections.emptyMap(), null, esHelper));
    }


}
