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

import eu.ebrains.kg.common.controller.translation.TranslatorRegistry;
import eu.ebrains.kg.common.controller.translation.models.TranslatorModel;
import eu.ebrains.kg.projects.tefHealth.target.Country;
import eu.ebrains.kg.projects.tefHealth.target.Organization;
import eu.ebrains.kg.projects.tefHealth.translators.CountryTranslator;
import eu.ebrains.kg.projects.tefHealth.translators.OrganizationTranslator;
import eu.ebrains.kg.projects.tefHealth.target.Service;
import eu.ebrains.kg.projects.tefHealth.translators.ServiceTranslator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@Profile("tef-health")
public class TEFHealthTranslatorRegistry implements TranslatorRegistry {

    private final List<eu.ebrains.kg.common.controller.translation.models.TranslatorModel<?,?>> TRANSLATORS = Arrays.asList(
            new TranslatorModel<>(Service.class, new ServiceTranslator(), false, 1000, true),
            new TranslatorModel<>(Organization.class, new OrganizationTranslator(), false, 1000, true),
            new TranslatorModel<>(Country.class, new CountryTranslator(), false, 1000, true)
    );

    @Override
    public List<TranslatorModel<?, ?>> getTranslators() {
        return TRANSLATORS;
    }

    @Override
    public Class<?> getFileClass() {
        return null;
    }

    @Override
    public Optional<String> getIndexPrefix() {
        return Optional.of("tefhealth");
    }

    @Override
    public String getName() {
        return "TEF-Health";
    }
}
