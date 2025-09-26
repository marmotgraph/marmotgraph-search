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
import eu.ebrains.kg.common.model.target.TargetExternalReference;
import eu.ebrains.kg.common.model.target.TargetInternalReference;
import eu.ebrains.kg.common.model.target.Value;
import eu.ebrains.kg.common.utils.IdUtils;
import eu.ebrains.kg.common.utils.TranslationException;
import eu.ebrains.kg.common.utils.TranslatorUtils;
import eu.ebrains.kg.projects.ebrains.EBRAINSTranslatorUtils;
import eu.ebrains.kg.projects.ebrains.source.LivePaperVersionV3;
import eu.ebrains.kg.projects.ebrains.source.commons.Version;
import eu.ebrains.kg.projects.ebrains.target.LivePaperVersion;
import eu.ebrains.kg.projects.ebrains.translators.commons.Constants;
import eu.ebrains.kg.projects.ebrains.translators.commons.EBRAINSTranslator;
import eu.ebrains.kg.projects.ebrains.translators.utils.MetaBadgeUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

public class LivePaperVersionTranslator extends EBRAINSTranslator<LivePaperVersionV3, LivePaperVersion, LivePaperVersionTranslator.Result> {

    public static class Result extends ResultsOfKG<LivePaperVersionV3> {
    }

    @Override
    public Class<LivePaperVersionV3> getSourceType() {
        return LivePaperVersionV3.class;
    }

    @Override
    public Class<LivePaperVersion> getTargetType() {
        return LivePaperVersion.class;
    }

    @Override
    public Class<Result> getResultType() {
        return Result.class;
    }

    @Override
    public List<String> getQueryIds() {
        return Collections.singletonList("5ab9cf1f-19ac-49b1-b189-f4ed84da335c");
    }

    @Override
    public List<String> semanticTypes() {
        return Collections.singletonList("https://openminds.ebrains.eu/publications/LivePaperVersion");
    }

