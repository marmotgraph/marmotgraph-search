/*
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
 */

package org.marmotgraph.search.common.configuration;

import org.junit.jupiter.api.Test;
import org.marmotgraph.sdk.MarmotGraphException;
import org.marmotgraph.sdk.auth.AuthenticationInformation;
import org.marmotgraph.sdk.auth.OidcConfig;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class SwaggerIndexPageWithLoginClientTest {

    // The end of the initialization of the Swagger UI, where springdoc adds the OAuth configuration
    private static final String HTML = "  });\n";

    private final CoreAuthentication coreAuthentication = mock(CoreAuthentication.class);
    private final SwaggerUiOAuthProperties oAuthProperties = new SwaggerUiOAuthProperties();

    private SwaggerIndexPageWithLoginClient transformer() {
        oAuthProperties.setUsePkceWithAuthorizationCodeGrant(true);
        return new SwaggerIndexPageWithLoginClient(new SwaggerUiConfigProperties(), oAuthProperties, mock(SwaggerWelcomeCommon.class), new ObjectMapperProvider(new SpringDocConfigProperties()), coreAuthentication);
    }

    private static AuthenticationInformation authentication() {
        return new AuthenticationInformation(new OidcConfig("token", null, "auth", null, "issuer", "jwks", "userinfo"), "login-client");
    }

    @Test
    void clientIdIsTheLoginClientOfTheCore() throws Exception {
        when(coreAuthentication.authenticationInformation()).thenReturn(authentication());
        String html = transformer().addInitOauth(HTML);
        assertEquals("login-client", oAuthProperties.getClientId());
        assertTrue(html.contains("\"clientId\":\"login-client\""), html);
    }

    @Test
    void explicitlyConfiguredClientIdIsKept() throws Exception {
        oAuthProperties.setClientId("configured");
        transformer().addInitOauth(HTML);
        assertEquals("configured", oAuthProperties.getClientId());
        verifyNoInteractions(coreAuthentication);
    }

    @Test
    void unavailableCoreIsRetriedOnTheNextRequest() throws Exception {
        when(coreAuthentication.authenticationInformation()).thenThrow(new MarmotGraphException("core is down")).thenReturn(authentication());
        SwaggerIndexPageWithLoginClient transformer = transformer();
        transformer.addInitOauth(HTML);
        assertEquals(null, oAuthProperties.getClientId());
        transformer.addInitOauth(HTML);
        assertEquals("login-client", oAuthProperties.getClientId());
    }
}
