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

import org.marmotgraph.search.common.configuration.AppConfig;
import org.marmotgraph.search.common.configuration.GracefulDeserializationProblemHandler;
import org.marmotgraph.search.common.model.DataStage;
import org.marmotgraph.search.common.model.source.ResultsOfKG;
import org.marmotgraph.search.common.utils.MetaModelUtils;
import org.marmotgraph.search.common.utils.queryGenerator.MarmotGraphQuery;
import org.marmotgraph.search.common.utils.queryGenerator.QueryGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

@Component
public class KGServiceClient {
    private static final String BookmarkedInstancesOfParametrisedTypeQuery = """
             {
              "@context": {
                "@vocab": "https://core.kg.ebrains.eu/vocab/query/",
                "query": "https://schema.hbp.eu/myQuery/",
                "propertyName": {
                  "@id": "propertyName",
                  "@type": "@id"
                },
                "path": {
                  "@id": "path",
                  "@type": "@id"
                }
              },
              "meta": {
                "name": "BookmarkOf",
                "responseVocab": "https://schema.hbp.eu/myQuery/",
                "type": "https://core.kg.ebrains.eu/vocab/type/Bookmark"
              },
              "structure": [
                {
                  "propertyName": "query:bookmarkOfId",
                  "singleValue": "FIRST",
                  "path": [
                    "https://core.kg.ebrains.eu/vocab/bookmarkOf",
                    "@id"
                  ]
                },
                {
                  "propertyName": "query:BookmarkOfType",
                  "singleValue": "FIRST",
                  "filter": {
                    "op": "EQUALS",
                    "parameter": "type"
                  },
                  "path": [
                    "https://core.kg.ebrains.eu/vocab/bookmarkOf",
                    "@type"
                  ]
                }
              ]
            }""";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final int MAX_RETRIES = 5;
    private final String kgCoreEndpoint;
    private final WebClient serviceAccountWebClient;
    private final WebClient userWebClient;
    private final String serviceClientId;
    private final AppConfig appConfig;
    private final QueryGenerator queryGenerator;

    public KGServiceClient(@Qualifier("asServiceAccount") WebClient serviceAccountWebClient, @Qualifier("asUser") WebClient userWebClient, @Value("${kgcore.endpoint}") String kgCoreEndpoint, @Value("${spring.security.oauth2.client.registration.kg.client-id}") String serviceClientId, AppConfig appConfig, QueryGenerator queryGenerator) {
        this.kgCoreEndpoint = kgCoreEndpoint;
        this.serviceAccountWebClient = serviceAccountWebClient;
        this.userWebClient = userWebClient;
        this.serviceClientId = serviceClientId;
        this.appConfig = appConfig;
        this.queryGenerator = queryGenerator;
    }

    @Cacheable(value = "authEndpoint", unless = "#result == null")
    public String getAuthEndpoint() {
        String url = String.format("%s/users/authorization", kgCoreEndpoint);
        try {
            Map result = serviceAccountWebClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (result != null) {
                Map data = (Map) result.get("data");
                return data.get("endpoint").toString();
            }
        } catch (WebClientResponseException e) {
            logger.error("Was not able to fetch the auth endpoint from KG", e);
        }
        return null;
    }

