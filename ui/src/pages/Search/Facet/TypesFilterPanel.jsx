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

import { faChevronDown } from '@fortawesome/free-solid-svg-icons/faChevronDown';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';

import FacetCheckbox from '../../../components/Facet/FacetCheckbox';
import '../../../components/Facet/Facet.css';
import { List } from '../../../components/List/List';
import { toggleCategory, clearCategories } from '../../../features/search/searchSlice';

import './TypesFilterPanel.css';

const TypesFilterPanel = () => {

  const dispatch = useDispatch();
  const selectedTypes = useSelector(state => state.search.selectedTypes);
  const types = useSelector(state => state.search.types);
  const selectionCount = selectedTypes.length;

  const [collapsed, setCollapsed] = useState(selectionCount === 0);

  useEffect(() => {
    if (selectionCount > 0) {
      setCollapsed(false);
    }
  }, [selectionCount]);

  const items = useMemo(() => (
    types.map(type => ({
      type: type.type,
      label: type.label,
      count: type.count,
      checked: (type.type === '' && selectedTypes.length === 0) || selectedTypes.includes(type.type),
    }))
  ), [types, selectedTypes]);

  const handleTypeClick = useCallback(item => {
    if (item.type === '') {
      dispatch(clearCategories());
    } else {
      dispatch(toggleCategory(item.type));
    }
  }, [dispatch]);

  const handleClearAll = useCallback(event => {
    event.stopPropagation();
    dispatch(clearCategories());
  }, [dispatch]);

  if (!items.length) {
    return null;
  }

  return (
    <div className={`kgs-facet kgs-facet--categories${collapsed ? ' is-collapsed' : ' is-expanded'}`}>
      <div className="kgs-facet--categories__header-bar">
        <button
          type="button"
          className="kgs-facet__header kgs-facet--categories__toggle"
          onClick={() => setCollapsed(current => !current)}
          aria-expanded={!collapsed}
          aria-controls="types-filter-options"
          id="types-filter-heading"
        >
          <span className="kgs-facet__title">Categories</span>
        </button>
        <div className="kgs-facet__header-actions kgs-facet--categories__header-actions">
          {selectionCount > 0 && (
            <button
              type="button"
              className="kgs-facet--categories__reset-button"
              onClick={handleClearAll}
              aria-label="Clear all categories"
            >
              Clear all
            </button>
          )}
          {selectionCount > 0 && (
            <span className="kgs-facet__badge" aria-label={`${selectionCount} selected`}>
              {selectionCount}
            </span>
          )}
          <button
            type="button"
            className="kgs-facet--categories__chevron-button"
            onClick={() => setCollapsed(current => !current)}
            aria-expanded={!collapsed}
            aria-controls="types-filter-options"
            aria-label={collapsed ? 'Expand categories' : 'Collapse categories'}
          >
            <FontAwesomeIcon icon={faChevronDown} className="kgs-facet__chevron" aria-hidden="true" />
          </button>
        </div>
      </div>
      {!collapsed && (
        <div
          id="types-filter-options"
          className="kgs-facet__body"
          role="group"
          aria-labelledby="types-filter-heading"
        >
          <List
            items={items}
            ItemComponent={FacetCheckbox}
            itemUniqKeyAttribute="type"
            onItemClick={handleTypeClick}
          />
        </div>
      )}
    </div>
  );
};

export default TypesFilterPanel;
