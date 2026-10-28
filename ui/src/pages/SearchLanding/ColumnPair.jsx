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

import LandingCta from './LandingCta';
import { hasCallToAction, hasText, textOf } from './landingContent';

import './ColumnPair.css';

const columnHasContent = column => hasText(column?.title)
  || hasText(column?.text)
  || hasCallToAction(column?.cta);

const Column = ({ column, variant }) => {
  const title = textOf(column?.title);
  const text = textOf(column?.text);

  return (
    <div className="kgs-landing-column">
      {title && <h2 className="kgs-landing-column__title">{title}</h2>}
      {text && <p className="kgs-landing-column__text">{text}</p>}
      <LandingCta cta={column?.cta} variant={variant} />
    </div>
  );
};

const ColumnPair = ({ section }) => {
  const columns = [
    section?.left && columnHasContent(section.left) ? { id: 'left', column: section.left, variant: 'button' } : null,
    section?.right && columnHasContent(section.right) ? { id: 'right', column: section.right, variant: 'button-secondary' } : null
  ].filter(Boolean);

  if (!columns.length) {
    return null;
  }

  return (
    <section className="kgs-landing-columns">
      {columns.map(({ id, column, variant }) => (
        <Column key={id} column={column} variant={variant} />
      ))}
    </section>
  );
};

export default ColumnPair;
