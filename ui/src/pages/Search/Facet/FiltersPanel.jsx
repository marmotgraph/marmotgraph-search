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

import { faSliders } from '@fortawesome/free-solid-svg-icons/faSliders';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import React from 'react';
import { useSelector, useDispatch } from 'react-redux';

import Facet from '../../../components/Facet/Facet';
import { setFacet, setFacetSize, resetFacets, selectFacets } from '../../../features/search/searchSlice';
import useAuth from '../../../hooks/useAuth';

import './FiltersPanel.css';
const FiltersPanel = () => {

  const { isAuthenticated } = useAuth();

  const dispatch = useDispatch();

  const typeFacets = useSelector(state => selectFacets(state, state.search.selectedTypes));
  const facets = isAuthenticated ? typeFacets : typeFacets.filter(f => !f.authenticationRequired);

  const handleOnChange = (name, active, keyword) => {
    dispatch(setFacet({
      name: name,
      active: active,
      keyword: keyword
    }));
  };

  const handleOnViewChange = (name, size) => {
    dispatch(setFacetSize({
      name: name,
      size: size
    }));
  };

  const handleOnReset = () => {
    dispatch(resetFacets());
  };

  if (!facets.length) {
    return null;
  }

  return (
    <div className="kgs-filters">
      <div className="kgs-filters__header">
        <div className="kgs-filters__title">
          <FontAwesomeIcon icon={faSliders} className="kgs-filters__icon" aria-hidden="true" />
          Filters
        </div>
        <button type="button" className="kgs-filters__reset-button" onClick={handleOnReset}>
          Clear all
        </button>
      </div>
      <div className="kgs-filters__body">
        {facets.map(facet => (
          <Facet
            key={facet.name}
            facet={facet}
            onChange={handleOnChange}
            onViewChange={handleOnViewChange}
          />
        ))}
      </div>
    </div>
  );
};

export default FiltersPanel;
