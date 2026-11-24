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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.marmotgraph.sdk.MarmotGraphException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;

/**
 * Replaces springdoc's transformer of the Swagger UI index page to provide the login client advertised by the
 * MarmotGraph core as OAuth client id - unless "springdoc.swagger-ui.oauth.client-id" is set explicitly. If the core
 * isn't reachable, the page is rendered without client id and the lookup is retried on the next request.
 */
public class SwaggerIndexPageWithLoginClient extends SwaggerIndexPageTransformer {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerIndexPageWithLoginClient.class);

    private final CoreAuthentication coreAuthentication;

    public SwaggerIndexPageWithLoginClient(SwaggerUiConfigProperties swaggerUiConfig, SwaggerUiOAuthProperties swaggerUiOAuthProperties, SwaggerWelcomeCommon swaggerWelcomeCommon, ObjectMapperProvider objectMapperProvider, CoreAuthentication coreAuthentication) {
        super(swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider);
        this.coreAuthentication = coreAuthentication;
    }

    @Override
    protected String addInitOauth(String html) throws JsonProcessingException {
        if (swaggerUiOAuthProperties.getClientId() == null) {
            try {
                swaggerUiOAuthProperties.setClientId(coreAuthentication.authenticationInformation().loginClientId());
            } catch (MarmotGraphException e) {
                logger.warn("Was not able to load the login client from the MarmotGraph core for the Swagger UI", e);
            }
        }
        return super.addInitOauth(html);
    }
}
