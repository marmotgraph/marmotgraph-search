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

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.OAuthFlow;
import io.swagger.v3.oas.models.security.OAuthFlows;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.marmotgraph.sdk.MarmotGraphException;
import org.marmotgraph.sdk.auth.AuthenticationInformation;
import org.marmotgraph.search.common.customization.Customization;
import org.marmotgraph.search.common.model.CommitInfo;
import org.marmotgraph.search.common.security.JwtUserInfoConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collections;


@Configuration
@EnableCaching
@EnableScheduling
public class Config {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Bean
    public OpenAPI customOpenAPI(@Value("${eu.ebrains.kg.commit}") String commit, Customization customization, CommitInfo commitInfo) {
        // The URLs of the flow are resolved from the core when the API docs are requested (see oAuthFlowCustomizer)
        OAuthFlow oAuthFlow = new OAuthFlow();
        SecurityScheme userToken = new SecurityScheme().name(Constants.AUTHORIZATION).type(SecurityScheme.Type.OAUTH2).flows(new OAuthFlows().authorizationCode(oAuthFlow)).description("The user authentication");
        SecurityRequirement userWithoutClientReq = new SecurityRequirement().addList(Constants.AUTHORIZATION);

        OpenAPI openapi = new OpenAPI().openapi("3.0.3");
        String description = String.format("This is the API of the EBRAINS Knowledge Graph (commit %s)", commitInfo);

        return openapi.info(new Info().version("v3.0.0").title(String.format("This is the KG Search API for %s", customization.getConfiguration().name())).description(description).license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html")).termsOfService("https://kg.ebrains.eu/search-terms-of-use.html"))
                .components(new Components()).schemaRequirement(Constants.AUTHORIZATION, userToken)
                .security(Collections.singletonList(userWithoutClientReq));
    }

    @Bean
    public OpenApiCustomizer oAuthFlowCustomizer(CoreAuthentication coreAuthentication) {
        return openApi -> {
            SecurityScheme scheme = openApi.getComponents() != null && openApi.getComponents().getSecuritySchemes() != null ? openApi.getComponents().getSecuritySchemes().get(Constants.AUTHORIZATION) : null;
            if (scheme != null && scheme.getFlows() != null && scheme.getFlows().getAuthorizationCode() != null) {
                try {
                    AuthenticationInformation config = coreAuthentication.authenticationInformation();
                    scheme.getFlows().getAuthorizationCode().authorizationUrl(config.authorizationEndpoint()).tokenUrl(config.tokenEndpoint());
                } catch (MarmotGraphException e) {
                    logger.warn("Was not able to load the authentication configuration from the MarmotGraph core for the API docs", e);
                }
            }
        };
    }

    @Bean
    public SwaggerIndexTransformer indexPageTransformer(SwaggerUiConfigProperties swaggerUiConfig, SwaggerUiOAuthProperties swaggerUiOAuthProperties, SwaggerWelcomeCommon swaggerWelcomeCommon, ObjectMapperProvider objectMapperProvider, CoreAuthentication coreAuthentication) {
        return new SwaggerIndexPageWithLoginClient(swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider, coreAuthentication);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtUserInfoConverter jwtConverter) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.oauth2ResourceServer(c -> c.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)));
        http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

}
