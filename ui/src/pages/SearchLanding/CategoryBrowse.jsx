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

import React, { useMemo } from 'react';
import { useSelector } from 'react-redux';

import { useGetSearchNewQuery } from '../../services/api';

import './CategoryBrowse.css';

const formatCount = count => {
  const value = Number(count);
  return Number.isFinite(value) ? value.toLocaleString() : '—';
};

const CategoryBrowse = ({ onCategorySelect }) => {
  const types = useSelector(state => state.search.types);
  const group = useSelector(state => state.groups.group);

  const { data, isFetching } = useGetSearchNewQuery(
    {
      group,
      q: '',
      type: '',
      size: 1,
      cursor: null,
      payload: {}
    },
    { skip: !types.length || !group }
  );

  const categories = useMemo(() => types.map(type => ({
    ...type,
    count: data?.types?.[type.type]?.count ?? type.count ?? null
  })), [types, data]);

  if (!types.length) {
    return null;
  }

  return (
    <section className="kgs-category-browse" aria-labelledby="kgs-category-browse-title">
      <h2 id="kgs-category-browse-title" className="kgs-category-browse__title">
        Search by category:
      </h2>
      <ul className="kgs-category-browse__grid">
        {categories.map(category => (
          <li key={category.type}>
            <button
              type="button"
              className="kgs-category-browse__box"
              onClick={() => onCategorySelect(category.type)}
            >
              <span className="kgs-category-browse__label">{category.label}</span>
              <span className="kgs-category-browse__count">
                {isFetching && category.count == null
                  ? '…'
                  : `${formatCount(category.count ?? 0)} results`}
              </span>
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
};

export default CategoryBrowse;
