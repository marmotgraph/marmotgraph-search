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

package eu.ebrains.kg.projects.ebrains.source.commons;

import eu.ebrains.kg.common.model.source.FullNameRef;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@Setter
public class QuantitativeValueOrRange {
    private Double value;
    private List<Double> uncertainty;
    private FullNameRef typeOfUncertainty;
    private FullNameRef unit;
    private Double maxValue;
    private Double minValue;
    private FullNameRef maxValueUnit;
    private FullNameRef minValueUnit;

    private String getValueDisplay(Double value) {
        if (value == null) {
            return null;
        }
        if (value % 1 == 0) {
            //It's an integer -> let's remove the floats.
            return String.valueOf(value.intValue());
        } else {
            return String.format("%.2f", value);
        }
    }

    public String displayString() {
        String valueStr = getValueDisplay(value);
        if (valueStr != null) {
            //Single value
            String valueWithUnit = unit == null ? valueStr : String.format("%s %s", valueStr, unit.getFullName());
            if (!CollectionUtils.isEmpty(uncertainty)) {
                final String uncertaintyValues = uncertainty.stream().map(this::getValueDisplay).filter(Objects::nonNull).collect(Collectors.joining(", "));
                if (!uncertaintyValues.isBlank() && typeOfUncertainty != null && typeOfUncertainty.getFullName() != null) {
                    if (unit == null) {
                        return String.format("%s (%s: %s)", valueWithUnit, typeOfUncertainty.getFullName(), uncertaintyValues);
                    }
                    return String.format("%s (%s: %s %s)", valueWithUnit, typeOfUncertainty.getFullName(), uncertaintyValues, unit.getFullName());
                }
            }
            return valueWithUnit;
        } else {
            //Value range
            boolean sameUnit = (minValueUnit == null && maxValueUnit == null) || (minValueUnit != null && minValueUnit.equals(maxValueUnit));
            String minValueStr = getValueDisplay(minValue);
            String maxValueStr = getValueDisplay(maxValue);
            return String.format("%s %s - %s %s",
                            StringUtils.defaultString(minValueStr, ""),
                            getString(sameUnit),
                            StringUtils.defaultString(maxValueStr, ""),
                            maxValueUnit != null ? StringUtils.defaultString(maxValueUnit.getFullName(), "") : "").trim()
                    .replaceAll(" {2,}", " ");
        }
    }

    private String getString(boolean sameUnit) {
        if (sameUnit) {
            return "";
        }
        if (minValueUnit != null) {
            return StringUtils.defaultString(minValueUnit.getFullName(), "");
        }
        return "";
    }
}
