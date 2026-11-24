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
import org.marmotgraph.search.common.model.DataStage;
import org.marmotgraph.search.common.services.KGServiceClient;
import org.marmotgraph.search.common.utils.queryGenerator.QueryGenerator;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The service has to start even if the MarmotGraph core isn't available - it is only contacted on first use.
 */
class OauthClientTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withAllowBeanDefinitionOverriding(true)
            .withUserConfiguration(OauthClient.class)
            // In production, the default builder reads MARMOTGRAPH_* from the process environment, which a test can't
            // set - so we configure the same builder explicitly. Nothing listens on port 1 -> any connection fails.
            .withBean("marmotGraphClient", MarmotGraphClient.class, () -> MarmotGraphClient.builder("http://127.0.0.1:1").clientId("kg-search").clientSecret("secret").build())
            .withBean(AppConfig.class, () -> new AppConfig(false))
            .withBean(QueryGenerator.class)
            .withBean(KGServiceClient.class)
            .withPropertyValues("MARMOTGRAPH_CLIENT_ID=kg-search");

    @Test
    void contextStartsWithoutTheCore() {
        contextRunner.run(context -> {
            assertNull(context.getStartupFailure());
            assertEquals("http://127.0.0.1:1", context.getBean(MarmotGraphClient.class).baseUrl());
            assertNotNull(context.getBean(KGServiceClient.class));
            assertThrows(MarmotGraphException.class, () -> context.getBean(CoreAuthentication.class).authenticationInformation());
        });
    }

    @Test
    void serviceAccountCallsFailWithoutTheCoreInsteadOfTheStartup() {
        contextRunner.run(context -> {
            RuntimeException e = assertThrows(RuntimeException.class,
                    () -> context.getBean(KGServiceClient.class).getTypeInformation(DataStage.IN_PROGRESS));
            assertInstanceOf(MarmotGraphException.class, e.getCause());
        });
    }

    @Test
    void tokensCanNotBeValidatedWithoutTheCore() {
        contextRunner.run(context -> {
            JwtException e = assertThrows(JwtException.class, () -> context.getBean(JwtDecoder.class).decode("token"));
            assertInstanceOf(MarmotGraphException.class, e.getCause());
        });
    }
}