    public Set<UUID> getInvitationsFromKG() {
        String url = String.format("%s/users/me/roles", kgCoreEndpoint);
        final Map<?, ?> result = userWebClient.get()
                .uri(url)
                .headers(h -> h.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (result != null) {
            final Object data = result.get("data");
            if (data instanceof Map) {
                final Object invitations = ((Map<?, ?>) data).get("invitations");
                if (invitations instanceof List) {
                    return ((List<?>) invitations).stream().filter(i -> i instanceof String).map(i -> MetaModelUtils.castToUUID((String) i)).filter(Objects::nonNull).collect(Collectors.toSet());
                }
            }
        }
        return Collections.emptySet();
    }

    public void addBookmark(UUID instanceId) {
        //save
        String url = String.format("%s/instances?space=myspace", kgCoreEndpoint);
        String fullyQualifyInstanceId = String.format("https://kg.ebrains.eu/api/instances/%s", instanceId);
        Map<String, Object> payload = Map.of(
                "@type", "https://core.kg.ebrains.eu/vocab/type/Bookmark",
                "https://core.kg.ebrains.eu/vocab/bookmarkOf", Map.of("@id", fullyQualifyInstanceId)
        );
        final Map<?, ?> result = userWebClient.post()
                .uri(url)
                .body(BodyInserters.fromValue(payload))
                .headers(h -> h.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (result != null) {
            final Object data = result.get("data");
            if (data instanceof Map) {
                final Object fullyQualifyBookmarkId = ((Map<?, ?>) data).get("@id");
                if (fullyQualifyBookmarkId instanceof String) {
                    final String uuid = substringAfterLast((String) fullyQualifyBookmarkId, "/");
                    final UUID bookmarkId = MetaModelUtils.castToUUID(uuid);
                    if (bookmarkId != null) {
                        //release
                        String releaseUrl = String.format("%s/instances/%s/release", kgCoreEndpoint, bookmarkId);
                        userWebClient.put()
                                .uri(releaseUrl)
                                .headers(h -> h.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                                .retrieve()
                                .bodyToMono(Void.class)
                                .block();
                    }
                }
            }
        }
    }

    public void deleteBookmark(UUID bookmarkId) {
        //unrelease
        String releaseUrl = String.format("%s/instances/%s/release", kgCoreEndpoint, bookmarkId);
        userWebClient.delete()
                .uri(releaseUrl)
                .headers(h -> h.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
        //delete
        String url = String.format("%s/instances/%s", kgCoreEndpoint, bookmarkId);
        userWebClient.delete()
                .uri(url)
                .headers(h -> h.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public List<UUID> getBookmarkIdsFromInstance(UUID id) {
        try {
            String fullyQualifyId = String.format("https://kg.ebrains.eu/api/instances/%s", id);
            String type = "https://core.kg.ebrains.eu/vocab/type/Bookmark";
            String encodedType = URLEncoder.encode(type, StandardCharsets.UTF_8.toString());
            String url = String.format("%s/instances?stage=%s&type=%s&space=myspace&from=0&size=1000", kgCoreEndpoint, DataStage.RELEASED, encodedType);
            final Map<?, ?> result = userWebClient.get()
                    .uri(url)
                    .headers(h -> h.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (result != null) {
                final Object data = result.get("data");
                if (data instanceof List) {
                    return ((List<?>) data).stream().filter(i -> i instanceof Map).map(i -> {
                        final Map<?, ?> bookmark = (Map<?, ?>) i;
                        final Object instance = bookmark.get("https://core.kg.ebrains.eu/vocab/bookmarkOf");
                        if (instance instanceof Map) {
                            final Object instanceId = ((Map<?, ?>) instance).get("@id");
                            if (instanceId instanceof String && instanceId.equals(fullyQualifyId)) {
                                final Object bookmarkId = bookmark.get("@id");
                                if (bookmarkId instanceof String) {
                                    final String uuid = substringAfterLast((String) bookmarkId, "/");
                                    return MetaModelUtils.castToUUID(uuid);
                                }
                            }
                        }
                        return null;
                    }).filter(Objects::nonNull).collect(Collectors.toList());
                }
            }
            return Collections.emptyList();
        } catch (UnsupportedEncodingException e) {
            return Collections.emptyList();
        }
    }

    public List<UUID> getBookmarkedInstancesOfType(String type) {
        try {
            String encodedType = URLEncoder.encode(type, StandardCharsets.UTF_8.toString());
            String restrictToSpaces = "myspace,common,dataset,model,metadatamodel,software,webservice";
            String url = String.format("%s/queries?size=1000&from=0&stage=%s&&restrictToSpaces=%s&type=%s", kgCoreEndpoint, DataStage.RELEASED, restrictToSpaces, encodedType);
            String payload = BookmarkedInstancesOfParametrisedTypeQuery;

            final Map<?, ?> result = userWebClient.post()
                    .uri(url)
                    .headers(h -> h.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                    .body(BodyInserters.fromValue(payload))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (result != null) {
                final Object data = result.get("data");
                if (data instanceof List) {
                    return ((List<?>) data).stream().filter(i -> i instanceof Map).map(i -> {
                        final Map<?, ?> bookmark = (Map<?, ?>) i;
                        final Object instanceId = bookmark.get("bookmarkOfId");
                        if (instanceId instanceof String) {
                            final String uuid = substringAfterLast((String) instanceId, "/");
                            return MetaModelUtils.castToUUID(uuid);
                        }
                        return null;
                    }).filter(Objects::nonNull).collect(Collectors.toList());
                }
            }
            return Collections.emptyList();
        } catch (UnsupportedEncodingException e) {
            return Collections.emptyList();
        }
    }


    public <T> ResultsOfKG<T> executeQueryForIndexing(Class<T> clazz, DataStage dataStage, String queryId, String semanticType, int from, int size) {
        String url;
        if (appConfig.localQueries()) {
            url = String.format("%s/queries?stage=%s&from=%d&size=%d", kgCoreEndpoint, dataStage, from, size);
        } else {
            url = String.format("%s/queries/%s/instances?stage=%s&from=%d&size=%d", kgCoreEndpoint, queryId, dataStage, from, size);
        }
        return executeCallForIndexing(clazz, url, semanticType);
    }


    public <T> T executeQueryForInstance(Class<T> clazz, DataStage dataStage, String queryId, String id, String semanticType, boolean asServiceAccount) {
        String url;
        if (appConfig.localQueries()) {
            url = String.format("%s/queries?stage=%s&instanceId=%s", kgCoreEndpoint, dataStage, id);
        } else {
            url = String.format("%s/queries/%s/instances?stage=%s&instanceId=%s", kgCoreEndpoint, queryId, dataStage, id);
        }
        try {
            return executeCallForInstance(clazz, url, semanticType, id, asServiceAccount);
        } catch (WebClientResponseException.NotFound e) {
            return null;
        }
    }

    @SuppressWarnings("java:S3740")
    public Map getInstance(String id, DataStage dataStage, boolean asServiceAccount) {
        String url = String.format("%s/instances/%s?stage=%s", kgCoreEndpoint, id, dataStage);
        return executeCallForInstance(Map.class, url, id, null, asServiceAccount);
    }

    private final static String ID_FOR_BADGE_REGISTRATION = "8909ab6c-45c9-4b57-9f8a-6111eef752f6";

    public void persistBadges(String type, Map<String, Object> badges) {
        final String typeSpecificBadgeRegistration = UUID.nameUUIDFromBytes(String.format("%s/%s", ID_FOR_BADGE_REGISTRATION, type).getBytes(UTF_8)).toString();
        Map<String, Object> document = new HashMap<>();
        document.put("@type", "https://search.kg.ebrains.eu/SearchAggregations");
        badges.put("@type", "https://search.kg.ebrains.eu/Badges");
        document.put("https://search.kg.ebrains.eu/vocab/badges", badges);
        document.put("https://search.kg.ebrains.eu/vocab/forType", type);
        try {
            try {
                serviceAccountWebClient.put().uri(String.format("%s/instances/%s", kgCoreEndpoint, typeSpecificBadgeRegistration)).body(BodyInserters.fromValue(document)).headers(h -> h.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                        .retrieve().bodyToMono(Void.class).block();
            } catch (WebClientResponseException.NotFound e) {
                serviceAccountWebClient.post().uri(String.format("%s/instances/%s?space=%s", kgCoreEndpoint, typeSpecificBadgeRegistration, serviceClientId)).body(BodyInserters.fromValue(document)).headers(h -> h.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                        .retrieve().bodyToMono(Void.class).block();
            }
            serviceAccountWebClient.put().uri(String.format("%s/instances/%s/release", kgCoreEndpoint, typeSpecificBadgeRegistration)).retrieve().bodyToMono(Void.class).block();
        } catch (WebClientResponseException e) {
            logger.error(String.format("Was not able to update the badge information in KG - %s", e.getMessage()), e);
        }
    }

    public void uploadQuery(String queryId, MarmotGraphQuery payload) {
        String url = String.format("%s/queries/%s?space=%s", kgCoreEndpoint, queryId, serviceClientId);
        try {
            serviceAccountWebClient.put()
                    .uri(url)
                    .bodyValue(payload)
                    .headers(h -> h.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (WebClientResponseException.Conflict c) {
            // The query is probably in a wrong space -> let's move it first

            Map releaseStatus = serviceAccountWebClient.get()
                    .uri(String.format("%s/instances/%s/release/status?releaseTreeScope=TOP_INSTANCE_ONLY", kgCoreEndpoint, queryId))
                    .headers(h -> h.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            boolean released = false;
            if (releaseStatus != null && releaseStatus.get("data") != null && !releaseStatus.get("data").equals("UNRELEASED")) {
                released = true;
            }
            if (released) {
                //Unrelease first
                serviceAccountWebClient.delete()
                        .uri(String.format("%s/instances/%s/release", kgCoreEndpoint, queryId))
                        .headers(h -> h.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .block();
            }
            // Move
            serviceAccountWebClient.put()
                    .uri(String.format("%s/instances/%s/spaces/%s", kgCoreEndpoint, queryId, serviceClientId))
                    .headers(h -> h.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
            if (released) {
                //Rerelease
                serviceAccountWebClient.put()
                        .uri(String.format("%s/instances/%s/release", kgCoreEndpoint, queryId))
                        .headers(h -> h.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                        .retrieve()
                        .bodyToMono(Void.class)
                        .block();
            }
            //Update
            serviceAccountWebClient.put()
                    .uri(url)
                    .body(BodyInserters.fromValue(payload))
                    .headers(h -> h.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        }
    }

    private <T> T executeCallForInstance(Class<T> clazz, String url, String id, String semanticType, boolean asServiceAccount) {
        WebClient webClient = asServiceAccount ? this.serviceAccountWebClient : this.userWebClient;

        boolean byQuery = appConfig.localQueries() && semanticType != null;
        if (byQuery) {
            MarmotGraphQuery query = queryGenerator.generate(clazz, semanticType);
            ResultsOfKG<T> result = (ResultsOfKG<T>) webClient.post().uri(url).bodyValue(query).headers(h -> h.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                    .retrieve().bodyToMono(ParameterizedTypeReference.forType(ResolvableType.forClassWithGenerics(ResultsOfKG.class, clazz).getType()))
                    .doOnSuccess(GracefulDeserializationProblemHandler::parsingErrorHandler)
                    .doFinally(t -> GracefulDeserializationProblemHandler.ERROR_REPORTING_THREAD_LOCAL.remove())
                    .block();
            if(result!=null && result.getData() != null){
                if(result.getData().size() == 1){
                    return result.getData().getFirst();
                }
                else{
                  throw new RuntimeException(String.format("Too many (%d) results when querying %s with id %s of type %s", result.getData().size(), semanticType, id, clazz.getSimpleName()));

                }
            }
            return null;

        } else {
            return webClient.get().uri(url).headers(h -> h.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                    .retrieve().bodyToMono(clazz).doOnSuccess(GracefulDeserializationProblemHandler::parsingErrorHandler)
                    .doFinally(t -> GracefulDeserializationProblemHandler.ERROR_REPORTING_THREAD_LOCAL.remove())
                    .block();
        }
    }

    private <T> ResultsOfKG<T> executeCallForIndexing(Class<T> clazz, String url, String semanticType) {
        return doExecuteCallForIndexing(clazz, url, semanticType, 0);
    }

    private <T> ResultsOfKG<T> doExecuteCallForIndexing(Class<T> clazz, String url, String semanticType, int currentTry) {
        try {
            WebClient.RequestHeadersSpec<?> request;
            if (appConfig.localQueries()) {
                MarmotGraphQuery query = queryGenerator.generate(clazz, semanticType);
                request = serviceAccountWebClient.post().uri(url).bodyValue(query);
            } else {
                request = serviceAccountWebClient.get().uri(url);
            }
            ResolvableType resolvableType = ResolvableType.forClassWithGenerics(ResultsOfKG.class, clazz);
            return (ResultsOfKG<T>) request.headers(h -> h.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                    .retrieve()
                    .bodyToMono(ParameterizedTypeReference.forType(resolvableType.getType())).doOnSuccess(GracefulDeserializationProblemHandler::parsingErrorHandler)
                    .doFinally(t -> GracefulDeserializationProblemHandler.ERROR_REPORTING_THREAD_LOCAL.remove())
                    .block();
        } catch (WebClientResponseException e) {
            logger.warn("Was not able to execute call for indexing", e);
            if (currentTry < MAX_RETRIES) {
                final long waitingTime = currentTry * currentTry * 10000L;
                logger.warn("Retrying to execute call for indexing for max {} more times - next time in {} seconds", MAX_RETRIES - currentTry, waitingTime / 1000);
                try {
                    Thread.sleep(waitingTime);
                    return doExecuteCallForIndexing(clazz, url, semanticType, currentTry + 1);
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
