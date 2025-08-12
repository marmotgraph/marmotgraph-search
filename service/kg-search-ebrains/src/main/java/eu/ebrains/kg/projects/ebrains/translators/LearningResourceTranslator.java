
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

package eu.ebrains.kg.projects.ebrains.translators;

import eu.ebrains.kg.common.model.DataStage;
import eu.ebrains.kg.common.model.source.ResultsOfKG;
import eu.ebrains.kg.common.model.target.Value;
import eu.ebrains.kg.common.utils.IdUtils;
import eu.ebrains.kg.common.utils.TranslationException;
import eu.ebrains.kg.common.utils.TranslatorUtils;
import eu.ebrains.kg.projects.ebrains.EBRAINSTranslatorUtils;
import eu.ebrains.kg.projects.ebrains.source.LearningResourceV3;
import eu.ebrains.kg.projects.ebrains.target.LearningResource;
import eu.ebrains.kg.projects.ebrains.translators.commons.EBRAINSTranslator;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

public class LearningResourceTranslator extends EBRAINSTranslator<LearningResourceV3, LearningResource, LearningResourceTranslator.Result> {

    public static class Result extends ResultsOfKG<LearningResourceV3> {
    }

    @Override
    public Class<LearningResourceV3> getSourceType() {
        return LearningResourceV3.class;
    }

    @Override
    public Class<LearningResource> getTargetType() {
        return LearningResource.class;
    }

    @Override
    public Class<Result> getResultType() {
        return Result.class;
    }

    @Override
    public List<String> getQueryIds() {
        return Collections.singletonList("7285f05c-6626-4295-b464-9fb6070f58cc");
    }

    @Override
    public List<String> semanticTypes() {
        return Collections.singletonList("https://openminds.ebrains.eu/publications/LearningResource");
    }

    public LearningResource translate(LearningResourceV3 source, DataStage dataStage, boolean liveMode, TranslatorUtils translatorUtils) throws TranslationException {
        LearningResource target = new LearningResource();

        target.setCategory(new Value<>("Learning resource"));
        target.setDisclaimer(new Value<>("Please alert us at [curation-support@ebrains.eu](mailto:curation-support@ebrains.eu) for errors or quality concerns regarding the learning resource, so we can forward this information to the custodian responsible."));
        target.setIdentifier(IdUtils.getUUID(source.getIdentifier()).stream().distinct().collect(Collectors.toList()));
        target.setId(IdUtils.getUUID(source.getId()));
        final Date releaseDate = source.getReleaseDate() != null && source.getReleaseDate().before(new Date()) ? source.getReleaseDate() : source.getFirstReleasedAt();
        final String releaseDateForSorting = translatorUtils.getReleasedDateForSorting(null, releaseDate);
        target.setFirstRelease(value(releaseDate));
        target.setLastRelease(value(source.getLastReleasedAt()));
        target.setReleasedDateForSorting(value(releaseDateForSorting));
        target.setAllIdentifiers(source.getIdentifier());
        target.setBadges(source.getLearningResourceType());
        target.setTitle(value(source.getName()));
        target.setDescription(value(source.getAbstractText()));
        target.setAbout(refVersion(source.getAbout(), true));
        target.setTopic(value(source.getTopic()));
        target.setAuthors(EBRAINSTranslatorUtils.refPerson(source.getAuthor()));
        target.setCustodians(EBRAINSTranslatorUtils.refPerson(source.getCustodian()));
        target.setEditors(EBRAINSTranslatorUtils.refPerson(source.getEditor()));
        target.setPublishers(EBRAINSTranslatorUtils.refPerson(source.getPublisher()));
        target.setLearningOutcome(value(source.getLearningOutcome()));
        target.setPrerequisites(value(source.getPrerequisite()));

        if(!CollectionUtils.isEmpty(source.getPublications())){
            target.setPublications(source.getPublications().stream().map(p -> EBRAINSTranslatorUtils.getFormattedDigitalIdentifier(translatorUtils.getDoiCitationFormatter(), p.getIdentifier(), p.resolvedType())).filter(Objects::nonNull).map(Value::new).collect(Collectors.toList()));
        }

        target.setEducationalLevel(ref(source.getEducationalLevel()));
        if(source.getRequiredTime()!=null) {
            target.setRequiredTime(value(source.getRequiredTime().displayString()));
        }
        target.setKeywords(value(source.getKeyword()));
        target.setPublicationDate(value(source.getPublicationDate()));
        return target;
    }
}
