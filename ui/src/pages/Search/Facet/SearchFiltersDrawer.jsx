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
import React, { useEffect, useId, useRef } from 'react';
import { useDispatch } from 'react-redux';

import { clearCategories, resetFacets } from '../../../features/search/searchSlice';

import FiltersPanel from './FiltersPanel';
import TypesFilterPanel from './TypesFilterPanel';

import './SearchFiltersDrawer.css';

const SearchFiltersDrawer = ({
  isOpen,
  onClose,
  resultCount,
  isFetching,
}) => {
  const dispatch = useDispatch();
  const titleId = useId();
  const closeButtonRef = useRef(null);

  useEffect(() => {
    if (!isOpen) {
      return undefined;
    }

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const handleKeyDown = event => {
      if (event.key === 'Escape') {
        onClose();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    closeButtonRef.current?.focus();

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose]);

  const handleClearAll = () => {
    dispatch(resetFacets());
    dispatch(clearCategories());
  };

  const resultsLabel = isFetching
    ? 'Show results'
    : resultCount === 1
      ? 'Show 1 result'
      : `Show ${resultCount} results`;

  return (
    <div
      className={`kgs-search-filters-drawer${isOpen ? ' is-open' : ''}`}
      aria-hidden={!isOpen}
    >
      <button
        type="button"
        className="kgs-search-filters-drawer__backdrop"
        onClick={onClose}
        tabIndex={isOpen ? 0 : -1}
        aria-label="Close filters"
      />
      <div
        className="kgs-search-filters-drawer__panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
      >
        <div className="kgs-search-filters-drawer__header">
          <h2 id={titleId} className="kgs-search-filters-drawer__title">Filters</h2>
          <button
            ref={closeButtonRef}
            type="button"
            className="kgs-search-filters-drawer__close"
            onClick={onClose}
            aria-label="Close filters"
          >
            <FontAwesomeIcon icon={faTimes} aria-hidden="true" />
          </button>
        </div>

        <div className="kgs-search-filters-drawer__body">
          <TypesFilterPanel />
          <FiltersPanel showHeader={false} />
        </div>

        <div className="kgs-search-filters-drawer__footer">
          <button
            type="button"
            className="kgs-search-filters-drawer__clear"
            onClick={handleClearAll}
          >
            Clear all
          </button>
          <button
            type="button"
            className="kgs-search-filters-drawer__apply"
            onClick={onClose}
          >
            {resultsLabel}
          </button>
        </div>
      </div>
    </div>
  );
};

export default SearchFiltersDrawer;
