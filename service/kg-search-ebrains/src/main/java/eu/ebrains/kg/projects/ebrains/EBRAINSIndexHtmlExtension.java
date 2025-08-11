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

package eu.ebrains.kg.projects.ebrains;

import eu.ebrains.kg.common.controller.translation.IndexHtmlExtension;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Profile("ebrains")
public class EBRAINSIndexHtmlExtension implements IndexHtmlExtension {

    @Override
    public String getHeaderAdditions() {
        return """
                <title>EBRAINS - Knowledge Graph Search</title>
                <meta name="og:title" content="EBRAINS">
                <meta name="og:image" content="/static/img/apple-touch-icon.png">
                <link rel="apple-touch-icon" href="/static/img/apple-touch-icon.png" sizes="180x180">
                <link rel="icon" type="image/png" href="/static/img/favicon-32x32.png" sizes="32x32">
                <link rel="icon" type="image/png" href="/static/img/favicon-16x16.png" sizes="16x16">
                <link rel="mask-icon" href="/static/img/safari-pinned-tab.svg" color="#111111">
                <meta name="theme-color" content="#e6e6e6">
                <meta name="description" content="EBRAINS offers advanced tools and resources for brain research, allowing scientists to study the brain at various scales. Join us in better understanding the brain's complexity.">
                <meta name="og:url" content="https://www.ebrains.eu/">
                <meta name="og:description" content="EBRAINS offers advanced tools and resources for brain research, allowing scientists to study the brain at various scales. Join us in better understanding the brain's complexity.">
                <meta name="og:type" content="article">
                <meta name="og:site_name" content="EBRAINS">
                <meta name="twitter:site" content="@EBRAINS_eu">
                <meta name="og:image" content="https://www.ebrains.eu/cover.png">
                <meta name="og:image:width" content="1200">
                <meta name="og:image:height" content="630">
                <meta name="twitter:card" content="summary_large_image">
                """;
    }
}
