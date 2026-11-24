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

package org.marmotgraph.search.api;

import org.junit.jupiter.api.Test;
import org.marmotgraph.sdk.auth.AuthenticationInformation;
import org.marmotgraph.sdk.auth.OidcConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SettingsKeycloakConfigTest {

    private static AuthenticationInformation authentication(String issuer) {
        return new AuthenticationInformation(new OidcConfig("token", null, "auth", null, issuer, "jwks", "userinfo"), "login-client");
    }

    @Test
    void realmAndUrlAreDerivedFromTheIssuer() {
        assertEquals(new Settings.KeycloakConfig("hbp", "https://iam.example.org/auth", "login-client", true),
                Settings.KeycloakConfig.from(authentication("https://iam.example.org/auth/realms/hbp"), true));
    }

    @Test
    void trailingSlashOfTheIssuerIsIgnored() {
        assertEquals(new Settings.KeycloakConfig("hbp", "https://iam.example.org", "login-client", false),
                Settings.KeycloakConfig.from(authentication("https://iam.example.org/realms/hbp/"), false));
    }

    @Test
    void issuerWithoutRealmProvidesNoRealm() {
        assertEquals(new Settings.KeycloakConfig(null, "https://login.example.org", "login-client", true),
                Settings.KeycloakConfig.from(authentication("https://login.example.org"), true));
    }
}
