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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.JacksonJsonDecoder;
import org.springframework.http.codec.json.JacksonJsonEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Configuration
public class OauthClient {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final JsonMapper jsonMapper = JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).addHandler(new GracefulDeserializationProblemHandler()).build();
    private final HttpClient httpClient = HttpClient.create().httpResponseDecoder(spec -> spec.maxHeaderSize(64*1024));

    private WebClient.Builder defaultWebClientBuilder(){
         return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient)).codecs(config -> {
            config.defaultCodecs().jacksonJsonDecoder(new JacksonJsonDecoder(jsonMapper));
            config.defaultCodecs().jacksonJsonEncoder(new JacksonJsonEncoder(jsonMapper));
            config.defaultCodecs().maxInMemorySize(1024 * 1000000);
        });
    }

    @Bean
    @Primary
    WebClient standardWebClient() {
        return defaultWebClientBuilder().build();
    }

    /**
     * The MarmotGraph client authenticated as the service account - configured by the SDK from the environment
     * (MARMOTGRAPH_HOST, MARMOTGRAPH_CLIENT_ID and MARMOTGRAPH_CLIENT_SECRET). The core is only contacted on the first
     * call, so the service starts even if the core isn't available. Clients acting on behalf of the user are derived
     * from it (see {@link MarmotGraphClient#withToken(String)}) so they share the same connection pool.
     */
    @Bean
    MarmotGraphClient marmotGraphClient() {
        return MarmotGraphClient.builder().build();
    }

    @Bean
    CoreAuthentication coreAuthentication(MarmotGraphClient marmotGraphClient, @Value("${MARMOTGRAPH_CLIENT_ID}") String clientId) {
        return new CoreAuthentication(marmotGraphClient, clientId);
    }

    /**
     * Validates the tokens of the users against the OpenID Connect provider of the core - resolved on the first
     * request, so the service starts even if the core isn't available.
     */
    @Bean
    JwtDecoder jwtDecoder(CoreAuthentication coreAuthentication) {
        return new JwtDecoder() {
            private volatile JwtDecoder delegate;

            @Override
            public Jwt decode(String token) throws JwtException {
                return delegate().decode(token);
            }

            private JwtDecoder delegate() {
                JwtDecoder result = delegate;
                if (result == null) {
                    final AuthenticationInformation config;
                    try {
                        config = coreAuthentication.authenticationInformation();
                    } catch (MarmotGraphException e) {
                        // Not a problem of the token -> results in a server error instead of a 401
                        throw new JwtException("Was not able to load the authentication configuration from the MarmotGraph core", e);
                    }
                    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(config.jwksUri()).build();
                    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(config.issuer()));
                    result = decoder;
                    delegate = result;
                }
                return result;
            }
        };
    }

}
