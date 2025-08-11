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

package eu.ebrains.kg.projects.tefHealth;

import eu.ebrains.kg.common.controller.translation.IndexHtmlExtension;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Profile("tef-health")
public class TEFHealthIndexHtmlExtension implements IndexHtmlExtension {

    @Override
    public String getHeaderAdditions() {
        return """
                <link rel="apple-touch-icon" sizes="57x57" href="https://tefhealth.eu/files/tef-health/layout/favicon/apple-icon-57x57.png">
                <link rel="apple-touch-icon" sizes="60x60" href="https://tefhealth.eu/files/tef-health/layout/favicon/apple-icon-60x60.png">
                <link rel="apple-touch-icon" sizes="72x72" href="https://tefhealth.eu/files/tef-health/layout/favicon/apple-icon-72x72.png">
                <link rel="apple-touch-icon" sizes="76x76" href="https://tefhealth.eu/files/tef-health/layout/favicon/apple-icon-76x76.png">
                <link rel="icon" type="image/png" sizes="16x16" href="https://tefhealth.eu/files/tef-health/layout/favicon/favicon-16x16.png"><link rel="apple-touch-icon" sizes="114x114" href="https://tefhealth.eu/files/tef-health/layout/favicon/apple-icon-114x114.png">
                <link rel="apple-touch-icon" sizes="120x120" href="https://tefhealth.eu/files/tef-health/layout/favicon/apple-icon-120x120.png">
                <link rel="apple-touch-icon" sizes="144x144" href="https://tefhealth.eu/files/tef-health/layout/favicon/apple-icon-144x144.png">
                <link rel="apple-touch-icon" sizes="152x152" href="https://tefhealth.eu/files/tef-health/layout/favicon/apple-icon-152x152.png">
                <link rel="apple-touch-icon" sizes="180x180" href="https://tefhealth.eu/files/tef-health/layout/favicon/apple-icon-180x180.png">
                <link rel="icon" type="image/png" sizes="192x192" href="https://tefhealth.eu/files/tef-health/layout/favicon/android-icon-192x192.png">
                <link rel="icon" type="image/png" sizes="32x32" href="https://tefhealth.eu/files/tef-health/layout/favicon/favicon-32x32.png">
                <link rel="icon" type="image/png" sizes="96x96" href="https://tefhealth.eu/files/tef-health/layout/favicon/favicon-96x96.png">
                <meta name="msapplication-TileColor" content="#ffffff">
                <meta name="msapplication-TileImage" content="https://tefhealth.eu/files/tef-health/layout/favicon/ms-icon-144x144.png">            
                <title>TEF-Health Service Catalogue</title>
                <meta name="og:title" content="TEF-Health Service Catalogue">
                <meta name="og:image" content="https://tefhealth.eu/files/tef-health/layout/favicon/apple-icon-180x180.png">
                """;
    }
}
