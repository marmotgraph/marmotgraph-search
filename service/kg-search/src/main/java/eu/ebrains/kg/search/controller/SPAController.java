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

import eu.ebrains.kg.common.controller.translation.IndexHtmlExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class SPAController {

    private final IndexHtmlExtension indexHtmlExtension;
    private final Resource indexHtml;

    public SPAController(IndexHtmlExtension indexHtmlExtension, @Value("classpath:public/index.html") Resource indexHtml) {
        this.indexHtmlExtension = indexHtmlExtension;
        this.indexHtml = indexHtml;
    }

    @Cacheable("index")
    public String getIndexHTML() {
        String payload = null;
        try {
            payload = indexHtml.getContentAsString(StandardCharsets.UTF_8);
            String headerAdditions = indexHtmlExtension.getHeaderAdditions();
            if (headerAdditions != null) {
                headerAdditions = headerAdditions + "</head>";
                payload = payload.replace("</head>", headerAdditions);
            }
            return payload;
        } catch (IOException e) {
            throw new RuntimeException("Was not able to read the index.html from classpath", e);
        }
    }

}
