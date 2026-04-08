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

package org.marmotgraph.search.indexing.controller.indexing;

import org.marmotgraph.search.common.controller.kg.KG;
import org.marmotgraph.search.common.controller.translation.TranslationController;
import org.marmotgraph.search.common.controller.translation.models.TargetInstancesResult;
import org.marmotgraph.search.common.controller.translation.models.Translator;
import org.marmotgraph.search.common.controller.translation.models.TranslatorModel;
import org.marmotgraph.search.common.controller.translation.utils.ReferenceResolver;
import org.marmotgraph.search.common.model.DataStage;
import org.marmotgraph.search.common.model.ErrorReport;
import org.marmotgraph.search.common.model.ErrorReportResult;
import org.marmotgraph.search.common.model.source.SourceInstance;
import org.marmotgraph.search.common.model.target.HasBadges;
import org.marmotgraph.search.common.model.target.TargetInstance;
import org.marmotgraph.search.common.services.ESServiceClient;
import org.marmotgraph.search.common.utils.ESHelper;
import org.marmotgraph.search.common.utils.TranslatorUtils;
import org.marmotgraph.search.indexing.controller.elasticsearch.ElasticSearchController;
import org.marmotgraph.search.indexing.controller.mapping.MappingController;
import org.marmotgraph.search.indexing.controller.metrics.MetricsController;
import org.marmotgraph.search.indexing.controller.settings.SettingsController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class IndexingController {

    private final MappingController mappingController;
    private final MetricsController metricsController;
    private final SettingsController settingsController;
    private final ElasticSearchController elasticSearchController;

    private final ESServiceClient esServiceClient;
    private final ESHelper esHelper;
    private final TranslationController translationController;

    private final ReferenceResolver referenceResolver;

    private final KG kgV3;

    private final static Logger logger = LoggerFactory.getLogger(IndexingController.class);

    public IndexingController(MappingController mappingController, MetricsController metricsController, SettingsController settingsController, ElasticSearchController elasticSearchController, TranslationController translationController, KG kgV3, ESServiceClient esServiceClient, ESHelper esHelper, ReferenceResolver referenceResolver) {
        this.mappingController = mappingController;
        this.metricsController = metricsController;
        this.settingsController = settingsController;
        this.elasticSearchController = elasticSearchController;
        this.translationController = translationController;
        this.esServiceClient = esServiceClient;
        this.esHelper = esHelper;
        this.referenceResolver = referenceResolver;
        this.kgV3 = kgV3;
    }

    public ErrorReportResult.ErrorReportResultByTargetType populateIndex(TranslatorModel translatorModel, DataStage dataStage, boolean temporary) {
        ErrorReportResult.ErrorReportResultByTargetType errorReportByTargetType =  null;
        Set<String> searchableIds = new HashSet<>();
        Set<String> nonSearchableIds = new HashSet<>();
        if (translatorModel.translator() != null) {
            final UpdateResult updateResult = update(
                    kgV3,
                    translatorModel,
                    dataStage,
                    Collections.emptySet(),
                    instance -> instance,
                    temporary);
            if (!updateResult.errors.isEmpty()) {
                errorReportByTargetType = new ErrorReportResult.ErrorReportResultByTargetType();
                errorReportByTargetType.setTargetType(translatorModel.targetClass().getSimpleName());
                errorReportByTargetType.setErrors(updateResult.errors);
            }
            searchableIds.addAll(updateResult.searchableIds);
            nonSearchableIds.addAll(updateResult.nonSearchableIds);
            if(!updateResult.badges.isEmpty()) {
                kgV3.persistBadges(translatorModel.targetClass().getSimpleName(), updateResult.badges);
            }
        }
        if (translatorModel.autoRelease()) {
            elasticSearchController.removeDeprecatedDocumentsFromAutoReleasedIndex(translatorModel.targetClass(), dataStage, nonSearchableIds, temporary);
        } else {
            elasticSearchController.removeDeprecatedDocumentsFromSearchIndex(translatorModel.targetClass(), dataStage, searchableIds, temporary);
            elasticSearchController.removeDeprecatedDocumentsFromIdentifiersIndex(translatorModel.targetClass(), dataStage, nonSearchableIds);
        }
        return errorReportByTargetType;
    }

    private static class UpdateResult {
        private final Set<String> searchableIds = new HashSet<>();
        private final Set<String> nonSearchableIds = new HashSet<>();
        private final ErrorReport errors = new ErrorReport();

        private final Map<String, Object> badges = new HashMap<>();
    }


    private static final List<String> relevantBadges = Arrays.asList(TranslatorUtils.IS_NEW_BADGE, TranslatorUtils.IS_TRENDING_BADGE);

    private <Target extends TargetInstance> UpdateResult update(KG kg, TranslatorModel translatorModel, DataStage dataStage, Set<String> excludedIds, Function<Target, Target> instanceHandler, boolean temporary) {
        UpdateResult updateResult = new UpdateResult();
        Translator<? extends SourceInstance, ? extends TargetInstance> translator = translatorModel.translator();
        final Map<String, Object> translationContext = translator.populateTranslationContext(esServiceClient, esHelper, dataStage);
        final Integer trendThreshold = metricsController.getTrendThreshold(translatorModel.targetClass(), dataStage);
        final Set<String> existingIdentifiers = referenceResolver.loadAllExistingIdentifiers(dataStage);
        translatorModel.queryIds().forEach(queryId -> {
            Integer lastTotal = null;
            boolean hasMore = true;
            int from = 0;
            while (hasMore) {
                TargetInstancesResult result = translationController.translateToTargetInstances(kg, translatorModel, queryId, dataStage, from, translatorModel.bulkSize(), trendThreshold, translationContext);
                if (result.getErrors() != null) {
                    updateResult.errors.putAll(result.getErrors());
                }
                List<TargetInstance> instances = result.getTargetInstances();
                if (instances != null) {
                    List<Target> searchableInstances = new ArrayList<>();
                    List<Target> nonSearchableInstances = new ArrayList<>();
                    final List<Target> processableInstances = instances.stream().filter(instance -> !excludedIds.contains(instance.getId())).map(instance -> (Target)instance).collect(Collectors.toList());
                    referenceResolver.clearNonResolvableReferences(processableInstances, existingIdentifiers);
                    processableInstances.forEach(instance -> {
                        logger.debug("Translating instance {}", instance.getId());
                        Target handledInstance = instanceHandler != null ? instanceHandler.apply(instance) : instance;
                        if (handledInstance.isSearchableInstance()) {
                            updateResult.searchableIds.add(handledInstance.getId());
                            searchableInstances.add(handledInstance);
                        } else {
                            updateResult.nonSearchableIds.add(handledInstance.getId());
                            nonSearchableInstances.add(handledInstance);
                        }
                        if(handledInstance instanceof HasBadges){
                            final List<String> badges = ((HasBadges) handledInstance).getBadges();
                            if(badges != null){
                                badges.stream().filter(relevantBadges::contains).forEach(badge -> {
                                    String qualifiedProperty = String.format("https://search.kg.ebrains.eu/vocab/badges/%s", badge);
                                    updateResult.badges.computeIfAbsent(qualifiedProperty, k -> new ArrayList<>());
                                    ((List)updateResult.badges.get(qualifiedProperty)).add(Map.of("@id", String.format("https://kg.ebrains.eu/api/instances/%s", handledInstance.getId())));
                                });
                            }
                        }
                    });
                    if (!CollectionUtils.isEmpty(searchableInstances)) {
                        elasticSearchController.updateSearchIndex(searchableInstances, translatorModel.targetClass(), dataStage, temporary);
                    }
                    if (!CollectionUtils.isEmpty(nonSearchableInstances)) {
                        if (translatorModel.autoRelease()) {
                            elasticSearchController.updateAutoReleasedIndex(nonSearchableInstances, dataStage, translatorModel.targetClass(), temporary);
                        } else {
                            elasticSearchController.updateIdentifiersIndex(nonSearchableInstances, dataStage);
                        }
                    }
                }
                if (result.getTotal() != null) {
                    lastTotal = result.getTotal();
                }
                from = result.getFrom() + result.getSize();
                hasMore = lastTotal != null && from < lastTotal;
            }
        });
        return updateResult;
    }

    public void recreateIdentifiersIndex(DataStage dataStage) {
        Map<String, Object> mapping = mappingController.generateIdentifierMapping();
        Map<String, Object> payload = Map.of("mappings", mapping);
        elasticSearchController.recreateIdentifiersIndex(payload, dataStage);
    }


    public void reindexTemporaryToReal(DataStage dataStage, Class<? extends TargetInstance> clazz, boolean autorelease) {
        recreateIndex(dataStage, clazz, autorelease, false);
        elasticSearchController.reindexTemporaryToRealIndex(clazz, dataStage, autorelease);
    }

    public void recreateIndex(DataStage dataStage, Class<? extends TargetInstance> clazz, boolean autorelease, boolean temporary) {
        Map<String, Object> mapping = mappingController.generateMapping(clazz, !autorelease);
        if (autorelease) {
            Map<String, Object> payload = Map.of("mappings", mapping);
            elasticSearchController.recreateAutoReleasedIndex(dataStage, payload, clazz, temporary);
        } else {
            Map<String, Object> settings = settingsController.generateSearchIndexSettings();
            Map<String, Object> payload = Map.of(
                    "mappings", mapping,
                    "settings", settings);
            elasticSearchController.recreateSearchIndex(payload, clazz, dataStage, temporary);
        }
    }

    public void addResource(String id, Map<String, Object> resource) {
        elasticSearchController.addResource(id, resource);
    }

    public void deleteResource(String id) {
        elasticSearchController.deleteResource(id);
    }

}
