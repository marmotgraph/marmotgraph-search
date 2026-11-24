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


package org.marmotgraph.search.common.services;

import lombok.Getter;
import lombok.Setter;
import org.marmotgraph.sdk.MarmotGraphApiException;
import org.marmotgraph.sdk.MarmotGraphClient;
import org.marmotgraph.sdk.MarmotGraphException;
import org.marmotgraph.sdk.model.*;
import org.marmotgraph.sdk.pagination.Page;
import org.marmotgraph.sdk.resources.InstanceListOptions;
import org.marmotgraph.sdk.resources.QueryRunOptions;
import org.marmotgraph.sdk.resources.ResponseOptions;
import org.marmotgraph.sdk.resources.TypeListOptions;
import org.marmotgraph.search.common.configuration.AppConfig;
import org.marmotgraph.search.common.configuration.Constants;
import org.marmotgraph.search.common.configuration.GracefulDeserializationProblemHandler;
import org.marmotgraph.search.common.model.DataStage;
import org.marmotgraph.search.common.model.source.ResultsOfKG;
import org.marmotgraph.search.common.utils.MetaModelUtils;
import org.marmotgraph.search.common.utils.queryGenerator.QueryGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class KGServiceClient {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final int MAX_RETRIES = 5;
    private static final int NOT_FOUND = 404;
    private static final int CONFLICT = 409;
    private static final String BEARER_PREFIX = "Bearer ";

    private final MarmotGraphClient serviceAccountClient;
    private final String serviceClientId;
    private final boolean developmentMode;
    private final AppConfig appConfig;
    private final QueryGenerator queryGenerator;

    /**
     * The SDK returns plain JSON-LD documents - we bind them to our source models with a graceful mapper so a single
     * unparseable value is reported instead of failing the whole page.
     */
    private final JsonMapper resultMapper = JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).addHandler(new GracefulDeserializationProblemHandler()).build();

    public KGServiceClient(MarmotGraphClient serviceAccountClient, @Value("${MARMOTGRAPH_CLIENT_ID}") String serviceClientId, @Value("${developmentMode:false}") boolean developmentMode, AppConfig appConfig, QueryGenerator queryGenerator) {
        this.serviceAccountClient = serviceAccountClient;
        this.serviceClientId = serviceClientId;
        this.developmentMode = developmentMode;
        this.appConfig = appConfig;
        this.queryGenerator = queryGenerator;
    }

    /**
     * A client acting on behalf of the user of the current request - we just reuse the original authorization header
     * of the request (without any client authorization to make the mechanism work with private spaces too).
     */
    private MarmotGraphClient userClient() {
        if (developmentMode) {
            return serviceAccountClient;
        }
        String authorization = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            authorization = attributes.getRequest().getHeader(Constants.AUTHORIZATION);
        }
        if (authorization != null && authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            authorization = authorization.substring(BEARER_PREFIX.length());
        }
        return serviceAccountClient.withToken(authorization);
    }

    private MarmotGraphClient client(boolean asServiceAccount) {
        return asServiceAccount ? serviceAccountClient : userClient();
    }

    private static Stage toStage(DataStage dataStage) {
        return Stage.valueOf(dataStage.name());
    }

    private static boolean isNotFound(MarmotGraphException e) {
        return e instanceof MarmotGraphApiException apiException && apiException.statusCode() == NOT_FOUND;
    }

    @Cacheable(value = "authEndpoint", unless = "#result == null")
    public String getAuthEndpoint() {
        try {
            AuthenticationConfig authenticationConfig = serviceAccountClient.setup().getAuthenticationConfig();
            return authenticationConfig != null ? authenticationConfig.endpoint() : null;
        } catch (MarmotGraphException e) {
            logger.error("Was not able to fetch the auth endpoint from KG", e);
        }
        return null;
    }

    /**
     * The ids of the instances the current user has been invited to - which are the instance level permissions.
     */
    public Set<UUID> getInvitationsFromKG() {
        UserWithRoles roles = userClient().users().myRoles();
        if (roles != null && roles.permissions() != null) {
            return roles.permissions().stream().map(FunctionalityInstance::id).filter(Objects::nonNull).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    public void addFavorite(UUID instanceId) {
        userClient().instances().addFavorite(instanceId);
    }

    public void removeFavorite(UUID instanceId) {
        userClient().instances().removeFavorite(instanceId);
    }

    public boolean isFavorite(UUID instanceId) {
        try {
            Instance instance = userClient().instances().get(instanceId, Stage.RELEASED, ResponseOptions.builder().returnPayload(false).returnFavorite(true).build());
            return instance != null && Boolean.TRUE.equals(instance.isFavorite());
        } catch (MarmotGraphException e) {
            if (isNotFound(e)) {
                return false;
            }
            throw e;
        }
    }

    public List<UUID> getFavoriteInstancesOfType(String type) {
        InstanceListOptions options = InstanceListOptions.builder()
                .stage(Stage.RELEASED)
                .types(type)
                .favoritesOnly(true)
                .returnTotalResults(false)
                .size(1000)
                .responseOptions(ResponseOptions.builder().returnPayload(false).build())
                .build();
        return userClient().instances().list(options).allItems().map(Instance::getUuid).flatMap(Optional::stream).toList();
    }

    /**
     * Executes the query for a single page - continuing from the given marker (null for the first page). The returned
     * result provides the marker for the next page (null if there are no more results).
     */
    public <T> ResultsOfKG<T> executeQueryForIndexing(Class<T> clazz, DataStage dataStage, String queryId, String semanticType, String marker, int size) {
        QueryRunOptions options = QueryRunOptions.builder()
                .stage(toStage(dataStage))
                .marker(marker)
                .size(size)
                .returnTotalResults(false)
                .build();
        return doExecuteCallForIndexing(clazz, queryId, semanticType, options, 0);
    }

    public <T> T executeQueryForInstance(Class<T> clazz, DataStage dataStage, String queryId, String id, String semanticType, boolean asServiceAccount) {
        UUID instanceId = MetaModelUtils.castToUUID(id);
        if (instanceId == null) {
            return null;
        }
        QueryRunOptions options = QueryRunOptions.builder()
                .stage(toStage(dataStage))
                .instanceId(instanceId)
                .returnTotalResults(false)
                .build();
        try {
            ResultsOfKG<T> result = toResults(clazz, runQuery(client(asServiceAccount), clazz, queryId, semanticType, options));
            if (result.getData() == null || result.getData().isEmpty()) {
                return null;
            }
            if (result.getData().size() > 1) {
                throw new RuntimeException(String.format("Too many (%d) results when querying %s with id %s of type %s", result.getData().size(), semanticType, id, clazz.getSimpleName()));
            }
            return result.getData().getFirst();
        } catch (MarmotGraphException e) {
            if (isNotFound(e)) {
                return null;
            }
            throw e;
        }
    }

    @Getter
    @Setter
    public static class TypeInformationItem implements Serializable {
        private String name;
        private String identifier;
        private String color;
    }

    public List<TypeInformationItem> getTypeInformation(DataStage dataStage) {
        TypeListOptions options = TypeListOptions.builder()
                .stage(toStage(dataStage))
                .withOccurrenceCount(false)
                .returnTotalResults(false)
                .build();
        try {
            return serviceAccountClient.types().list(options).allItems().map(t -> {
                TypeInformationItem item = new TypeInformationItem();
                item.setName(t.name());
                item.setIdentifier(t.identifier());
                item.setColor(t.color());
                return item;
            }).toList();
        } catch (MarmotGraphException e) {
            throw new RuntimeException("Wasn't able to fetch type information from KG", e);
        }
    }

    /**
     * @return the instance or null if it doesn't exist (or isn't accessible)
     */
    public Instance getInstance(String id, DataStage dataStage, boolean asServiceAccount) {
        UUID instanceId = MetaModelUtils.castToUUID(id);
        if (instanceId == null) {
            return null;
        }
        try {
            return client(asServiceAccount).instances().get(instanceId, toStage(dataStage), ResponseOptions.DEFAULT);
        } catch (MarmotGraphException e) {
            if (isNotFound(e)) {
                return null;
            }
            throw e;
        }
    }

    private final static String ID_FOR_BADGE_REGISTRATION = "8909ab6c-45c9-4b57-9f8a-6111eef752f6";

    public void persistBadges(String type, Map<String, Object> badges) {
        final UUID typeSpecificBadgeRegistration = UUID.nameUUIDFromBytes(String.format("%s/%s", ID_FOR_BADGE_REGISTRATION, type).getBytes(UTF_8));
        Map<String, Object> document = new HashMap<>();
        document.put("@type", "https://search.kg.ebrains.eu/SearchAggregations");
        badges.put("@type", "https://search.kg.ebrains.eu/Badges");
        document.put("https://search.kg.ebrains.eu/vocab/badges", badges);
        document.put("https://search.kg.ebrains.eu/vocab/forType", type);
        try {
            try {
                serviceAccountClient.instances().replace(typeSpecificBadgeRegistration, document);
            } catch (MarmotGraphException e) {
                if (!isNotFound(e)) {
                    throw e;
                }
                serviceAccountClient.instances().createWithId(typeSpecificBadgeRegistration, serviceClientId, document);
            }
            serviceAccountClient.instances().release(typeSpecificBadgeRegistration);
        } catch (MarmotGraphException e) {
            logger.error(String.format("Was not able to update the badge information in KG - %s", e.getMessage()), e);
        }
    }

    public void uploadQuery(String queryId, QuerySpecification query) {
        UUID id = UUID.fromString(queryId);
        try {
            serviceAccountClient.queries().save(id, query, serviceClientId);
        } catch (MarmotGraphApiException e) {
            if (e.statusCode() != CONFLICT) {
                throw e;
            }
            // The query is probably in a wrong space -> let's move it first and then update it
            serviceAccountClient.queries().move(id, serviceClientId);
            serviceAccountClient.queries().save(id, query, serviceClientId);
        }
    }

    private Page<JsonLdDocument> runQuery(MarmotGraphClient client, Class<?> clazz, String queryId, String semanticType, QueryRunOptions options) {
        if (appConfig.localQueries()) {
            return client.queries().run(queryGenerator.generate(clazz, semanticType), options, Map.of());
        }
        return client.queries().execute(UUID.fromString(queryId), options, Map.of());
    }

    /**
     * Binds the documents of the page to the given class - errors in the deserialization are reported in the
     * result instead of failing the whole page.
     */
    <T> ResultsOfKG<T> toResults(Class<T> clazz, Page<JsonLdDocument> page) {
        JavaType resultType = resultMapper.getTypeFactory().constructParametricType(ResultsOfKG.class, clazz);
        try {
            ResultsOfKG<T> result = resultMapper.convertValue(Map.of("data", page.items()), resultType);
            result.setSize(page.count());
            result.setTotal(page.total());
            // The server only provides a next marker if the page is full - the following page can still be empty
            result.setNextMarker(page.hasNext() ? page.nextMarker() : null);
            if (page.execution() != null) {
                result.setMessage(page.execution().message());
                result.setStartTime(page.execution().startTime());
                result.setDurationInMs(page.execution().durationInMs() != null ? page.execution().durationInMs().longValue() : null);
            }
            GracefulDeserializationProblemHandler.parsingErrorHandler(result);
            return result;
        } finally {
            GracefulDeserializationProblemHandler.ERROR_REPORTING_THREAD_LOCAL.remove();
        }
    }

    private <T> ResultsOfKG<T> doExecuteCallForIndexing(Class<T> clazz, String queryId, String semanticType, QueryRunOptions options, int currentTry) {
        try {
            return toResults(clazz, runQuery(serviceAccountClient, clazz, queryId, semanticType, options));
        } catch (MarmotGraphException e) {
            logger.warn("Was not able to execute call for indexing", e);
            if (currentTry < MAX_RETRIES) {
                final long waitingTime = currentTry * currentTry * 10000L;
                logger.warn("Retrying to execute call for indexing for max {} more times - next time in {} seconds", MAX_RETRIES - currentTry, waitingTime / 1000);
                try {
                    Thread.sleep(waitingTime);
                    return doExecuteCallForIndexing(clazz, queryId, semanticType, options, currentTry + 1);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            } else {
                logger.error("Was not able to execute the call for indexing. Going to skip it.", e);
                return null;
            }
        }
    }
}
