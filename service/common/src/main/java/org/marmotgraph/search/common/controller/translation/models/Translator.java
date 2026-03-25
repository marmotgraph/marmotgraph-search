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

package org.marmotgraph.search.common.controller.translation.models;

import org.marmotgraph.search.common.model.DataStage;
import org.marmotgraph.search.common.model.source.SourceInstance;
import org.marmotgraph.search.common.model.target.TargetInstance;
import org.marmotgraph.search.common.model.target.Value;
import org.marmotgraph.search.common.services.ESServiceClient;
import org.marmotgraph.search.common.utils.ESHelper;
import org.marmotgraph.search.common.utils.IdUtils;
import org.marmotgraph.search.common.utils.TranslationException;
import org.marmotgraph.search.common.utils.TranslatorUtils;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;

public abstract class Translator<Source extends SourceInstance, Target extends TargetInstance> extends TranslatorBase {
    @java.lang.annotation.Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Component
    public @interface Instance {
        boolean autoRelease() default false;
        boolean addToSitemap() default false;
        int orderNumber() default Integer.MAX_VALUE;
    }


    public static final class DontIndexException extends Exception{}

    public final Target translate(SourceInstance source, DataStage dataStage, String category, Class<?> targetType, boolean liveMode, TranslatorUtils translatorUtils) throws TranslationException{
        Target t = setup(category, (Source)source, (Class<Target>)targetType);
        try {
            translate((Source) source, t, dataStage, liveMode, translatorUtils);
            return t;
        }
        catch (DontIndexException e){
            return null;
        }
    }

    private Target setup(String category, Source sourceEntity, Class<Target> targetType){
        try {
            Target target = null;
            target = targetType.getConstructor().newInstance();
            target.setType(new Value<>(category));
            target.setCategory(new Value<>(category));
            target.setId(IdUtils.getUUID(sourceEntity.getId()));
            target.setAllIdentifiers(sourceEntity.getIdentifier());
            target.setIdentifier(Collections.singletonList(target.getId()));
            return target;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void translate(Source source, Target target, DataStage dataStage, boolean liveMode, TranslatorUtils translatorUtils) throws DontIndexException, TranslationException;

    public Map<String, Object> populateTranslationContext(ESServiceClient esServiceClient, ESHelper esHelper, DataStage stage){
        return Collections.emptyMap();
    }
}