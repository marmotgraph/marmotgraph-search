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

import React, { useId, useMemo } from 'react';

import { getOptionsStatusMessage } from '../../helpers/Facets';
import { List } from '../List/List';

export const PaginatedList = ({
  items,
  optionCount,
  ItemComponent,
  itemUniqKeyAttribute,
  onItemClick,
}) => {
  const statusId = useId();

  const selectedCount = useMemo(
    () => items.filter(item => item.checked).length,
    [items]
  );

  const totalOptions = optionCount ?? items.length;

  const statusMessage = useMemo(
    () => getOptionsStatusMessage(totalOptions, selectedCount),
    [totalOptions, selectedCount]
  );

  if (!Array.isArray(items) || !items.length) {
    return null;
  }

  return (
    <div className="kgs-facet-list">
      {statusMessage && (
        <div
          id={statusId}
          className="kgs-facet__options-status"
          aria-live="polite"
          aria-atomic="true"
        >
          {statusMessage}
        </div>
      )}
      <div className="kgs-facet__scrollable-options" aria-describedby={statusMessage ? statusId : undefined}>
        <List
          items={items}
          ItemComponent={ItemComponent}
          itemUniqKeyAttribute={itemUniqKeyAttribute}
          onItemClick={onItemClick}
        />
      </div>
    </div>
  );
};

export default PaginatedList;
