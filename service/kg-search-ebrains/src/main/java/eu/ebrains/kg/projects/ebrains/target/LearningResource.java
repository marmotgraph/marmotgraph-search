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

import java.util.List;

@Getter
@Setter
@MetaInfo(name = "Learning Resource", searchable = true)
public class LearningResource implements TargetInstance {

    @JsonIgnore
    private List<String> allIdentifiers;

    @ElasticSearchInfo(type = "keyword")
    private final Value<String> type = new Value<>("LearningResource");

    @FieldInfo(ignoreForSearch = true, visible = false)
    private String id;

    @ElasticSearchInfo(type = "keyword")
    private List<String> badges;

    @ElasticSearchInfo(type = "keyword")
    @FieldInfo(ignoreForSearch = true, visible = false)
    private List<String> identifier;

    @FieldInfo(ignoreForSearch = true, visible = false)
    private SchemaOrgInstance meta;

    @ElasticSearchInfo(type = "keyword")
    private Value<String> category;

    @ElasticSearchInfo(type = "keyword")
    private Value<String> disclaimer;

    @FieldInfo(label = "Name", layout = "header")
    private Value<String> title;

    @FieldInfo(label = "About")
    private List<TargetInternalReference> about;

    @FieldInfo(label = "Topic")
    private Value<String> topic;

    @FieldInfo(label = "Authors", separator = "; ", layout = "header", type = FieldInfo.Type.TEXT, boost = 10, labelHidden = true, useForSuggestion = true)
    private List<TargetInternalReference> authors;

    @FieldInfo(label = "Custodians", separator = "; ", hint = "A custodian is the person responsible for the learning resource.", boost = 10)
    private List<TargetInternalReference> custodians;

    @FieldInfo(label = "Developers", separator = "; ", boost = 10)
    private List<TargetInternalReference> editors;

    @FieldInfo(label = "Publishers", separator = "; ", boost = 10)
    private List<TargetInternalReference> publishers;

    @FieldInfo(label = "Description", labelHidden = true, fieldType = FieldInfo.FieldType.MARKDOWN, boost = 2, overview = true)
    private Value<String> description;

    @FieldInfo(label = "Learning outcome", fieldType = FieldInfo.FieldType.MARKDOWN, boost = 2)
    private Value<String> learningOutcome;

    @FieldInfo(label = "Prerequisites", fieldType = FieldInfo.FieldType.MARKDOWN, boost = 2)
    private Value<String> prerequisites;

    @FieldInfo(layout = "Publications", fieldType = FieldInfo.FieldType.MARKDOWN, label = "Publications")
    private List<Value<String>> publications;

    @FieldInfo(label = "Educational level", layout = "summary", useForSuggestion = true)
    private List<TargetInternalReference> educationalLevel;

    @FieldInfo(label = "Required time", layout = "summary", useForSuggestion = true)
    private Value<String> requiredTime;

    @FieldInfo(label = "Keywords", layout = "summary", useForSuggestion = true)
    private List<Value<String>> keywords;

    @FieldInfo(label = "Publication date", layout = "summary", useForSuggestion = true)
    private Value<String> publicationDate;

    @JsonProperty("first_release")
    @FieldInfo(label = "First release", ignoreForSearch = true, visible = false, type = FieldInfo.Type.DATE)
    private ISODateValue firstRelease;

    @JsonProperty("last_release")
    @FieldInfo(label = "Last release", ignoreForSearch = true, visible = false, type = FieldInfo.Type.DATE)
    private ISODateValue lastRelease;

    @ElasticSearchInfo(type = "keyword")
    @FieldInfo(ignoreForSearch = true, visible = false)
    private Value<String> releasedDateForSorting;


    @Override
    @JsonIgnore
    public boolean isSearchableInstance() {
        return true;
    }

}
