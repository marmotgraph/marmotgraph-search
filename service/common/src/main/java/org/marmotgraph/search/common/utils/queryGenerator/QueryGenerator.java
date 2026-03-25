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

package org.marmotgraph.search.common.utils.queryGenerator;

import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

@Component
public class QueryGenerator {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;

        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }


    private List<MarmotGraphQuery.Path> evaluatePath(Field field, MergedAnnotation<Query.Field> annotation, String defaultPropertyNamespace, String defaultTypeNamespace){
        if(annotation.isPresent()) {
            MergedAnnotation<Query.PathElement>[] path = annotation.getAnnotationArray("path", Query.PathElement.class);
            if(path.length > 0) {
                return Arrays.stream(path).map(p -> {
                    MarmotGraphQuery.Path result = new MarmotGraphQuery.Path();
                    String propertyNamespace = p.getString("namespace");
                    Optional<Boolean> reverse = p.getValue("reverse", Boolean.class);
                    if(reverse.isPresent() && reverse.get()) {
                        result.setReverse(true);
                    }
                    String[] typeFilters = p.getStringArray("typeFilter");
                    if(typeFilters.length > 0) {
                        result.setTypeFilter(Arrays.stream(typeFilters).map(t ->{
                            MarmotGraphQuery.Ref ref = new MarmotGraphQuery.Ref();
                            ref.setId(qualifyType(t, defaultTypeNamespace));
                            return ref;
                        }).toList());
                    }
                    String name = p.getString("name");
                    if(StringUtils.isBlank(name)){
                        name = field.getName();
                    }
                    result.setId(qualifyProperty(name, StringUtils.isBlank(propertyNamespace) ? defaultPropertyNamespace : propertyNamespace));
                    return result;
                }).toList();
            }
        }
        MarmotGraphQuery.Path path = new MarmotGraphQuery.Path();
        path.setId(qualifyProperty(field.getName(), defaultPropertyNamespace));
        return Collections.singletonList(path);

    }

    private String qualifyType(String typeName, String typeNamespace) {
        if(typeName.startsWith("http://") || typeName.startsWith("https://")){
            return typeName; //The property is already fully qualified.
        }
        else{
            return String.format("%s%s", typeNamespace, typeName);
        }
    }

    private String qualifyProperty(String propertyName, String propertyNamespace){
        if(propertyName.startsWith("@") || propertyName.startsWith("http://") || propertyName.startsWith("https://")){
            return propertyName; //The property is already fully qualified.
        }
        else{
            return String.format("%s%s", propertyNamespace, propertyName);
        }
    }

    private List<MarmotGraphQuery.Property> evaluateStructure(Class<?> clazz, String defaultPropertyNamespace, String defaultTypeNamespace){
        return getAllFields(clazz).stream().map(field -> {
            if(!MergedAnnotations.from(field, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).get(Query.Ignore.class).isPresent()) {
                MarmotGraphQuery.Property property = new MarmotGraphQuery.Property();
                property.setPropertyName(String.format("query:%s", field.getName()));
                MergedAnnotation<Query.Field> annotation = MergedAnnotations.from(field, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).get(Query.Field.class);
                property.setPath(evaluatePath(field, annotation, defaultPropertyNamespace, defaultTypeNamespace));
                if(annotation.isPresent()){
                    Optional<Boolean> required = annotation.getValue("required", Boolean.class);
                    if(required.isPresent() && required.get()) {
                        property.setRequired(true);
                    }
                }
                Type fieldType = field.getGenericType();
                Type basicType = fieldType;
                if (fieldType instanceof ParameterizedType) {
                    // Get the first type argument → Foo.class
                    basicType = ((ParameterizedType)fieldType).getActualTypeArguments()[0];
                }
                if(!Collection.class.isAssignableFrom(field.getType())){
                    property.setSingleValue("FIRST");
                }
                if(basicType.getTypeName().startsWith("org.marmotgraph")){
                    //Complex type -> dive into it.
                    property.setStructure(evaluateStructure((Class<?>) basicType, defaultPropertyNamespace, defaultTypeNamespace));
                }
                return property;
            }
            return null;
        }).filter(Objects::nonNull).toList();


    }

    public MarmotGraphQuery generate(Class<?> clazz, String targetType){
        logger.info(String.format("Generating query for %s (%s)", clazz.getCanonicalName(), targetType));
        MergedAnnotations classAnnotations = MergedAnnotations.from(clazz, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
        if(classAnnotations.isPresent(Query.class)){
            MergedAnnotation<Query> queryAnnotation = classAnnotations.get(Query.class);
            String defaultTypeNamespace = queryAnnotation.getString("defaultTypeNamespace");
            String defaultPropertyNamespace = queryAnnotation.getString("defaultPropertyNamespace");
            MarmotGraphQuery query = new MarmotGraphQuery("https://core.kg.ebrains.eu/vocab/query/", "https://schema.hbp.eu/myQuery/", targetType);
            query.setStructure(evaluateStructure(clazz, defaultPropertyNamespace, defaultTypeNamespace));
            return query;
        }
        return null;
    }



}
