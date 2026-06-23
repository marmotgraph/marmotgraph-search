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
import {createSlice} from '@reduxjs/toolkit';

import {constructFacet, resetFacet} from '../../helpers/Facets';
import {api} from '../../services/api';

export const typesToQueryParam = selectedTypes => (
  Array.isArray(selectedTypes) && selectedTypes.length > 0
    ? selectedTypes.join(',')
    : ''
);

const resolveTypes = (category, list) => {
  let values = [];
  if (Array.isArray(category)) {
    values = category;
  } else if (category) {
    values = [category];
  }
  const valid = new Set(list.map(t => t.type));
  return values.filter(value => valid.has(value));
};

const resolveFacets = (facets, params) => {
  if (!Array.isArray(facets)) {
    return;
  }
  facets.forEach(facet => {
    const value = params[facet.name];
    if (value) {
      switch (facet.type) {
        case 'list':
          facet.value = Array.isArray(value) ? value : [];
          break;
        case 'exists':
          facet.value = true;
          break;
        default:
          break;
      }
    }
  });
};

const getFacet = (types, typeName, facetName) => {
  const type = types.find(t => t.type === typeName);
  if (!Array.isArray(type?.facets)) {
    return null;
  }
  return type.facets.find(f => f.name === facetName);
};

const getRelevantTypes = (types, selectedTypes) => {
  if (!Array.isArray(selectedTypes) || selectedTypes.length === 0) {
    return types;
  }
  return types.filter(type => selectedTypes.includes(type.type));
};

const applyFacetUpdate = (types, selectedTypes, facetName, payload) => {
  if (selectedTypes.length === 1) {
    const facet = getFacet(types, selectedTypes[0], facetName);
    if (facet) {
      updateFacet(facet, payload);
    }
    return;
  }
  getRelevantTypes(types, selectedTypes).forEach(type => {
    if (!Array.isArray(type.facets)) {
      return;
    }
    type.facets
      .filter(facet => facet.name === facetName)
      .forEach(facet => updateFacet(facet, payload));
  });
};

const updateListFacet = (facet, payload) => {
  if (payload.keyword) {
    if (payload.active) {
      const values = Array.isArray(facet.value) ? facet.value : [];
      if (Array.isArray(payload.keyword)) {
        payload.keyword.forEach(keyword => {
          if (!values.includes(keyword)) {
            values.push(keyword);
          }
        });
      } else if (!values.includes(payload.keyword)) {
        values.push(payload.keyword);
      }
      facet.value = values;
    } else if (Array.isArray(facet.value)) {
      facet.value = facet.value.filter(value => {
        if (Array.isArray(payload.keyword)) {
          return !payload.keyword.includes(value);
        }
        return value !== payload.keyword;
      });
    }
  }
};

const updateExistFacet = (facet, payload) => {
  facet.value = !!(payload?.active);
};

const updateFacet = (facet, payload) => {
  if (facet.type === 'list') {
    updateListFacet(facet, payload);
  } else if (facet.type === 'exists') {
    updateExistFacet(facet, payload);
  }
};

const updateFacetsFromResults = (facets, isSelectedType, results) => {
  const aggs = (results?.aggregations) ? results.aggregations : {};
  facets.forEach(facet => {
    if (isSelectedType) {
      const res = aggs[facet.name];
      if (facet.type === 'list') {
        facet.keywords = (res?.keywords) ? res.keywords : [];
        facet.others = (res?.others) ? res.others : 0;
        facet.count = res?.count;
      }
      if (facet.type === 'exists') {
        facet.count = res ? res.count : null; //null value to hide the facet, undefined to hide the count
      } else {
        facet.count = res?.count;
      }
    }
  });
};

const updateTypesFromResults = (types, selectedTypes, results) => {
  types.forEach(type => {
    const count = Number(results?.types?.[type.type]?.count);
    type.count = isNaN(count) ? 0 : count;
    if (Array.isArray(type.facets)) {
      updateFacetsFromResults(
        type.facets,
        selectedTypes.length === 0 || selectedTypes.includes(type.type),
        results
      );
    }
  });
};

const resetAllFacets = state => {
  state.types.forEach(type => {
    if (Array.isArray(type.facets)) {
      type.facets.forEach(facet => resetFacet(facet));
    }
  });
};

const syncParameters = (state, payload) => {
  const {q, category} = (payload instanceof Object) ? payload : {};
  const queryString = q ?? '';
  const selectedTypes = resolveTypes(category, state.types);
  resetAllFacets(state);
  if (selectedTypes.length === 1) {
    const type = state.types.find(t => t.type === selectedTypes[0]);
    resolveFacets(type?.facets, payload);
  } else {
    getRelevantTypes(state.types, selectedTypes).forEach(type => {
      if (Array.isArray(type.facets)) {
        resolveFacets(type.facets, payload);
      }
    });
  }
  state.queryString = queryString;
  state.selectedTypes = selectedTypes;
};

