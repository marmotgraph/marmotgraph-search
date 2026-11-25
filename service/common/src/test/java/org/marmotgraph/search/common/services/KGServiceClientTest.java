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
import org.junit.jupiter.api.Test;
import org.marmotgraph.sdk.MarmotGraphClient;
import org.marmotgraph.sdk.http.HttpRequestSpec;
import org.marmotgraph.sdk.http.HttpResponse;
import org.marmotgraph.sdk.http.HttpTransport;
import org.marmotgraph.sdk.model.ScopeSpecification;
import org.marmotgraph.search.common.configuration.AppConfig;
import org.marmotgraph.search.common.model.DataStage;
import org.marmotgraph.search.common.model.source.ResultsOfKG;
import org.marmotgraph.search.common.utils.queryGenerator.QueryGenerator;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class KGServiceClientTest {

    @Getter
    @Setter
    public static class Item {
        private String id;
        private String name;
    }

    private static final String QUERY_ID = UUID.randomUUID().toString();

    private final List<HttpRequestSpec> requests = new ArrayList<>();

    private KGServiceClient client(Function<HttpRequestSpec, String> responder) {
        return client(responder, null);
    }

    private KGServiceClient client(Function<HttpRequestSpec, String> responder, String tenant) {
        HttpTransport transport = request -> {
            requests.add(request);
            return new HttpResponse(200, Map.of(), responder.apply(request).getBytes(StandardCharsets.UTF_8));
        };
        MarmotGraphClient marmotGraphClient = MarmotGraphClient.builder("http://localhost:8080").token("token").transport(transport).build();
        return new KGServiceClient(marmotGraphClient, "kg-search", true, new AppConfig(false, tenant), new QueryGenerator());
    }

    @Test
    void executeQueryForIndexingFollowsTheMarkerOfTheServer() {
        KGServiceClient kgServiceClient = client(request -> request.url().contains("marker=abc")
                ? "{\"data\": [{\"id\": \"3\", \"name\": \"third\"}], \"size\": 1}"
                : "{\"data\": [{\"id\": \"1\", \"name\": \"first\"}, {\"id\": \"2\", \"name\": \"second\"}], \"size\": 2, \"nextMarker\": \"abc\"}");

        ResultsOfKG<Item> firstPage = kgServiceClient.executeQueryForIndexing(Item.class, DataStage.RELEASED, QUERY_ID, "https://schema/Type", null, 2);

        assertEquals(List.of("first", "second"), firstPage.getData().stream().map(Item::getName).toList());
        assertEquals("abc", firstPage.getNextMarker());
        assertEquals(2, firstPage.getSize());
        String firstUrl = requests.getFirst().url();
        assertTrue(firstUrl.startsWith("http://localhost:8080/v4/queries/" + QUERY_ID + "/instances"), firstUrl);
        assertFalse(firstUrl.contains("marker="), firstUrl);
        assertFalse(firstUrl.contains("from="), firstUrl);
        assertTrue(firstUrl.contains("stage=RELEASED"), firstUrl);
        assertTrue(firstUrl.contains("returnTotalResults=false"), firstUrl);

        ResultsOfKG<Item> secondPage = kgServiceClient.executeQueryForIndexing(Item.class, DataStage.RELEASED, QUERY_ID, "https://schema/Type", firstPage.getNextMarker(), 2);

        assertEquals(List.of("third"), secondPage.getData().stream().map(Item::getName).toList());
        assertNull(secondPage.getNextMarker());
        assertTrue(requests.get(1).url().contains("marker=abc"), requests.get(1).url());
    }

    @Test
    void executeQueryForIndexingStopsOnEmptyPageEvenIfAMarkerIsProvided() {
        // The server provides a next marker for full pages - the following page can still be empty
        KGServiceClient kgServiceClient = client(request -> "{\"data\": [], \"size\": 0, \"nextMarker\": \"xyz\"}");

        ResultsOfKG<Item> page = kgServiceClient.executeQueryForIndexing(Item.class, DataStage.IN_PROGRESS, QUERY_ID, "https://schema/Type", "abc", 2);

        assertTrue(page.getData().isEmpty());
        assertNull(page.getNextMarker());
    }

    @Test
    void uploadScopeDefinesTheSearchScopeForTheType() {
        KGServiceClient kgServiceClient = client(request -> "");

        kgServiceClient.uploadScope("https://schema/Type", ScopeSpecification.of(ScopeSpecification.Step.of(ScopeSpecification.Path.outgoing("https://schema/author"))));

        HttpRequestSpec request = requests.getFirst();
        assertEquals("PUT", request.method());
        assertTrue(request.url().startsWith("http://localhost:8080/v4/types/scopes/search?"), request.url());
        assertTrue(request.url().contains("type=https"), request.url());
        String body = new String(request.body(), StandardCharsets.UTF_8);
        assertTrue(body.contains("\"@id\":\"https://schema/author\""), body);
    }

    @Test
    void uploadScopePrefixesTheScopeWithTheTenant() {
        KGServiceClient kgServiceClient = client(request -> "", "foo");

        kgServiceClient.uploadScope("https://schema/Type", ScopeSpecification.of(ScopeSpecification.Step.of(ScopeSpecification.Path.outgoing("https://schema/author"))));

        String url = requests.getFirst().url();
        assertTrue(url.startsWith("http://localhost:8080/v4/types/scopes/foo-search?"), url);
    }
}
