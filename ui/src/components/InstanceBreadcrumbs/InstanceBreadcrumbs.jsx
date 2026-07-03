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
import React, { useCallback, useEffect, useId, useRef, useState } from 'react';

import { getTypeBadgeAccentColor } from '../Badges/Badges';

import { breadcrumbItemShape, getBreadcrumbSegments } from './instanceBreadcrumbHelpers';

import './InstanceBreadcrumbs.css';

const BreadcrumbLabel = ({ title, badges, isCurrent }) => (
  <span className="kgs-instance-breadcrumbs__label">
    <span
      className="kgs-instance-breadcrumbs__dot"
      aria-hidden="true"
      style={{ '--breadcrumb-accent': getTypeBadgeAccentColor(badges) }}
    />
    <span className={`kgs-instance-breadcrumbs__text${isCurrent ? ' is-current' : ''}`}>{title}</span>
  </span>
);

const BreadcrumbSeparator = () => (
  <span className="kgs-instance-breadcrumbs__separator" aria-hidden="true">&gt;</span>
);

const BreadcrumbLink = ({ crumb, onBreadcrumbClick }) => (
  <button
    type="button"
    className="kgs-instance-breadcrumbs__link"
    onClick={() => onBreadcrumbClick?.(crumb.index, crumb.crumb)}
  >
    <BreadcrumbLabel title={crumb.crumb.title} badges={crumb.crumb.badges} />
  </button>
);

const BreadcrumbEllipsisMenu = ({ items, onBreadcrumbClick }) => {
  const menuId = useId();
  const wrapperRef = useRef(null);
  const closeTimerRef = useRef(null);
  const [isOpen, setIsOpen] = useState(false);

  const clearCloseTimer = useCallback(() => {
    if (closeTimerRef.current) {
      clearTimeout(closeTimerRef.current);
      closeTimerRef.current = null;
    }
  }, []);

  const openMenu = useCallback(() => {
    clearCloseTimer();
    setIsOpen(true);
  }, [clearCloseTimer]);

  const scheduleClose = useCallback(() => {
    clearCloseTimer();
    closeTimerRef.current = setTimeout(() => {
      setIsOpen(false);
    }, 150);
  }, [clearCloseTimer]);

  const toggleMenu = useCallback(() => {
    setIsOpen(current => !current);
  }, []);

  const closeMenu = useCallback(() => {
    clearCloseTimer();
    setIsOpen(false);
  }, [clearCloseTimer]);

  useEffect(() => () => clearCloseTimer(), [clearCloseTimer]);

  useEffect(() => {
    if (!isOpen) {
      return undefined;
    }

    const handlePointerDown = event => {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target)) {
        closeMenu();
      }
    };

    const handleKeyDown = event => {
      if (event.key === 'Escape') {
        closeMenu();
      }
    };

    document.addEventListener('mousedown', handlePointerDown);
    document.addEventListener('keydown', handleKeyDown);

    return () => {
      document.removeEventListener('mousedown', handlePointerDown);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [closeMenu, isOpen]);

  const handleItemClick = (index, crumb) => {
    closeMenu();
    onBreadcrumbClick?.(index, crumb);
  };

  return (
    <li
      className="kgs-instance-breadcrumbs__item kgs-instance-breadcrumbs__item--ellipsis"
      ref={wrapperRef}
      onMouseEnter={openMenu}
      onMouseLeave={scheduleClose}
    >
      <button
        type="button"
        className="kgs-instance-breadcrumbs__ellipsis"
        aria-haspopup="menu"
        aria-expanded={isOpen}
        aria-controls={menuId}
        aria-label={`Show ${items.length} hidden instances`}
        onClick={toggleMenu}
      >
        …
      </button>
      {isOpen && (
        <ul className="kgs-instance-breadcrumbs__menu" id={menuId} role="menu">
          {items.map(({ crumb, index }) => (
            <li key={crumb.id} role="none">
              <button
                type="button"
                className="kgs-instance-breadcrumbs__menu-item"
                role="menuitem"
                onClick={() => handleItemClick(index, crumb)}
              >
                <BreadcrumbLabel title={crumb.title} badges={crumb.badges} />
              </button>
            </li>
          ))}
        </ul>
      )}
      <BreadcrumbSeparator />
    </li>
  );
};

const InstanceBreadcrumbs = ({ className, breadcrumbs, onBreadcrumbClick }) => {
  if (!breadcrumbs || breadcrumbs.length <= 1) {
    return null;
  }

  const { leading, collapsed, trailing, current } = getBreadcrumbSegments(breadcrumbs);

  return (
    <nav
      className={`kgs-instance-breadcrumbs${className ? ` ${className}` : ''}`}
      aria-label="Instance navigation"
    >
      <ol className="kgs-instance-breadcrumbs__list">
        {leading.map(item => (
          <li key={item.crumb.id} className="kgs-instance-breadcrumbs__item">
            <BreadcrumbLink crumb={item} onBreadcrumbClick={onBreadcrumbClick} />
            <BreadcrumbSeparator />
          </li>
        ))}
        {collapsed.length > 0 && (
          <BreadcrumbEllipsisMenu items={collapsed} onBreadcrumbClick={onBreadcrumbClick} />
        )}
        {trailing.map(item => (
          <li key={item.crumb.id} className="kgs-instance-breadcrumbs__item">
            <BreadcrumbLink crumb={item} onBreadcrumbClick={onBreadcrumbClick} />
            <BreadcrumbSeparator />
          </li>
        ))}
        <li className="kgs-instance-breadcrumbs__item kgs-instance-breadcrumbs__item--current">
          <span className="kgs-instance-breadcrumbs__current" aria-current="page">
            <BreadcrumbLabel title={current.title} badges={current.badges} isCurrent />
          </span>
        </li>
      </ol>
    </nav>
  );
};

InstanceBreadcrumbs.propTypes = {
  className: PropTypes.string,
  breadcrumbs: PropTypes.arrayOf(breadcrumbItemShape),
  onBreadcrumbClick: PropTypes.func
};

export default InstanceBreadcrumbs;