const initialState = {
  types: [],
  cursor: null,
  isInitialized: false,
  isFetching: false,
  queryString: '',
  selectedTypes: [],
  hitsPerPage: 20,
  hits: [],
  suggestions: {},
  total: 0,
  isUpToDate: false
};

const searchSlice = createSlice({
  name: 'search',
  initialState,
  reducers: {
    initializeSearch(state, action) {
      syncParameters(state, action.payload);
      state.isInitialized = true;
      state.hits = []
    },
    syncSearchParameters(state, action) {
      syncParameters(state, action.payload);
      state.isUpToDate = false;
      state.hits = []
    },
    setQueryString(state, action) {
      state.queryString = action.payload;
      state.cursor = null;
      state.isUpToDate = false;
    },
    setFacet(state, action) {
      applyFacetUpdate(state.types, state.selectedTypes, action.payload.name, action.payload);
      state.hits = []
      state.cursor = null;
      state.isUpToDate = false;
    },
    resetFacets(state) {
      resetAllFacets(state);
      state.hits = []
      state.cursor = null;
      state.isUpToDate = false;
    },
    setFacetSize(state, action) {
      state.hits = []
      const facet = state.selectedTypes.length === 1
        ? getFacet(state.types, state.selectedTypes[0], action.payload.name)
        : state.types.flatMap(type => type.facets ?? []).find(f => f.name === action.payload.name);
      if (facet) {
        if (facet.type === 'list') {
          facet.size = action.payload.size;
          state.isUpToDate = false;
        }
      }
    },
    setCursor(state, action) {
      state.cursor = action.payload;
      state.isUpToDate = false;
    },
    toggleCategory(state, action) {
      const typeName = action.payload;
      const type = state.types.find(t => t.type === typeName);
      if (!type) {
        return;
      }
      const isSelected = state.selectedTypes.includes(typeName);
      resetAllFacets(state);
      state.selectedTypes = isSelected
        ? state.selectedTypes.filter(value => value !== typeName)
        : [...state.selectedTypes, typeName];
      state.hits = [];
      state.cursor = null;
      state.isUpToDate = false;
    },
    clearCategories(state) {
      if (state.selectedTypes.length > 0) {
        resetAllFacets(state);
        state.selectedTypes = [];
        state.hits = [];
        state.cursor = null;
        state.isUpToDate = false;
      }
    },
    setSearchResults(state, action) {
      const results = action.payload;
      updateTypesFromResults(state.types, state.selectedTypes, results);
      if (state.cursor != null) {
        if (Array.isArray(results?.hits) && results.hits.length > 0) {

          const lastKnownCursor = Array.isArray(state.hits) && state.hits.length > 0 ? state.hits.at(-1)["cursor"] : null;
          if (results.hits.at(-1)["cursor"] !== lastKnownCursor) {
            state.hits.push(...results.hits);
          }
        }
      } else {
        state.hits = results.hits;
      }
      state.suggestions = (results?.suggestions instanceof Object) ? results.suggestions : {};
      state.total = isNaN(Number(results?.total)) ? 0 : Number(results.total);
    }
  },
  extraReducers(builder) {
    builder
      .addMatcher(
        api.endpoints.getSettings.matchFulfilled,
        (state, {payload}) => {
          state.types = Array.isArray(payload?.types) ? payload.types.map(t => {
            const instanceType = {
              ...t,
              count: 0
            };
            if (Array.isArray(t.facets)) {
              instanceType.facets = t.facets.map(f => constructFacet(f));
            }
            return instanceType;
          }) : [];
        }
      )
      .addMatcher(
        api.endpoints.getSearchNew.matchFulfilled,
        state => {
          state.isUpToDate = true;
          state.isFetching = false;
        }
      )
      .addMatcher(
        api.endpoints.getSearchNew.matchPending,
        state => {
          state.isFetching = true;
        }
      )
      .addMatcher(
        api.endpoints.getSearchNew.matchRejected,
        state => {
          state.hits = [];
          state.suggestions = {};
          state.cursor = null;
          state.isFetching = false;
        }
      );
  }
});

export const selectType = (state, typeName) => state.search.types.find(t => t.type === typeName);

export const selectFacets = (state, selectedTypes) => {
  const types = Array.isArray(selectedTypes) ? selectedTypes : [];
  if (types.length === 1) {
    const type = selectType(state, types[0]);
    if (!Array.isArray(type?.facets)) {
      return [];
    }
    return type.facets;
  }
  const relevantTypes = types.length === 0
    ? state.search.types
    : state.search.types.filter(type => types.includes(type.type));
  const seen = new Set();
  return relevantTypes.flatMap(type => {
    if (!Array.isArray(type.facets)) {
      return [];
    }
    return type.facets.filter(facet => {
      if (seen.has(facet.name)) {
        return false;
      }
      seen.add(facet.name);
      return true;
    });
  });
};

export const {
  initializeSearch,
  syncSearchParameters,
  setQueryString,
  setFacet,
  resetFacets,
  setFacetSize,
  toggleCategory,
  clearCategories,
  setSearchResults,
  setCursor
} = searchSlice.actions;

export default searchSlice.reducer;
