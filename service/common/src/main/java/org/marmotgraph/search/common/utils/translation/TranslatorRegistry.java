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

package org.marmotgraph.search.common.utils.translation;


import lombok.Getter;
import org.marmotgraph.search.common.controller.translation.models.Translator;
import org.marmotgraph.search.common.controller.translation.models.TranslatorModel;
import org.marmotgraph.search.common.model.source.SourceInstance;
import org.marmotgraph.search.common.model.target.MetaInfo;
import org.marmotgraph.search.common.model.target.TargetInstance;
import org.marmotgraph.search.common.utils.queryGenerator.Query;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TranslatorRegistry {

    @Getter
    private final List<TranslatorModel> translators;

    public TranslatorRegistry(ApplicationContext applicationContext) {
        Map<String, Object> translatorBeans = applicationContext.getBeansWithAnnotation(Translator.Instance.class);
        Map<Boolean, List<Object>> discoveredTranslators = translatorBeans.values().stream().collect(Collectors.groupingBy(t -> Translator.class.isAssignableFrom(t.getClass())));
        if(discoveredTranslators.get(Boolean.FALSE) != null){
            String invalidTranslators = discoveredTranslators.get(Boolean.FALSE).stream().map(t -> t.getClass().getSimpleName()).collect(Collectors.joining(", "));
            this.translators = null;
            throw new IllegalArgumentException(String.format("The translators %s do not implement the Translator interface.", invalidTranslators));
        }
        else {
            this.translators = discoveredTranslators.get(Boolean.TRUE).stream().map(t -> ((Translator<? extends SourceInstance, ? extends TargetInstance>) t)).map(t -> {
                ResolvableType resolvableType = ResolvableType.forClass(t.getClass());
                ResolvableType asInterface = resolvableType.as(Translator.class);
                ResolvableType[] generics = asInterface.getGenerics();
                Class<?> source = generics[0].resolve();

                MergedAnnotation<Query> queryAnnotation = MergedAnnotations.from(source, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).get(Query.class);
                if(!queryAnnotation.isPresent()){
                    throw new IllegalArgumentException(String.format("The source (%s) does not have a query annotation", source.getCanonicalName()));
                }
                String[] semanticTypes = queryAnnotation.getStringArray("semanticTypes");
                List<String> normalizedSemanticTypes = Arrays.stream(semanticTypes).map(s -> s.startsWith("http") ? s : String.format("%s%s", queryAnnotation.getString("defaultTypeNamespace"), s)).toList();
                Class<?> target = generics[1].resolve();
                MergedAnnotation<MetaInfo> metaInfoAnnotation = MergedAnnotations.from(target, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).get(MetaInfo.class);
                if(!metaInfoAnnotation.isPresent()){
                    throw new IllegalArgumentException(String.format("The target (%s) does not have a metadata annotation", source.getCanonicalName()));
                }
                MergedAnnotation<Translator.Instance> translatorAnnotation = MergedAnnotations.from(t.getClass(), MergedAnnotations.SearchStrategy.TYPE_HIERARCHY).get(Translator.Instance.class);

                return new TranslatorModel((Class<? extends SourceInstance>) source, (Class<? extends TargetInstance>) target, t, translatorAnnotation.getBoolean("autoRelease"), queryAnnotation.getInt("bulkSize"), translatorAnnotation.getBoolean("addToSitemap"), normalizedSemanticTypes, metaInfoAnnotation.getString("name"), translatorAnnotation.getInt("orderNumber"));
            }).sorted(TranslatorModel.COMPARATOR).toList();
        }
    }

    public Class<?> getFileClass() {
        return null;
    }

    public List<String> getMainCategories(){
        return translators.stream().filter(t -> t.orderNumber()<Integer.MAX_VALUE).map(TranslatorModel::category).toList();

    }

}
