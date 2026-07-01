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

import { faSearch } from '@fortawesome/free-solid-svg-icons/faSearch';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import React, { useId, useMemo, useState } from 'react';

import { List } from '../List/List';

import './FilteredList.css';

export const FilteredList = ({
  label,
  items,
  ItemComponent,
  itemUniqKeyAttribute,
  onItemClick,
}) => {
  const [filter, setFilter] = useState('');
  const searchId = useId();
  const listId = useId();
  const statusId = useId();

  const selectedCount = useMemo(
    () => items.filter(item => item.checked).length,
    [items]
  );

  const visibleItems = useMemo(() => {
    const term = filter.toLowerCase().trim();
    const filtered = term
      ? items.filter(item => item.value && item.value.toLowerCase().includes(term))
      : items;

    if (!term) {
      return [...filtered].sort((a, b) => Number(b.checked) - Number(a.checked));
    }

    return filtered;
  }, [items, filter]);

  const statusMessage = useMemo(() => {
    const term = filter.trim();

    if (term && visibleItems.length === 0) {
      return `No ${label.toLowerCase()} match "${filter}".`;
    }

    if (term) {
      const countLabel = visibleItems.length === 1 ? 'option' : 'options';
      return `${visibleItems.length} ${countLabel} shown for "${filter}".`;
    }

    if (selectedCount > 0) {
      const countLabel = selectedCount === 1 ? 'filter' : 'filters';
      return `${selectedCount} ${countLabel} selected.`;
    }

    return `${items.length} options.`;
  }, [filter, visibleItems.length, label, selectedCount, items.length]);

  const handleSearchKeyDown = event => {
    if (event.key === 'Escape') {
      setFilter('');
      event.currentTarget.blur();
    }
  };

  if (!Array.isArray(items) || !items.length) {
    return null;
  }

  return (
    <div className="kgs-filtered-list">
      <div className="kgs-filtered-list__search">
        <label className="kgs-filtered-list__label" htmlFor={searchId}>
          Filter {label.toLowerCase()}
        </label>
        <div className="kgs-filtered-list__search-field">
          <input
            id={searchId}
            className="kgs-filtered-list__input"
            type="search"
            value={filter}
            onChange={event => setFilter(event.target.value)}
            onKeyDown={handleSearchKeyDown}
            placeholder={`Search ${label.toLowerCase()}…`}
            aria-controls={listId}
            aria-describedby={statusId}
            autoComplete="off"
            spellCheck={false}
          />
          <FontAwesomeIcon
            icon={faSearch}
            className="kgs-filtered-list__search-icon"
            aria-hidden="true"
          />
        </div>
      </div>

      <div
        id={statusId}
        className="kgs-filtered-list__status"
        aria-live="polite"
        aria-atomic="true"
      >
        {statusMessage}
      </div>

      {visibleItems.length > 0 ? (
        <div
          id={listId}
          className="kgs-filtered-list__options"
          role="group"
          aria-label={label}
        >
          <List
            items={visibleItems}
            ItemComponent={ItemComponent}
            itemUniqKeyAttribute={itemUniqKeyAttribute}
            onItemClick={onItemClick}
          />
        </div>
      ) : (
        <p className="kgs-filtered-list__empty">No matching options.</p>
      )}
    </div>
  );
};

export default FilteredList;
