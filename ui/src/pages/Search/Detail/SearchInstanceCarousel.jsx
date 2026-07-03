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
import React from 'react';
import { useSelector } from 'react-redux';

import Carousel from '../../../components/Carousel/Carousel';
import {
  buildInstanceCarouselData,
  useInstanceBreadcrumbs
} from '../../../components/InstanceBreadcrumbs';

const SearchInstanceCarousel = ({
  className,
  itemComponent,
  navigationComponent,
  onBack,
  onClose
}) => {
  const current = useSelector(state => state.instance.data);
  const history = useSelector(state => state.instance.history);
  const breadcrumbNavigation = useInstanceBreadcrumbs();

  if (!current) {
    return null;
  }

  return (
    <Carousel
      className={className}
      data={buildInstanceCarouselData(history, current)}
      itemComponent={itemComponent}
      navigationComponent={navigationComponent}
      breadcrumbNavigation={breadcrumbNavigation}
      onBack={onBack}
      onClose={onClose}
    />
  );
};

SearchInstanceCarousel.propTypes = {
  className: PropTypes.string,
  itemComponent: PropTypes.oneOfType([
    PropTypes.element,
    PropTypes.func,
    PropTypes.object
  ]).isRequired,
  navigationComponent: PropTypes.oneOfType([
    PropTypes.element,
    PropTypes.func,
    PropTypes.object
  ]),
  onBack: PropTypes.func.isRequired,
  onClose: PropTypes.func.isRequired
};

export default SearchInstanceCarousel;