    public LivePaperVersion translate(LivePaperVersionV3 livePaperVersion, DataStage dataStage, boolean liveMode, TranslatorUtils translatorUtils) throws TranslationException {
        LivePaperVersion lp = new LivePaperVersion();

        lp.setCategory(new Value<>("Live Paper"));
        lp.setDisclaimer(new Value<>("Please alert us at [curation-support@ebrains.eu](mailto:curation-support@ebrains.eu) for errors or quality concerns regarding the live paper, so we can forward this information to the custodian responsible."));

        LivePaperVersionV3.LivePaperVersions livePaper = livePaperVersion.getLivePaper();
        lp.setId(IdUtils.getUUID(livePaperVersion.getId()));
        final Date releaseDate = livePaperVersion.getReleaseDate() != null && livePaperVersion.getReleaseDate().before(new Date()) ? livePaperVersion.getReleaseDate() : livePaperVersion.getFirstReleasedAt();
        final String releaseDateForSorting = translatorUtils.getReleasedDateForSorting(livePaperVersion.getIssueDate(), releaseDate);
        lp.setFirstRelease(value(releaseDate));
        lp.setLastRelease(value(livePaperVersion.getLastReleasedAt()));
        lp.setReleasedAt(value(releaseDateForSorting != null ? releaseDateForSorting.split("T")[0] : null));
        lp.setReleasedDateForSorting(value(releaseDateForSorting));
        lp.setAllIdentifiers(livePaperVersion.getIdentifier());
        lp.setIdentifier(IdUtils.getUUID(livePaperVersion.getIdentifier()).stream().distinct().collect(Collectors.toList()));

        List<Version> versions = livePaper == null ? null : livePaper.getVersions();
        boolean hasMultipleVersions = !CollectionUtils.isEmpty(versions) && versions.size() > 1;
        if (!CollectionUtils.isEmpty(versions) && versions.size() > 1) {
            lp.setVersion(livePaperVersion.getVersion());
            List<Version> sortedVersions = EBRAINSTranslatorUtils.sort(versions, translatorUtils.getErrors());
            List<TargetInternalReference> references = sortedVersions.stream().map(v -> new TargetInternalReference(IdUtils.getUUID(v.getId()), v.getVersionIdentifier())).collect(Collectors.toList());
            references.add(new TargetInternalReference(IdUtils.getUUID(livePaper.getId()), "version overview"));
            lp.setVersions(references);
            lp.setSearchable(sortedVersions.get(sortedVersions.size() - 1).getId().equals(livePaperVersion.getId()));
        } else {
            lp.setSearchable(true);
        }

        // title
        if (StringUtils.isNotBlank(livePaperVersion.getFullName())) {
            if (hasMultipleVersions || StringUtils.isBlank(livePaperVersion.getVersion())) {
                lp.setTitle(value(livePaperVersion.getFullName()));
            } else {
                lp.setTitle(value(String.format("%s (%s)", livePaperVersion.getFullName(), livePaperVersion.getVersion())));
            }
        } else if (livePaper != null && StringUtils.isNotBlank(livePaper.getFullName())) {
            if (hasMultipleVersions || StringUtils.isBlank(livePaperVersion.getVersion())) {
                lp.setTitle(value(livePaper.getFullName()));
            } else {
                lp.setTitle(value(String.format("%s (%s)", livePaper.getFullName(), livePaperVersion.getVersion())));
            }
        }

        // developers
        if (!CollectionUtils.isEmpty(livePaperVersion.getAuthor())) {
            lp.setAuthors(livePaperVersion.getAuthor().stream()
                    .map(a -> new TargetInternalReference(
                            IdUtils.getUUID(a.getId()),
                            EBRAINSTranslatorUtils.getFullName(a.getFullName(), a.getFamilyName(), a.getGivenName())
                    )).collect(Collectors.toList()));
        } else if (livePaper != null && !CollectionUtils.isEmpty(livePaper.getAuthor())) {
            lp.setAuthors(livePaper.getAuthor().stream()
                    .map(a -> new TargetInternalReference(
                            IdUtils.getUUID(a.getId()),
                            EBRAINSTranslatorUtils.getFullName(a.getFullName(), a.getFamilyName(), a.getGivenName())
                    )).collect(Collectors.toList()));
        }

        handleCitation(livePaperVersion, lp);

        if (!CollectionUtils.isEmpty(livePaperVersion.getLicense())) {
            lp.setLicense(livePaperVersion.getLicense().stream().map(l -> new TargetExternalReference(l.getUrl(), l.getLabel())).collect(Collectors.toList()));
            lp.setLicenseForFilter(livePaperVersion.getLicense().stream().map(l -> new Value<>(l.getShortName())).collect(Collectors.toList()));
        }

        if (livePaperVersion.getCopyright() != null) {
            final String copyrightHolders = livePaperVersion.getCopyright().getHolder().stream().map(h -> EBRAINSTranslatorUtils.getFullName(h.getFullName(), h.getFamilyName(), h.getGivenName())).filter(Objects::nonNull).collect(Collectors.joining(", "));
            lp.setCopyright(new Value<>(String.format("%s %s", livePaperVersion.getCopyright().getYear(), copyrightHolders)));
        }

        List<TargetInternalReference> projects = new ArrayList<>();
        if (!CollectionUtils.isEmpty(livePaperVersion.getProjects())) {
            projects.addAll(livePaperVersion.getProjects().stream().map(this::ref).toList());
        }
        if (livePaper != null && !CollectionUtils.isEmpty(livePaper.getProjects())) {
            projects.addAll(livePaper.getProjects().stream().map(p -> new TargetInternalReference(IdUtils.getUUID(p.getId()), p.getFullName())).filter(p -> !projects.contains(p)).toList());
        }
        if (!CollectionUtils.isEmpty(projects)) {
            lp.setProjects(projects);
        }

        if (!CollectionUtils.isEmpty(livePaperVersion.getCustodian())) {
            lp.setCustodians(livePaperVersion.getCustodian().stream()
                    .map(a -> new TargetInternalReference(
                            IdUtils.getUUID(a.getId()),
                            EBRAINSTranslatorUtils.getFullName(a.getFullName(), a.getFamilyName(), a.getGivenName())
                    )).collect(Collectors.toList()));
        } else if (livePaper != null && !CollectionUtils.isEmpty(livePaper.getCustodian())) {
            lp.setCustodians(livePaper.getCustodian().stream()
                    .map(a -> new TargetInternalReference(
                            IdUtils.getUUID(a.getId()),
                            EBRAINSTranslatorUtils.getFullName(a.getFullName(), a.getFamilyName(), a.getGivenName())
                    )).collect(Collectors.toList()));
        }


        if (StringUtils.isNotBlank(livePaperVersion.getDescription())) {
            lp.setDescription(value(livePaperVersion.getDescription()));
        } else if (livePaper != null) {
            lp.setDescription(value(livePaper.getDescription()));
        }

        if (StringUtils.isNotBlank(livePaperVersion.getVersionInnovation()) && !Constants.VERSION_INNOVATION_DEFAULTS.contains(StringUtils.trim(livePaperVersion.getVersionInnovation()).toLowerCase())) {
            lp.setNewInThisVersion(new Value<>(livePaperVersion.getVersionInnovation()));
        }

        if (!CollectionUtils.isEmpty(livePaperVersion.getPublications())) {
            lp.setPublications(livePaperVersion.getPublications().stream().map(p -> EBRAINSTranslatorUtils.getFormattedDigitalIdentifier(translatorUtils.getDoiCitationFormatter(), p.getIdentifier(), p.resolvedType())).filter(Objects::nonNull).map(Value::new).collect(Collectors.toList()));
        }

        if (livePaperVersion.getHomepage() != null) {
            lp.setHomepage(new TargetExternalReference(livePaperVersion.getHomepage(), livePaperVersion.getHomepage()));
        } else if (livePaper != null && livePaper.getHomepage() != null) {
            lp.setHomepage(new TargetExternalReference(livePaper.getHomepage(), livePaper.getHomepage()));
        }

        List<TargetExternalReference> documentationElements = new ArrayList<>();
        if (livePaperVersion.getDocumentationDOI() != null) {
            documentationElements.add(new TargetExternalReference(livePaperVersion.getDocumentationDOI(), livePaperVersion.getDocumentationDOI()));
        }
        if (livePaperVersion.getDocumentationURL() != null) {
            documentationElements.add(new TargetExternalReference(livePaperVersion.getDocumentationURL(), livePaperVersion.getDocumentationURL()));
        }
        if (livePaperVersion.getDocumentationWebResource() != null) {
            documentationElements.add(new TargetExternalReference(livePaperVersion.getDocumentationWebResource(), livePaperVersion.getDocumentationWebResource()));
        }
        if (livePaperVersion.getDocumentationFile() != null) {
            //TODO make this a little bit prettier (maybe just show the relative file name or similar)
            documentationElements.add(new TargetExternalReference(livePaperVersion.getDocumentationFile(), livePaperVersion.getDocumentationFile()));
        }
        if (!documentationElements.isEmpty()) {
            lp.setDocumentation(documentationElements);
        }
        if (!CollectionUtils.isEmpty(livePaperVersion.getSupportChannel())) {
            final List<TargetExternalReference> links = livePaperVersion.getSupportChannel().stream().filter(channel -> channel.startsWith("http")).
                    map(url -> new TargetExternalReference(url, url)).collect(Collectors.toList());
            if (links.isEmpty()) {
                //Decision from Oct 2th 2021: we only show e-mail addresses if there are no links available
                final List<TargetExternalReference> emailAddresses = livePaperVersion.getSupportChannel().stream().filter(channel -> channel.contains("@")).map(email -> new TargetExternalReference(String.format("mailto:%s", email), email)).collect(Collectors.toList());
                if (!emailAddresses.isEmpty()) {
                    lp.setSupport(emailAddresses);
                }
            } else {
                lp.setSupport(links);
            }
        }

        translatorUtils.defineBadgesAndTrendingState(lp, livePaperVersion.getIssueDate(), releaseDate, livePaperVersion.getLast30DaysViews(), MetaBadgeUtils.evaluateMetaBadgeUtils(livePaperVersion, false, false));
        lp.setLearningResources(ref(livePaperVersion.getLearningResource()));
        return lp;
    }

}
