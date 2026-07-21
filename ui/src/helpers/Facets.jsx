/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This open source software code was developed in part or in whole in the
 * Human Brain Project, funded from the European Union's Horizon 2020
 * Framework Programme for Research and Innovation under
 * Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 * (Human Brain Project SGA1, SGA2 and SGA3).
 *
 */
const FACET_DEFAULT_SIZE = 50;
export const FACET_ALL_SIZE = 1000000000;
const OPTIONS_STATUS_THRESHOLD = 5;
export const SEARCHABLE_OPTIONS_THRESHOLD = 6;

export const getFacetOptionCount = facet => facet.count ?? facet.keywords?.length ?? 0;

export const isFacetSearchable = (facet, optionCount = getFacetOptionCount(facet)) => (
  facet.type === 'list'
  && !facet.isHierarchical
  && optionCount > SEARCHABLE_OPTIONS_THRESHOLD
);

export const getOptionsStatusMessage = (optionCount, selectedCount) => {
  if (optionCount <= OPTIONS_STATUS_THRESHOLD) {
    return null;
  }
  if (selectedCount > 0) {
    return `${selectedCount} of ${optionCount} selected.`;
  }
  return `${optionCount} options.`;
};

export const resetFacet = facet => {
  switch (facet.type) {
  case 'list':
    facet.value = null;
    facet.size = (facet.isHierarchical || facet.isFilterable || isFacetSearchable(facet))
      ? FACET_ALL_SIZE
      : FACET_DEFAULT_SIZE;
    break;
  case 'exists':
  default:
    facet.value = null;
  }
};

export const constructFacet = facet => ({
  ...facet,
  count: undefined,
  value: null,
  keywords: [],
  others: 0,
  size: facet.isFilterable?FACET_ALL_SIZE:FACET_DEFAULT_SIZE,
  defaultSize: facet.isFilterable?FACET_ALL_SIZE:FACET_DEFAULT_SIZE
});

export const getFacetSelectionCount = facet => {
  switch (facet.type) {
  case 'list':
    return Array.isArray(facet.value) ? facet.value.length : 0;
  case 'exists':
    return facet.value ? 1 : 0;
  default:
    return 0;
  }
};

export const getSelectedFilters = facets => {
  if (!Array.isArray(facets)) {
    return [];
  }
  return facets.reduce((acc, facet) => {
    switch (facet.type) {
    case 'list':
      if (Array.isArray(facet.value) && facet.value.length) {
        const facetLabel = facet.title ?? facet.label;
        facet.value.forEach(keyword => {
          acc.push({
            name: facet.name,
            keyword,
            facetLabel,
            valueLabel: keyword,
            label: facetLabel ? `${facetLabel}: ${keyword}` : keyword,
            many: true
          });
        });
      }
      break;
    case 'exists':
      if (facet.value) {
        const valueLabel = facet.subLabel ?? `Has ${facet.label}`;
        acc.push({
          name: facet.name,
          keyword: undefined,
          facetLabel: null,
          valueLabel,
          label: valueLabel,
          many: false
        });
      }
      break;
    default:
      break;
    }
    return acc;
  }, []);
};

export const getActiveFilterCount = (facets, selectedTypes) => {
  const facetCount = getSelectedFilters(facets).length;
  const categoryCount = Array.isArray(selectedTypes) ? selectedTypes.length : 0;
  return facetCount + categoryCount;
};

export const getAggregation = facets => {
  if (!Array.isArray(facets)) {
    return {};
  }
  return facets.reduce((acc, facet) => {
    switch (facet.type) {
    case 'list':
      //if (facet.isHierarchical) {
      if (Array.isArray(facet.value) && facet.value.length) {
        acc[facet.name] = {
          values: facet.value,
          size: facet.size
        };
      } else {
        acc[facet.name] = {
          size: facet.size
        };
      }
      break;
    case 'exists':
      if (facet.value) {
        acc[facet.name] = {};
      }
      break;
    default:
      break;
    }
    return acc;
  }, {});
};