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
import eu.ebrains.kg.common.model.target.Children;
import eu.ebrains.kg.common.model.target.TargetInternalReference;
import eu.ebrains.kg.common.model.target.Value;
import eu.ebrains.kg.common.utils.IdUtils;
import eu.ebrains.kg.common.utils.TranslationException;
import eu.ebrains.kg.common.utils.TranslatorUtils;
import eu.ebrains.kg.projects.ebrains.EBRAINSTranslatorUtils;
import eu.ebrains.kg.projects.ebrains.source.LivePaperV3;
import eu.ebrains.kg.projects.ebrains.source.commons.Version;
import eu.ebrains.kg.projects.ebrains.target.LivePaper;
import eu.ebrains.kg.projects.ebrains.translators.commons.EBRAINSTranslator;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LivePaperTranslator extends EBRAINSTranslator<LivePaperV3, LivePaper, LivePaperTranslator.Result> {
    public static class Result extends ResultsOfKG<LivePaperV3> {
    }

    @Override
    public Class<LivePaperV3> getSourceType() {
        return LivePaperV3.class;
    }

    @Override
    public Class<LivePaper> getTargetType() {
        return LivePaper.class;
    }

    @Override
    public Class<Result> getResultType() {
        return Result.class;
    }

    @Override
    public List<String> getQueryIds() {
        return Collections.singletonList("4b38f7fb-5312-4aa3-86c3-d610babbc899");
    }

    @Override
    public List<String> semanticTypes() {
        return Collections.singletonList("https://openminds.ebrains.eu/publications/LivePaper");
    }

    public LivePaper translate(LivePaperV3 livePaper, DataStage dataStage, boolean liveMode, TranslatorUtils translatorUtils) throws TranslationException {
        LivePaper lp = new LivePaper();

        lp.setCategory(new Value<>("Live Paper Overview"));
        lp.setDisclaimer(new Value<>("Please alert us at [curation-support@ebrains.eu](mailto:curation-support@ebrains.eu) for errors or quality concerns regarding the live paper, so we can forward this information to the custodian responsible."));

        List<Version> sortedVersions = EBRAINSTranslatorUtils.sort(livePaper.getVersions(), translatorUtils.getErrors());
        List<Children<LivePaper.Version>> livePaperVersions = sortedVersions.stream().map(v -> {
            LivePaper.Version version = new LivePaper.Version();
            version.setVersion(new TargetInternalReference(IdUtils.getUUID(v.getId()), v.getVersionIdentifier()));
            version.setInnovation(v.getVersionInnovation() != null ? new Value<>(v.getVersionInnovation()) : null);
            return new Children<>(version);
        }).collect(Collectors.toList());
        lp.setLivePaperVersions(livePaperVersions);
        lp.setId(IdUtils.getUUID(livePaper.getId()));
        lp.setAllIdentifiers(livePaper.getIdentifier());
        lp.setIdentifier(IdUtils.getUUID(livePaper.getIdentifier()).stream().distinct().collect(Collectors.toList()));
        lp.setDescription(livePaper.getDescription());
        lp.setTitle(livePaper.getTitle());
        if (!CollectionUtils.isEmpty(livePaper.getAuthor())) {
            lp.setAuthors(livePaper.getAuthor().stream()
                    .map(a -> new TargetInternalReference(
                            IdUtils.getUUID(a.getId()),
                            EBRAINSTranslatorUtils.getFullName(a.getFullName(), a.getFamilyName(), a.getGivenName())
                    )).collect(Collectors.toList()));
        }
        if (!CollectionUtils.isEmpty(livePaper.getCustodian())) {
            lp.setCustodians(livePaper.getCustodian().stream()
                    .map(a -> new TargetInternalReference(
                            IdUtils.getUUID(a.getId()),
                            EBRAINSTranslatorUtils.getFullName(a.getFullName(), a.getFamilyName(), a.getGivenName())
                    )).collect(Collectors.toList()));
        }

        handleCitation(livePaper, lp);
        if(lp.getCitation()!=null){
            lp.setCitationHint(value("Using this citation allows you to reference all versions of this live paper with one citation.\nUsage of version specific live paper and metadata should be acknowledged by citing the individual live paper version."));
        }
        return lp;
    }
}
