/*
 *  Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *  Copyright 2021 - 2024 EBRAINS AISBL
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
 *  limitations under the License.
 *
 *  This open source software code was developed in part or in whole in the
 *  Human Brain Project, funded from the European Union's Horizon 2020
 *  Framework Programme for Research and Innovation under
 *  Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 *  (Human Brain Project SGA1, SGA2 and SGA3).
 */

package eu.ebrains.kg.projects.tefHealth.source;

import eu.ebrains.kg.common.model.source.SourceInstance;
import eu.ebrains.kg.projects.tefHealth.source.models.NameRef;
import eu.ebrains.kg.projects.tefHealth.source.models.NameRefWithAbbreviation;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ServiceFromKG extends SourceInstance {

    private String name;
    private String description;
    private String serviceInput;
    private String serviceOutput;
    private Boolean externalService;
    private List<NameRef> calls;
    private List<NameRef> category;
    private List<NameRef> serviceStandards;
    private List<NameRef> certificationSupport;
    private List<NameRef> dependenciesAndRestrictions;
    private List<NameRef> offerings;
    private PricingInformation pricing;
    private Provider provider;



    @Getter
    @Setter
    public static class PricingInformation {
        private String billing;
        private String fullPriceInEuro;
        private String reducedPriceInEuro;
        private String pricingDetails;
    }

    @Getter
    @Setter
    public static class Provider {
        private NameRefWithAbbreviation organization;
        private List<String> contacts;
    }

}
