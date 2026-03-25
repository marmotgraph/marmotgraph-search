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

package org.marmotgraph.search.api;

import lombok.AllArgsConstructor;
import org.marmotgraph.search.common.controller.kg.KG;
import org.marmotgraph.search.common.controller.translation.TranslationController;
import org.marmotgraph.search.common.controller.translation.models.TranslatorModel;
import org.marmotgraph.search.common.model.DataStage;
import org.marmotgraph.search.common.model.target.TargetInstance;
import org.marmotgraph.search.common.security.UserAuthorization;
import org.marmotgraph.search.common.security.UserRoles;
import org.marmotgraph.search.common.services.DOICitationFormatter;
import org.marmotgraph.search.common.utils.MetaModelUtils;
import org.marmotgraph.search.common.utils.TranslationException;
import org.marmotgraph.search.common.utils.translation.TranslatorRegistry;
import org.marmotgraph.search.controller.search.SearchController;
import org.marmotgraph.search.model.FacetValue;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequestMapping(value = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
@AllArgsConstructor
@SuppressWarnings("java:S1452") // we keep the generics intentionally
public class Search {
    private final SearchController searchController;
    private final TranslationController translationController;
    private final KG kgV3;
    private final DOICitationFormatter doiCitationFormatter;
    private final TranslatorRegistry translatorRegistry;
    private final UserAuthorization userAuthorization;


    @GetMapping("/citation")
    public ResponseEntity<String> getCitation(@RequestParam("doi") String doiWithoutPrefix, @RequestParam("style") String style, @RequestParam("contentType") String contentType) {
        String doi = String.format("https://doi.org/%s", doiWithoutPrefix);
        String citation = doiCitationFormatter.getDOICitation(doi, style, contentType);
        return ResponseEntity
                .ok()
                .contentType(MediaType.TEXT_PLAIN)
                .body(citation);
    }


    @GetMapping("/groups")
    public ResponseEntity<List<UserAuthorization.GroupResponse>> getGroups(JwtAuthenticationToken token) {
        List<UserAuthorization.GroupResponse> groups = userAuthorization.getGroups(token);
        if (groups != null) {
            return ResponseEntity.ok(groups);
        } else {
            return ResponseEntity.ok(Collections.emptyList());
        }
    }


    @SuppressWarnings("java:S3740") // we keep the generics intentionally
    @GetMapping("/{id}/live")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<?, ?>> translate(@PathVariable("id") String id) throws TranslationException {
        try {
            final List<String> typesOfInstance = kgV3.getTypesOfInstance(id, DataStage.IN_PROGRESS, false);
            final TranslatorModel translatorModel = this.translatorRegistry.getTranslators().stream().filter(m -> m.translator() != null && m.semanticTypes().stream().anyMatch(typesOfInstance::contains)).findFirst().orElse(null);
            if (translatorModel != null) {
                final String queryId = typesOfInstance.stream().map(translatorModel::queryId).findFirst().orElse(null);
                final TargetInstance v = translationController.translateToTargetInstanceForLiveMode(kgV3, translatorModel, queryId, DataStage.IN_PROGRESS, id);
                if (v != null) {
                    return ResponseEntity.ok(searchController.getLiveDocument(v));
                }
            }
            return ResponseEntity.notFound().build();
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (HttpClientErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/groups/public/documents/{id}")
    public ResponseEntity<?> getDocumentForPublic(@PathVariable("id") String id) {
        try {
            Map<String, Object> res = searchController.getSearchDocument(DataStage.RELEASED, id);
            if (res != null) {
                return ResponseEntity.ok(res);
            }
            return ResponseEntity.notFound().build();
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    private boolean canReadLiveFiles(JwtAuthenticationToken token, UUID repositoryUUID) {
        return UserRoles.hasInProgressRole(token) || searchController.isInvitedForFileRepository(repositoryUUID);
    }


    @GetMapping("/repositories/{id}/files/live")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getFilesFromRepoForLive(@PathVariable("id") String repositoryId,
                                                     @RequestParam(required = false, defaultValue = "", name = "format") String format,
                                                     @RequestParam(required = false, defaultValue = "", name = "groupingType") String groupingType, JwtAuthenticationToken token) {
        final UUID repositoryUUID = MetaModelUtils.castToUUID(repositoryId);
        if (repositoryUUID == null) {
            return ResponseEntity.badRequest().build();
        }
        if (userAuthorization.canReadLiveFiles(token, repositoryUUID)) {
            try {
                return searchController.getFilesFromRepo(DataStage.IN_PROGRESS, repositoryUUID, format, groupingType);
            } catch (WebClientResponseException e) {
                return ResponseEntity.status(e.getStatusCode()).build();
            }
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/repositories/{id}/files/formats/live")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getFileFormatsFromRepoForLive(@PathVariable("id") String repositoryId, JwtAuthenticationToken token) {
        final UUID repositoryUUID = MetaModelUtils.castToUUID(repositoryId);
        if (repositoryUUID == null) {
            return ResponseEntity.badRequest().build();
        }
        if (userAuthorization.canReadLiveFiles(token, repositoryUUID)) {
            return searchController.getFileFormatsFromRepo(DataStage.IN_PROGRESS, repositoryUUID);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/repositories/{id}/files/groupingTypes/live")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getGroupingTypesFromRepoForLive(@PathVariable("id") String repositoryId,
                                                             JwtAuthenticationToken token) {
        final UUID repositoryUUID = MetaModelUtils.castToUUID(repositoryId);
        if (repositoryUUID == null) {
            return ResponseEntity.badRequest().build();
        }
        if (userAuthorization.canReadLiveFiles(token, repositoryUUID)) {
            //kgV3.executeQueryForInstance("clazz", DataStage.IN_PROGRESS, "queryId", repositoryId, false)
            return searchController.getGroupingTypesFromRepo(DataStage.IN_PROGRESS, repositoryUUID);
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @GetMapping("/groups/public/repositories/{id}/files/formats")
    public ResponseEntity<?> getFileFormatsFromRepoForPublic(@PathVariable("id") String repositoryId) {
        final UUID repositoryUUID = MetaModelUtils.castToUUID(repositoryId);
        if (repositoryUUID == null) {
            return ResponseEntity.badRequest().build();
        }
        return searchController.getFileFormatsFromRepo(DataStage.RELEASED, repositoryUUID);
    }

    @GetMapping("/groups/curated/repositories/{id}/files/formats")
    @UserRoles.MustBeInProgress
    public ResponseEntity<?> getFileFormatsFromRepoForCurated(@PathVariable("id") String repositoryId) {
        final UUID repositoryUUID = MetaModelUtils.castToUUID(repositoryId);
        if (repositoryUUID == null) {
            return ResponseEntity.badRequest().build();
        }
        return searchController.getFileFormatsFromRepo(DataStage.IN_PROGRESS, repositoryUUID);
    }

    @GetMapping("/groups/public/repositories/{id}/files/groupingTypes")
    public ResponseEntity<?> getGroupingTypesFromRepoForPublic(@PathVariable("id") String repositoryId) {
        final UUID repositoryUUID = MetaModelUtils.castToUUID(repositoryId);
        if (repositoryUUID == null) {
            return ResponseEntity.badRequest().build();
        }
        return searchController.getGroupingTypesFromRepo(DataStage.RELEASED, repositoryUUID);
    }

    @GetMapping("/groups/curated/repositories/{id}/files/groupingTypes")
    @UserRoles.MustBeInProgress
    public ResponseEntity<?> getGroupingTypesFromRepoForCurated(@PathVariable("id") String repositoryId) {
        final UUID repositoryUUID = MetaModelUtils.castToUUID(repositoryId);
        if (repositoryUUID == null) {
            return ResponseEntity.badRequest().build();
        }
        return searchController.getGroupingTypesFromRepo(DataStage.IN_PROGRESS, repositoryUUID);
    }

    @GetMapping("/groups/public/repositories/{id}/files")
    public ResponseEntity<?> getFilesFromRepoForPublic(@PathVariable("id") String id,
                                                       @RequestParam(required = false, defaultValue = "", name = "format") String format,
                                                       @RequestParam(required = false, defaultValue = "", name = "groupingType") String groupingType) {
        final UUID uuid = MetaModelUtils.castToUUID(id);
        if (uuid == null) {
            return ResponseEntity.badRequest().build();
        }
        return searchController.getFilesFromRepo(DataStage.RELEASED, uuid, format, groupingType);
    }

    @GetMapping("/groups/curated/repositories/{id}/files")
    @UserRoles.MustBeInProgress
    public ResponseEntity<?> getFilesFromRepoForCurated(@PathVariable("id") String id,
                                                        @RequestParam(required = false, defaultValue = "", name = "format") String format,
                                                        @RequestParam(required = false, defaultValue = "", name = "groupingType") String groupingType) {
        final UUID uuid = MetaModelUtils.castToUUID(id);
        if (uuid == null) {
            return ResponseEntity.badRequest().build();
        }
        return searchController.getFilesFromRepo(DataStage.IN_PROGRESS, uuid, format, groupingType);
    }

    @GetMapping("/groups/public/documents/{type}/{id}")
    public ResponseEntity<?> getDocumentForPublic(@PathVariable("type") String type, @PathVariable("id") String id) {
        try {
            Map<String, Object> res = searchController.getSearchDocument(DataStage.RELEASED, String.format("%s/%s", type, id));
            if (res != null) {
                return ResponseEntity.ok(res);
            }
            return ResponseEntity.notFound().build();
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/groups/curated/documents/{id}")
    @UserRoles.MustBeInProgress
    public ResponseEntity<?> getDocumentForCurated(@PathVariable("id") String id) {
        try {
            Map<String, Object> res = searchController.getSearchDocument(DataStage.IN_PROGRESS, id);
            if (res != null) {
                return ResponseEntity.ok(res);
            }
            return ResponseEntity.notFound().build();
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/groups/curated/documents/{type}/{id}")
    @UserRoles.MustBeInProgress
    public ResponseEntity<?> getDocumentForCurated(@PathVariable("type") String type, @PathVariable("id") String id) {
        try {
            Map<String, Object> res = searchController.getSearchDocument(DataStage.IN_PROGRESS, String.format("%s/%s", type, id));
            if (res != null) {
                return ResponseEntity.ok(res);
            }
            return ResponseEntity.notFound().build();
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping("/groups/public/search")
    public ResponseEntity<?> searchPublic(
            @RequestParam(required = false, defaultValue = "", name = "q") String q,
            @RequestParam(required = false, defaultValue = "", name = "type") String type,
            @RequestParam(required = false, defaultValue = "0", name = "from") int from,
            @RequestParam(required = false, defaultValue = "20", name = "size") int size,
            @RequestBody Map<String, FacetValue> facetValues
    ) {
        try {
            Map<String, Object> result = searchController.search(q, type, from, size, facetValues, DataStage.RELEASED);
            return ResponseEntity.ok(result);
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PostMapping("/groups/curated/search")
    @UserRoles.MustBeInProgress
    public ResponseEntity<?> searchCurated(
            @RequestParam(required = false, defaultValue = "", name = "q") String q,
            @RequestParam(required = false, defaultValue = "", name = "type") String type,
            @RequestParam(required = false, defaultValue = "0", name = "from") int from,
            @RequestParam(required = false, defaultValue = "20", name = "size") int size,
            @RequestBody Map<String, FacetValue> facetValues) {
        try {
            Map<String, Object> result = searchController.search(q, type, from, size, facetValues, DataStage.IN_PROGRESS);
            return ResponseEntity.ok(result);
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @GetMapping("/{id}/bookmark")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> userBookmarkedInstance(@PathVariable("id") String id) {
        UUID uuid = MetaModelUtils.castToUUID(id);
        if (uuid == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        try {
            Map<String, Object> result = Map.of(
                    "id", id,
                    "bookmarked", searchController.isBookmarked(uuid)
            );
            return ResponseEntity.ok(result);
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @DeleteMapping("/{id}/bookmark")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> userDeleteBookmarkOfInstance(@PathVariable("id") String id) {
        UUID uuid = MetaModelUtils.castToUUID(id);
        if (uuid == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        try {
            searchController.deleteBookmark(uuid);
            Map<String, Object> result = Map.of(
                    "id", id,
                    "bookmarked", false
            );
            return ResponseEntity.ok(result);
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }

    @PutMapping("/{id}/bookmark")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> userBookmarkInstance(@PathVariable("id") String id) {
        UUID uuid = MetaModelUtils.castToUUID(id);
        if (uuid == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        try {
            searchController.addBookmark(MetaModelUtils.castToUUID(id));
            Map<String, Object> result = Map.of(
                    "id", id,
                    "bookmarked", true
            );
            return ResponseEntity.ok(result);
        } catch (WebClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).build();
        }
    }
}
