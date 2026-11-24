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
import org.marmotgraph.sdk.model.QuerySpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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


    private List<QuerySpecification.Path> evaluatePath(Field field, MergedAnnotation<Query.Field> annotation, String defaultPropertyNamespace, String defaultTypeNamespace){
        if(annotation.isPresent()) {
            MergedAnnotation<Query.PathElement>[] path = annotation.getAnnotationArray("path", Query.PathElement.class);
            if(path.length > 0) {
                return Arrays.stream(path).map(p -> {
                    String propertyNamespace = p.getString("namespace");
                    Optional<Boolean> reverse = p.getValue("reverse", Boolean.class);
                    String[] typeFilters = p.getStringArray("typeFilter");
                    String name = p.getString("name");
                    if(StringUtils.isBlank(name)){
                        name = field.getName();
                    }
                    return new QuerySpecification.Path(
                            qualifyProperty(name, StringUtils.isBlank(propertyNamespace) ? defaultPropertyNamespace : propertyNamespace),
                            reverse.isPresent() && reverse.get() ? Boolean.TRUE : null,
                            typeFilters.length > 0 ? Arrays.stream(typeFilters).map(t -> qualifyType(t, defaultTypeNamespace)).toList() : null);
                }).toList();
            }
        }
        return Collections.singletonList(QuerySpecification.Path.outgoing(qualifyProperty(field.getName(), defaultPropertyNamespace)));

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

    private List<QuerySpecification.StructureItem> evaluateStructure(Class<?> clazz, String defaultPropertyNamespace, String defaultTypeNamespace){
        return getAllFields(clazz).stream().filter(f -> !Modifier.isTransient(f.getModifiers())).map(field -> {
            if(!MergedAnnotations.from(field, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).get(Query.Ignore.class).isPresent()) {
                MergedAnnotation<Query.Field> annotation = MergedAnnotations.from(field, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).get(Query.Field.class);
                List<QuerySpecification.Path> path = evaluatePath(field, annotation, defaultPropertyNamespace, defaultTypeNamespace);
                Boolean required = null;
                Boolean ensureOrder = null;
                if(annotation.isPresent()){
                    Optional<Boolean> requiredValue = annotation.getValue("required", Boolean.class);
                    if(requiredValue.isPresent() && requiredValue.get()) {
                        required = true;
                    }
                    Optional<Boolean> ensureOrderValue = annotation.getValue("ensureOrder", Boolean.class);
                    if(ensureOrderValue.isPresent() && ensureOrderValue.get()) {
                        ensureOrder = true;
                    }
                }
                Type fieldType = field.getGenericType();
                Type basicType = fieldType;
                if (fieldType instanceof ParameterizedType) {
                    // Get the first type argument → Foo.class
                    basicType = ((ParameterizedType)fieldType).getActualTypeArguments()[0];
                }
                QuerySpecification.SingleValue singleValue = Collection.class.isAssignableFrom(field.getType()) ? null : QuerySpecification.SingleValue.FIRST;
                List<QuerySpecification.StructureItem> structure = null;
                if(basicType.getTypeName().startsWith("org.marmotgraph")){
                    //Complex type -> dive into it.
                    structure = evaluateStructure((Class<?>) basicType, defaultPropertyNamespace, defaultTypeNamespace);
                }
                // The keys of the results are the property names - so they match the field names of the source classes
                return new QuerySpecification.StructureItem(field.getName(), path, structure, required, null, ensureOrder, null, singleValue);
            }
            return null;
        }).filter(Objects::nonNull).toList();


    }


    private final Map<String, Map<String, QuerySpecification>> queryCache = new ConcurrentHashMap<>();

    public QuerySpecification generate(Class<?> clazz, String targetType){
        QuerySpecification fromCache = queryCache.getOrDefault(clazz.getCanonicalName(), new HashMap<>()).get(targetType);
        if(fromCache!=null){
            logger.info(String.format("Query for %s (%s) found in cache", clazz.getCanonicalName(), targetType));
            return fromCache;
        }
        logger.info(String.format("Generating query for %s (%s)", clazz.getCanonicalName(), targetType));
        MergedAnnotations classAnnotations = MergedAnnotations.from(clazz, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);
        if(classAnnotations.isPresent(Query.class)){
            MergedAnnotation<Query> queryAnnotation = classAnnotations.get(Query.class);
            String defaultTypeNamespace = queryAnnotation.getString("defaultTypeNamespace");
            String defaultPropertyNamespace = queryAnnotation.getString("defaultPropertyNamespace");
            QuerySpecification query = new QuerySpecification(new QuerySpecification.Meta(targetType, null, null), evaluateStructure(clazz, defaultPropertyNamespace, defaultTypeNamespace));
            queryCache.computeIfAbsent(clazz.getCanonicalName(), f -> new HashMap<>()).put(targetType, query);
            logger.info(String.format("Query for %s (%s) generated", clazz.getCanonicalName(), targetType));
            return query;
        }
        return null;
    }



}
