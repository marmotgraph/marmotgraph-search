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

import './MobileFiltersToggle.css';

const MobileFiltersToggle = ({ activeCount, onOpen }) => (
  <div className="kgs-mobile-filters-toggle">
    <button
      type="button"
      className="kgs-mobile-filters-toggle__button"
      onClick={onOpen}
      aria-haspopup="dialog"
    >
      <FontAwesomeIcon icon={faSliders} className="kgs-mobile-filters-toggle__icon" aria-hidden="true" />
      Filters
      {activeCount > 0 && (
        <span className="kgs-mobile-filters-toggle__badge" aria-label={`${activeCount} active filters`}>
          {activeCount}
        </span>
      )}
    </button>
  </div>
);

export default MobileFiltersToggle;
