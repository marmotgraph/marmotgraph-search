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

import PropTypes from 'prop-types';

export const breadcrumbItemShape = PropTypes.shape({
  id: PropTypes.string.isRequired,
  title: PropTypes.string,
  badges: PropTypes.arrayOf(PropTypes.string)
});

export const breadcrumbNavigationShape = PropTypes.shape({
  breadcrumbs: PropTypes.arrayOf(breadcrumbItemShape),
  onBreadcrumbClick: PropTypes.func
});

export const buildInstanceBreadcrumbs = (history, current, currentTitle) => {
  if (!current?.id) {
    return [];
  }
  return [
    ...history.map(entry => ({
      id: entry.id,
      title: entry.title || entry.id,
      badges: entry.badges
    })),
    {
      id: current.id,
      title: currentTitle || current.title || current.id,
      badges: current.badges
    }
  ];
};

export const buildInstanceCarouselData = (history, current) => {
  if (!current) {
    return [];
  }
  return [
    ...history.map(() => null),
    current
  ];
};

export const COLLAPSE_THRESHOLD = 5;

export const getBreadcrumbSegments = breadcrumbs => {
  const lastIndex = breadcrumbs.length - 1;

  if (breadcrumbs.length < COLLAPSE_THRESHOLD) {
    return {
      leading: breadcrumbs.slice(0, lastIndex).map((crumb, index) => ({ crumb, index })),
      collapsed: [],
      trailing: [],
      current: breadcrumbs[lastIndex]
    };
  }

  return {
    leading: [{ crumb: breadcrumbs[0], index: 0 }],
    collapsed: breadcrumbs.slice(1, lastIndex - 1).map((crumb, offset) => ({
      crumb,
      index: offset + 1
    })),
    trailing: [{ crumb: breadcrumbs[lastIndex - 1], index: lastIndex - 1 }],
    current: breadcrumbs[lastIndex]
  };
};
