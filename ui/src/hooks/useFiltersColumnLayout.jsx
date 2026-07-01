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

import { useEffect } from 'react';

const DESKTOP_MIN_WIDTH = 992;

const measureStickyTop = () => {
  const fixedSearch = document.querySelector('.kgs-search-panel.is-fixed-position');
  if (!fixedSearch) {
    return 0;
  }
  return Math.ceil(fixedSearch.getBoundingClientRect().height);
};

const applyLayoutVars = rootEl => {
  if (!rootEl) {
    return;
  }

  if (window.innerWidth < DESKTOP_MIN_WIDTH) {
    rootEl.style.removeProperty('--filters-column-sticky-top');
    rootEl.style.removeProperty('--filters-column-available-height');
    return;
  }

  const stickyTop = measureStickyTop();
  const filtersEl = rootEl.querySelector('.kgs-search__filters');
  const filtersTop = filtersEl
    ? Math.ceil(filtersEl.getBoundingClientRect().top)
    : stickyTop;
  const offsetTop = Math.max(stickyTop, filtersTop);

  rootEl.style.setProperty('--filters-column-sticky-top', `${stickyTop}px`);
  rootEl.style.setProperty(
    '--filters-column-available-height',
    `calc(100dvh - ${offsetTop}px)`
  );
};

const useFiltersColumnLayout = (rootRef, isActive) => {
  useEffect(() => {
    if (!isActive) {
      return undefined;
    }

    let rafId = null;

    const scheduleUpdate = () => {
      if (rafId !== null) {
        cancelAnimationFrame(rafId);
      }
      rafId = requestAnimationFrame(() => {
        rafId = null;
        applyLayoutVars(rootRef.current);
      });
    };

    scheduleUpdate();
    window.addEventListener('scroll', scheduleUpdate, { passive: true });
    window.addEventListener('resize', scheduleUpdate);

    const searchPanel = document.querySelector('.kgs-search-panel');
    const observer = searchPanel ? new MutationObserver(scheduleUpdate) : null;
    if (observer && searchPanel) {
      observer.observe(searchPanel, {
        attributes: true,
        attributeFilter: ['class'],
      });
    }

    return () => {
      if (rafId !== null) {
        cancelAnimationFrame(rafId);
      }
      window.removeEventListener('scroll', scheduleUpdate);
      window.removeEventListener('resize', scheduleUpdate);
      observer?.disconnect();
      rootRef.current?.style.removeProperty('--filters-column-sticky-top');
      rootRef.current?.style.removeProperty('--filters-column-available-height');
    };
  }, [rootRef, isActive]);
};

export default useFiltersColumnLayout;
