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

package eu.ebrains.kg.projects.ebrains.target;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.ebrains.kg.common.model.target.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Getter
@Setter
@MetaInfo(name = "LivePaperVersions")
public class LivePaper implements TargetInstance, HasCitation {

    @Setter
    @JsonIgnore
    private List<String> allIdentifiers;

    @Getter
    @ElasticSearchInfo(type = "keyword")
    private final Value<String> type = new Value<>("LivePaperVersions");

    @Setter
    @FieldInfo(ignoreForSearch = true, visible = false)
    private String id;

    @Setter
    @ElasticSearchInfo(type = "keyword")
    @FieldInfo(ignoreForSearch = true, visible = false)
    private List<String> identifier;

    @FieldInfo(ignoreForSearch = true, visible = false)
    private SchemaOrgInstance meta;
    @Setter
    @Getter
    @ElasticSearchInfo(type = "keyword")
    private Value<String> category;

    @Setter
    @Getter
    @ElasticSearchInfo(type = "keyword")
    private Value<String> disclaimer;

    @Getter
    @FieldInfo(label = "Name", layout = "header")
    private Value<String> title;

    @Getter
    @FieldInfo(label = "Description", labelHidden = true, fieldType = FieldInfo.FieldType.MARKDOWN, boost = 2, overview = true)
    private Value<String> description;

    @Setter
    @Getter
    @FieldInfo(label = "Custodians", separator = "; ", hint = "A custodian is the person responsible for the data bundle.", boost = 10)
    private List<TargetInternalReference> custodians;

    @Setter
    @Getter
    @FieldInfo(label = "Authors", separator = "; ", boost = 10)
    private List<TargetInternalReference> authors;

    @Getter
    @FieldInfo(layout = "How to cite", labelHidden = true, fieldType = FieldInfo.FieldType.CITATION)
    private Value<String> citation;

    @FieldInfo(layout = "How to cite", labelHidden = true, fieldType = FieldInfo.FieldType.CITATION)
    private Value<String> customCitation;

    @FieldInfo(layout = "How to cite", labelHidden = true)
    private Value<String> citationHint;

    @Getter
    @FieldInfo(label = "DOI", hint = "This is the software DOI representing all the underlying software's versions you must cite if you reuse this data in a way that leads to a publication")
    private Value<String> doi;

    @Setter
    @Getter
    @FieldInfo(label = "Live paper versions", fieldType = FieldInfo.FieldType.TABLE, layout = "Live paper versions")
    private List<Children<Version>> livePaperVersions;

    @Setter
    @Getter
    @JsonProperty("first_release")
    @FieldInfo(label = "First release", ignoreForSearch = true, visible = false, type = FieldInfo.Type.DATE)
    private ISODateValue firstRelease;

    @Setter
    @Getter
    @JsonProperty("last_release")
    @FieldInfo(label = "Last release", ignoreForSearch = true, visible = false, type = FieldInfo.Type.DATE)
    private ISODateValue lastRelease;


    @Override
    @JsonIgnore
    public boolean isSearchableInstance() {
        return false;
    }

    public void setTitle(Value<String> title){
        this.title = title;
    }

    public void setTitle(String title) {
        setTitle(StringUtils.isBlank(title) ? null : new Value<>(title));
    }

    public void setDescription(String description) {
        setDescription(StringUtils.isBlank(description) ? null : new Value<>(description));
    }

    public void setDescription(Value<String> description){
        this.description = description;
    }

    public void setCitation(String citation) {
        setCitation(StringUtils.isBlank(citation) ? null : new Value<>(citation));
    }

    public void setCitation(Value<String> citation){
        this.citation = citation;
    }

    public void setDoi(String doi) {
        setDoi(StringUtils.isBlank(doi) ? null : new Value<>(doi));
    }

    public void setDoi(Value<String> doi){
        this.doi = doi;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public List<String> getIdentifier() {
        return identifier;
    }

    @Override
    public List<String> getAllIdentifiers() {
        return allIdentifiers;
    }

    @Setter
    @Getter
    public static class Version {
        @FieldInfo(label = "Version")
        private TargetInternalReference version;

        @FieldInfo(label = "Innovation", fieldType = FieldInfo.FieldType.MARKDOWN)
        private Value<String> innovation;


    }
}
