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

import { faTimes } from '@fortawesome/free-solid-svg-icons/faTimes';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import React, { useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';

import { setFacet, resetFacets, selectFacets } from '../../../features/search/searchSlice';
import { getSelectedFilters } from '../../../helpers/Facets';
import useAuth from '../../../hooks/useAuth';

import './SelectedFilters.css';

const COLLAPSED_LIMIT = 8;

const SelectedFilter = ({ filter, onRemove }) => (
  <li>
    <button
      type="button"
      className="kgs-selected-filter"
      onClick={() => onRemove(filter)}
      aria-label={`Remove filter ${filter.label}`}
      title={filter.label}
    >
      {filter.facetLabel && (
        <span className="kgs-selected-filter__facet">{filter.facetLabel}</span>
      )}
      <span className="kgs-selected-filter__value">{filter.valueLabel}</span>
      <FontAwesomeIcon icon={faTimes} className="kgs-selected-filter__remove" aria-hidden="true" />
    </button>
  </li>
);

const SelectedFilters = () => {
  const dispatch = useDispatch();
  const { isAuthenticated } = useAuth();
  const [expanded, setExpanded] = useState(false);

  const selectedTypes = useSelector(state => state.search.selectedTypes);
  const typeFacets = useSelector(state => selectFacets(state, selectedTypes));
  const facets = isAuthenticated
    ? typeFacets
    : typeFacets.filter(f => !f.authenticationRequired);

  const selectedFilters = getSelectedFilters(facets);
  const hasOverflow = selectedFilters.length > COLLAPSED_LIMIT;
  const visibleFilters = !hasOverflow || expanded
    ? selectedFilters
    : selectedFilters.slice(0, COLLAPSED_LIMIT);
  const hiddenCount = selectedFilters.length - COLLAPSED_LIMIT;

  const handleRemove = filter => {
    dispatch(setFacet({
      name: filter.name,
      active: false,
      keyword: filter.keyword
    }));
  };

  const handleReset = () => {
    dispatch(resetFacets());
    setExpanded(false);
  };

  const handleToggleExpanded = () => {
    setExpanded(current => !current);
  };

  if (!selectedFilters.length) {
    return null;
  }

  return (
    <section className="kgs-selected-filters" aria-label="Active filters">
      <div className="kgs-selected-filters__header">
        <div className="kgs-selected-filters__heading">
          <span className="kgs-selected-filters__title">Active filters</span>
          <span className="kgs-selected-filters__count">{selectedFilters.length}</span>
        </div>
        <button
          type="button"
          className="kgs-selected-filters__reset"
          onClick={handleReset}
        >
          Clear all
        </button>
      </div>
      <div className={`kgs-selected-filters__body${expanded ? ' is-expanded' : ''}`}>
        <ul className="kgs-selected-filters__list">
          {visibleFilters.map(filter => (
            <SelectedFilter
              key={`${filter.name}-${filter.keyword ?? 'exists'}`}
              filter={filter}
              onRemove={handleRemove}
            />
          ))}
          {hasOverflow && !expanded && (
            <li>
              <button
                type="button"
                className="kgs-selected-filters__more"
                onClick={handleToggleExpanded}
                aria-expanded={false}
              >
                +{hiddenCount} more
              </button>
            </li>
          )}
        </ul>
        {hasOverflow && expanded && (
          <button
            type="button"
            className="kgs-selected-filters__toggle"
            onClick={handleToggleExpanded}
            aria-expanded={true}
          >
            Show less
          </button>
        )}
      </div>
    </section>
  );
};

export default SelectedFilters;
