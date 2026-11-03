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

import { textOf } from './landingContent';

import './ExampleQueries.css';

const ExampleQueries = ({ examples, onSelect }) => {
  const queries = (Array.isArray(examples) ? examples : [])
    .map(example => ({
      label: textOf(example?.label) || textOf(example?.query),
      query: textOf(example?.query) || textOf(example?.label)
    }))
    .filter(example => example.query);

  if (!queries.length) {
    return null;
  }

  return (
    <div className="kgs-landing-examples">
      <p className="kgs-landing-examples__label" id="kgs-landing-examples-label">Try</p>
      <ul className="kgs-landing-examples__list" aria-labelledby="kgs-landing-examples-label">
        {queries.map(example => (
          <li key={example.query}>
            <button
              type="button"
              className="kgs-landing-examples__chip"
              onClick={() => onSelect(example.query)}
            >
              {example.label}
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
};

export default ExampleQueries;
