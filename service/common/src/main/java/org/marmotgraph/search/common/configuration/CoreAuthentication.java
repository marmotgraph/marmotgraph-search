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

import org.marmotgraph.sdk.MarmotGraphClient;
import org.marmotgraph.sdk.MarmotGraphException;
import org.marmotgraph.sdk.auth.AuthenticationInformation;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

/**
 * The authentication settings of the MarmotGraph core (its OpenID Connect provider), loaded lazily on first use and
 * cached afterwards - so the service can start independently of the availability of the core. If the core can't be
 * reached, the lookup fails and is retried on the next use.
 */
public class CoreAuthentication {

    static final String CLIENT_REGISTRATION_ID = "kg";

    private final MarmotGraphClient unauthenticatedClient;
    private final String clientId;

    private volatile AuthenticationInformation authenticationInformation;
    private volatile ClientRegistration clientRegistration;

    public CoreAuthentication(MarmotGraphClient marmotGraphClient, String clientId) {
        // The setup endpoint is public - and the discovery must not trigger the (lazy) authentication of the client
        this.unauthenticatedClient = marmotGraphClient.withTokenProvider(_ -> null);
        this.clientId = clientId;
    }

    /**
     * @throws MarmotGraphException if the core (or its OpenID Connect provider) can't be reached
     */
    public AuthenticationInformation authenticationInformation() {
        AuthenticationInformation result = authenticationInformation;
        if (result == null) {
            synchronized (this) {
                result = authenticationInformation;
                if (result == null) {
                    result = unauthenticatedClient.setup().discoverAuthentication();
                    if (result.issuer() == null || result.jwksUri() == null) {
                        throw new MarmotGraphException("The OpenID Connect configuration of the MarmotGraph core is incomplete (issuer and jwks_uri are required)");
                    }
                    authenticationInformation = result;
                }
            }
        }
        return result;
    }

    /**
     * The client registration of this service - e.g. to load the user info of a token.
     */
    public ClientRegistration clientRegistration() {
        ClientRegistration result = clientRegistration;
        if (result == null) {
            AuthenticationInformation authentication = authenticationInformation();
            result = ClientRegistration.withRegistrationId(CLIENT_REGISTRATION_ID)
                    .clientId(clientId)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .issuerUri(authentication.issuer())
                    .tokenUri(authentication.tokenEndpoint())
                    .authorizationUri(authentication.authorizationEndpoint())
                    .userInfoUri(authentication.userinfoEndpoint())
                    .jwkSetUri(authentication.jwksUri())
                    .userNameAttributeName(IdTokenClaimNames.SUB)
                    .build();
            clientRegistration = result;
        }
        return result;
    }
}
