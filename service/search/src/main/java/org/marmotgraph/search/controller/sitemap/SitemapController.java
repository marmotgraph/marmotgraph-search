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

package org.marmotgraph.search.controller.sitemap;

import org.marmotgraph.search.common.controller.translation.models.TranslatorModel;
import org.marmotgraph.search.common.model.DataStage;
import org.marmotgraph.search.common.model.elasticsearch.Document;
import org.marmotgraph.search.common.services.ESServiceClient;
import org.marmotgraph.search.common.utils.ESHelper;
import org.marmotgraph.search.common.utils.translation.TranslatorRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SitemapController {

    private final ESServiceClient esServiceClient;
    private final ESHelper esHelper;
    private final TranslatorRegistry translatorRegistry;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public SitemapController(ESServiceClient esServiceClient, ESHelper esHelper, TranslatorRegistry translatorRegistry) {
        this.esServiceClient = esServiceClient;
        this.esHelper = esHelper;
        this.translatorRegistry = translatorRegistry;
    }

    public boolean isInAdminRole() {
        return false;

//        return userInfoRoles.isInAnyOfRoles("kg-cron", "admin");
    }

    /**
     * @return the ids of the instances to be listed in the sitemap or null if there are none. We only cache the ids
     * since the absolute URLs depend on the host the sitemap is requested from.
     */
    @Cacheable(value = "sitemap", unless = "#result == null")
    public List<String> getSitemapInstanceIds() {
        return fetchSitemapInstanceIds();
    }


    private List<String> fetchSitemapInstanceIds() {
        List<String> ids = new ArrayList<>();
        String index = esHelper.getIndexesForDocument(DataStage.RELEASED);
        final Set<String> relevantTypes = translatorRegistry.getTranslators().stream().filter(TranslatorModel::addToSitemap).map(TranslatorModel::category).filter(Objects::nonNull).collect(Collectors.toSet());
        try {
            List<Document> documents = esServiceClient.getDocumentsForSitemap(index, relevantTypes);
            documents.forEach(doc -> ids.add(doc.getId()));
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                throw e;
            }
        }
        if (ids.isEmpty()) {
            return null;
        }
        return ids;
    }

    @CachePut(value = "sitemap", unless = "#result == null")
    public List<String> updateSitemapCache() {
        return fetchSitemapInstanceIds();
    }

}
