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


package org.marmotgraph.search.common.configuration;

import org.junit.jupiter.api.Test;
import org.marmotgraph.sdk.MarmotGraphClient;
import org.marmotgraph.sdk.MarmotGraphException;
import org.marmotgraph.sdk.auth.AuthenticationInformation;
import org.marmotgraph.sdk.http.HttpRequestSpec;
import org.marmotgraph.sdk.http.HttpResponse;
import org.marmotgraph.sdk.http.HttpTransport;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CoreAuthenticationTest {

    private static final String HOST = "http://core:8080";
    private static final String WELL_KNOWN = "http://idp/realms/test/.well-known/openid-configuration";

    private final List<HttpRequestSpec> requests = new ArrayList<>();
    private boolean coreAvailable = true;

    private final HttpTransport transport = request -> {
        requests.add(request);
        if (!coreAvailable) {
            throw new MarmotGraphException("Connection refused");
        }
        String body;
        if (request.url().equals(HOST + "/v4/setup/authentication")) {
            body = "{\"data\": {\"endpoint\": \"" + WELL_KNOWN + "\", \"loginClientId\": \"login\"}}";
        } else if (request.url().equals(WELL_KNOWN)) {
            body = """
                    {"issuer": "http://idp/realms/test", "authorization_endpoint": "http://idp/auth", "token_endpoint": "http://idp/token",
                     "userinfo_endpoint": "http://idp/userinfo", "jwks_uri": "http://idp/certs"}""";
        } else if (request.url().equals("http://idp/token")) {
            body = "{\"access_token\": \"service-token\"}";
        } else {
            body = "{\"data\": {}}";
        }
        return new HttpResponse(200, Map.of(), body.getBytes(StandardCharsets.UTF_8));
    };

    private MarmotGraphClient marmotGraphClient() {
        return MarmotGraphClient.builder(HOST).clientId("kg-search").clientSecret("secret").transport(transport).build();
    }

    @Test
    void nothingIsRequestedFromTheCoreBeforeTheFirstUse() {
        new CoreAuthentication(marmotGraphClient(), "kg-search");

        assertTrue(requests.isEmpty());
    }

    @Test
    void authenticationInformationIsLoadedFromTheCoreAndCached() {
        CoreAuthentication coreAuthentication = new CoreAuthentication(marmotGraphClient(), "kg-search");

        AuthenticationInformation authentication = coreAuthentication.authenticationInformation();
        coreAuthentication.authenticationInformation();

        assertEquals("http://idp/realms/test", authentication.issuer());
        assertEquals("http://idp/certs", authentication.jwksUri());
        assertEquals(2, requests.size());
        // The discovery must not trigger the authentication of the service account
        assertFalse(requests.getFirst().headers().containsKey("Authorization"));

        ClientRegistration registration = coreAuthentication.clientRegistration();
        assertEquals("kg-search", registration.getClientId());
        assertEquals("http://idp/userinfo", registration.getProviderDetails().getUserInfoEndpoint().getUri());
    }

    @Test
    void lookupIsRetriedIfTheCoreWasNotAvailable() {
        CoreAuthentication coreAuthentication = new CoreAuthentication(marmotGraphClient(), "kg-search");
        coreAvailable = false;
        assertThrows(MarmotGraphException.class, coreAuthentication::authenticationInformation);

        coreAvailable = true;
        assertEquals("http://idp/token", coreAuthentication.authenticationInformation().tokenEndpoint());
    }

    @Test
    void serviceAccountTokenIsFetchedFromTheDiscoveredTokenEndpoint() {
        String token = marmotGraphClient().httpExecutor().tokenProvider().getToken();

        assertEquals("service-token", token);
        assertEquals("http://idp/token", requests.getLast().url());
    }
}
