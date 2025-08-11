/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2024 EBRAINS AISBL
 * Copyright 2024 - 2025 ETH Zurich
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

package eu.ebrains.kg.search.controller;

import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Arrays;
import java.util.List;

import static org.springframework.web.servlet.function.RequestPredicates.path;
import static org.springframework.web.servlet.function.RequestPredicates.pathExtension;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@Configuration
public class SPARouting {

    @Bean
    RouterFunction<ServerResponse> spaRouter(SPAController controller) {
        List<String> extensions = Arrays.asList("js", "css", "ico", "png", "jpg", "gif", "html", "svg");
        RequestPredicate spaPredicate = path("/api/**").or(path("/internal/**")).or(path("/sitemap/**")).or(path("/error")).or(pathExtension(extensions::contains)).negate();
        return route(spaPredicate, request -> ServerResponse.ok().contentType(MediaType.TEXT_HTML).body(controller.getIndexHTML()));
    }

    /**
     * Tomcat prevents the use of square brackets in query param - we currently rely on this on the UI though.
     * @return
     */
    //TODO revisit the way we represent filters in the UI
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return (tomcat) -> {
            tomcat.addConnectorCustomizers((connector) ->
                    ((AbstractHttp11Protocol<?>)connector.getProtocolHandler()).setRelaxedQueryChars("[]"));
        };
    }
}
