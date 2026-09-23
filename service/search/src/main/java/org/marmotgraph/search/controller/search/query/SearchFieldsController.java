/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2024 EBRAINS AISBL
 * Copyright 2024 - 2026 ETH Zurich
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

package org.marmotgraph.search.controller.search.query;

import org.apache.commons.lang3.StringUtils;
import org.marmotgraph.search.common.controller.translation.models.TranslatorModel;
import org.marmotgraph.search.common.model.target.FieldInfo;
import org.marmotgraph.search.common.model.target.Value;
import org.marmotgraph.search.common.utils.MetaModelUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@SuppressWarnings("java:S1452") // we keep the generics intentionally
public class SearchFieldsController {
    private final Set<String> FIELDS_TO_HIGHLIGHT = Stream.of(
            "description.value",
            "contributors.value",
            "custodians.value",
            "owners.value",
            "component.value",
            "created_at.value",
            "releasedate.value",
            "activities.value"
    ).collect(Collectors.toSet());

    private final MetaModelUtils utils;

    public SearchFieldsController(MetaModelUtils utils) {
        this.utils = utils;
    }


    //@Cacheable(value = "highlight", key = "#type")
    public List<String> getFieldsHighlight(Type type) {
        List<MetaModelUtils.FieldWithGenericTypeInfo> allFields = utils.getAllFields(type);
        return allFields.stream().map(f -> {
            try {
                return fieldHighlight(f, "");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }).filter(Objects::nonNull).toList();
    }

    private String fieldHighlight(MetaModelUtils.FieldWithGenericTypeInfo f, String parentPath) throws ClassNotFoundException {
        FieldInfo info = f.field().getAnnotation(FieldInfo.class);
        if (info != null && !info.ignoreForSearch()) {
            String propertyName = utils.getPropertyName(f.field());
            String path = String.format("%s%s", parentPath, propertyName);
            if (!propertyName.equals("children")) { // if (f.getField().getType() != Children.class) { if (f.getField().getDeclaringClass() != Children.class) {
                if (StringUtils.isBlank(parentPath) || f.field().getType() == Value.class) {
                    return String.format("%s.value", path);
                } else {
                    return path;
                }
            }
//          Type topTypeToHandle = f.getGenericType() != null ? f.getGenericType() : MetaModelUtils.getTopTypeToHandle(f.getField().getGenericType());
//          addChildrenFieldHighlight(highlights, topTypeToHandle, String.format("%s.children", path));
        }
        return null;
    }


    public Optional<TranslatorModel> getTranslatorModelByName(String humanReadableName){
        return utils.getTranslatorModels().stream().filter(m -> MetaModelUtils.getNameForClass(m.targetClass()).equals(humanReadableName)).findAny();
    }

    public List<String> getEsQueryFields(Type type) {
        return reflectFields(type, null).entrySet().stream().map(e -> {
            String field = e.getKey();
            double boost = e.getValue() == null ? 1.0 : e.getValue();
            return String.format("%s^%d", field, (int) boost);
        }).sorted().collect(Collectors.toList());
    }

    private Map<String, Double> reflectFields(Type type, Predicate<FieldInfo> filter) {
        Map<String, Double> boosts = new HashMap<>();
        List<MetaModelUtils.FieldWithGenericTypeInfo> allFields = utils.getAllFields(type);
        allFields.forEach(f -> {
            try {
                reflectFields(boosts, f, "", filter);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
        return boosts;
    }

    private void reflectFields(Map<String, Double> fieldsWithBoost, MetaModelUtils.FieldWithGenericTypeInfo f, String parentPath, Predicate<FieldInfo> filter) throws ClassNotFoundException {
        FieldInfo info = f.field().getAnnotation(FieldInfo.class);
        if (info != null && !info.ignoreForSearch()) {
            String propertyName = utils.getPropertyName(f.field());
            String path = String.format("%s%s", parentPath, propertyName);
            if ((filter == null || filter.test(info)) && !propertyName.equals("children")) { // if (f.getField().getType() != Children.class) { if (f.getField().getDeclaringClass() != Children.class) {
                if (StringUtils.isBlank(parentPath) || f.field().getType() == Value.class) {
                    String valuePath = String.format("%s.value", path);
                    fieldsWithBoost.put(valuePath, info.boost());
                } else {
                    fieldsWithBoost.put(path, info.boost());
                }
            }
            Type topTypeToHandle = f.genericType() != null ? f.genericType() : MetaModelUtils.getTopTypeToHandle(f.field().getGenericType());
            reflectChildFields(fieldsWithBoost, topTypeToHandle, String.format("%s.", path), filter);
        }
    }

    private void reflectChildFields(Map<String, Double> boosts, Type type, String parentPath, Predicate<FieldInfo> filter) {
        List<MetaModelUtils.FieldWithGenericTypeInfo> allFields = utils.getAllFields(type);
        allFields.forEach(field -> {
            try {
                reflectFields(boosts, field, parentPath, filter);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
