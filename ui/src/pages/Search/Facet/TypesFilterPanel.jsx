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

import React from 'react';
import { useSelector, useDispatch } from 'react-redux';

import { toggleCategory, clearCategories } from '../../../features/search/searchSlice';
import './TypesFilterPanel.css';

const formatCount = count => {
  const value = Number(count);
  return Number.isFinite(value) ? value.toLocaleString() : count;
};

const TypeFilter = ({ type }) => {

  const dispatch = useDispatch();

  const handleOnClick = () => {
    if(type.type === ""){
      dispatch(clearCategories());
    }
    else {
      dispatch(toggleCategory(type.type));
    }
  };

  return (
    <div
      className={`kgs-fieldsFilter-checkbox ${type.active ? 'is-active' : ''}`}
      onClick={handleOnClick}
      role="checkbox"
      aria-checked={type.active}
      tabIndex={0}
      onKeyDown={event => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          handleOnClick();
        }
      }}
    >
      <div className="kgs-fieldsFilter-checkbox__text">{type.label}</div>
      <div className="kgs-fieldsFilter-checkbox__count">{formatCount(type.count)}</div>
    </div>
  );
};

const TypesFilterPanel = () => {

  const selectedTypes = useSelector(state => state.search.selectedTypes);
  const types = useSelector(state => state.search.types
    .map(t => ({
      ...t,
      active: (t.type === "" && selectedTypes.length===0) || selectedTypes.includes(t.type)
    }))
  );


  return (
    <div className="kgs-fieldsFilter" >
      <div className="kgs-fieldsFilter-title" >Categories</div>
      {types.map(type => {
          return <TypeFilter key={type.type} type={type}/>
        }
      )}
    </div>
  );
};

export default TypesFilterPanel;
