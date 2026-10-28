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

import { fas } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import React from 'react';

import LandingCta from './LandingCta';
import { hasCallToAction, hasText, isImageIcon, safeImageSource, textOf } from './landingContent';

import './FeatureCards.css';

const toIconKey = name => {
  const trimmed = name.trim();
  if (/^fa[A-Z0-9]/.test(trimmed) && fas[trimmed]) {
    return trimmed;
  }
  const withoutPrefix = trimmed.replace(/^fa-/i, '');
  const pascal = withoutPrefix
    .split(/[^a-zA-Z0-9]+/)
    .filter(Boolean)
    .map(part => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join('');
  return pascal ? `fa${pascal}` : '';
};

const resolveIcon = name => {
  if (!hasText(name) || isImageIcon(name)) {
    return null;
  }
  const icon = fas[toIconKey(name)];
  return icon?.iconName ? icon : null;
};

const cardActions = card => {
  const actions = [];
  if (card?.cta) {
    actions.push(card.cta);
  }
  if (Array.isArray(card?.ctas)) {
    actions.push(...card.ctas);
  }
  return actions.filter(hasCallToAction);
};

const cardHasContent = card => hasText(card?.title)
  || hasText(card?.text)
  || cardActions(card).length > 0
  || !!safeImageSource(card?.icon)
  || !!resolveIcon(card?.icon);

const CardIcon = ({ icon, title }) => {
  const image = isImageIcon(icon) ? safeImageSource(icon) : null;
  if (image) {
    return (
      <span className="kgs-landing-card__icon">
        <img src={image} alt={title ? '' : 'Card icon'} />
      </span>
    );
  }

  const glyph = resolveIcon(icon);
  if (!glyph) {
    return null;
  }

  return (
    <span className="kgs-landing-card__icon" aria-hidden="true">
      <FontAwesomeIcon icon={glyph} />
    </span>
  );
};

const FeatureCards = ({ section }) => {
  const title = textOf(section?.title);
  const items = (Array.isArray(section?.items) ? section.items : []).filter(cardHasContent);
  if (!items.length) {
    return null;
  }

  const TitleTag = title ? 'h3' : 'h2';

  return (
    <section
      className="kgs-landing-cards"
      {...(title ? { 'aria-labelledby': 'kgs-landing-cards-title' } : {})}
    >
      {title && (
        <h2 id="kgs-landing-cards-title" className="kgs-landing-cards__title">
          {title}
        </h2>
      )}
      <ul className="kgs-landing-cards__grid">
        {items.map((card, index) => {
          const cardTitle = textOf(card.title);
          const cardText = textOf(card.text);
          const actions = cardActions(card);
          return (
            <li key={`${cardTitle || 'card'}-${index}`}>
              <article className="kgs-landing-card">
                <CardIcon icon={card.icon} title={cardTitle} />
                {cardTitle && <TitleTag className="kgs-landing-card__title">{cardTitle}</TitleTag>}
                {cardText && <p className="kgs-landing-card__text">{cardText}</p>}
                {actions.length > 0 && (
                  <div className="kgs-landing-card__actions">
                    {actions.map((cta, actionIndex) => (
                      <LandingCta key={`${textOf(cta.label)}-${actionIndex}`} cta={cta} />
                    ))}
                  </div>
                )}
              </article>
            </li>
          );
        })}
      </ul>
    </section>
  );
};

export default FeatureCards;
