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
import {faChevronDown} from '@fortawesome/free-solid-svg-icons/faChevronDown';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import React, {useCallback, useEffect, useId, useRef, useState} from 'react';
import {useDispatch} from 'react-redux';
import FieldsPanel from '../../../components/Field/FieldsPanel';
import {ImagePreviews} from '../../../features/image/ImagePreviews';
import './Tabs.css';
import './Overview.css';
import {setTab} from '../../../features/instance/instanceSlice';
import {Field} from '../../Field/Field';

const Tab = ({tab, active, onClick}) => {

  const handleClick = () => onClick(tab.name);

  const className = `kgs-tabs__button ${active ? 'is-active' : ''}`;
  return (
    <button type="button" className={className} onClick={handleClick}>{tab.name ? tab.name : ''}</button>
  );
};

const TabsDropdown = ({tabs, activeTab, onTabClick}) => {
  const menuId = useId();
  const wrapperRef = useRef(null);
  const [isOpen, setIsOpen] = useState(false);

  const closeMenu = useCallback(() => setIsOpen(false), []);

  useEffect(() => {
    closeMenu();
  }, [activeTab?.name, closeMenu]);

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

  const handleSelect = name => {
    closeMenu();
    onTabClick(name);
  };

  return (
    <div className="kgs-tabs__dropdown" ref={wrapperRef}>
      <button
        type="button"
        className={`kgs-tabs__dropdown-toggle${isOpen ? ' is-open' : ''}`}
        aria-haspopup="menu"
        aria-expanded={isOpen}
        aria-controls={menuId}
        onClick={() => setIsOpen(current => !current)}
      >
        <span className="kgs-tabs__dropdown-label">{activeTab?.name || ''}</span>
        <FontAwesomeIcon icon={faChevronDown} className="kgs-tabs__dropdown-chevron" aria-hidden="true" />
      </button>
      {isOpen && (
        <ul className="kgs-tabs__dropdown-menu" id={menuId} role="menu">
          {tabs.map(t => (
            <li key={t.name} role="none">
              <button
                type="button"
                className={`kgs-tabs__dropdown-item${t.name === activeTab?.name ? ' is-active' : ''}`}
                role="menuitem"
                aria-current={t.name === activeTab?.name ? 'page' : undefined}
                onClick={() => handleSelect(t.name)}
              >
                {t.name}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

const MOBILE_TABS_QUERY = '(max-width: 767px)';

const useIsMobileTabsLayout = () => {
  const [isMobile, setIsMobile] = useState(() => (
    typeof window !== 'undefined' && window.matchMedia(MOBILE_TABS_QUERY).matches
  ));

  useEffect(() => {
    const media = window.matchMedia(MOBILE_TABS_QUERY);
    const update = event => setIsMobile(event.matches);
    setIsMobile(media.matches);
    media.addEventListener('change', update);
    return () => media.removeEventListener('change', update);
  }, []);

  return isMobile;
};

const getTabSectionSelector = name => {
  const escaped = typeof CSS !== 'undefined' && typeof CSS.escape === 'function'
    ? CSS.escape(name)
    : String(name).replace(/\\/g, '\\\\').replace(/"/g, '\\"');
  return `.kgs-tabs__section[data-tab="${escaped}"]`;
};

const TabsView = ({tab}) => {
  if (!tab || !Array.isArray(tab.fields)) {
    return null;
  }

  if (tab.name === 'Overview') {
    const previews = tab.previews;
    const topFields = tab.fields.filter(f => f.mapping.layout === 'top');
    const summaryFields = tab.fields.filter(f => f.mapping.layout === 'summary');
    const mainFields = tab.fields.filter(f => f.mapping.layout !== 'top' && f.mapping.layout !== 'summary');
    return (
      <div
        className={`kgs-tabs__view kgs-tabs__overview ${(previews && previews.length) ? 'kgs-tabs__overview__with-previews' : ''}  ${(summaryFields && summaryFields.length) ? 'kgs-tabs__overview__with-summary' : ''} ${(topFields && topFields.length) ? 'kgs-tabs__overview__with-top' : ''}`}>
        <FieldsPanel className="kgs-tabs__overview__top" fields={topFields} fieldComponent={Field}/>
        <ImagePreviews className={`kgs-tabs__overview__previews ${(previews && previews.length > 1) ? 'has-many' : ''}`}
                       width="300px" images={previews}/>
        <FieldsPanel className="kgs-tabs__overview__summary" fields={summaryFields} fieldComponent={Field} group={true}/>
        <FieldsPanel className="kgs-tabs__overview__main" fields={mainFields} fieldComponent={Field}/>
      </div>
    );
  }

  return (
    <FieldsPanel className="kgs-tabs__view" fields={tab.fields} fieldComponent={Field}/>
  );
};

const TabSection = ({tab}) => (
  <section className="kgs-tabs__section" data-tab={tab.name}>
    <h2 className="kgs-tabs__section-title">{tab.name}</h2>
    <TabsView tab={tab}/>
  </section>
);

const Tabs = ({tabs, selectedTab, onTabClick}) => {
  const dispatch = useDispatch();
  const isMobile = useIsMobileTabsLayout();
  const contentRef = useRef(null);
  const selectedTabRef = useRef(selectedTab);
  const isProgrammaticScroll = useRef(false);
  const programmaticScrollTimeout = useRef(null);
  const hasContent = Array.isArray(tabs) && tabs.length > 0;
  let activeTab = selectedTab ? tabs.find(t => t.name === selectedTab) : null;
  if (!activeTab && hasContent) {
    activeTab = tabs[0];
  }
  selectedTabRef.current = activeTab?.name;

  useEffect(() => {
    if (activeTab && selectedTab !== activeTab.name) {
      dispatch(setTab(activeTab.name));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab]);

  useEffect(() => () => {
    window.clearTimeout(programmaticScrollTimeout.current);
  }, []);

  useEffect(() => {
    if (!isMobile || !hasContent) {
      return undefined;
    }

    const content = contentRef.current;
    if (!content) {
      return undefined;
    }

    const getScrollParent = element => {
      let current = element.parentElement;
      while (current) {
        const overflowY = window.getComputedStyle(current).overflowY;
        if (overflowY === 'auto' || overflowY === 'scroll') {
          return current;
        }
        current = current.parentElement;
      }
      return element;
    };

    const container = getScrollParent(content);

    const updateActiveFromScroll = () => {
      if (isProgrammaticScroll.current) {
        return;
      }

      const containerTop = container.getBoundingClientRect().top;
      const tabsButtons = container.querySelector('.kgs-tabs__buttons');
      const marker = Math.max(
        48,
        (tabsButtons?.getBoundingClientRect().bottom ?? containerTop) - containerTop + 8
      );
      const sections = Array.from(content.querySelectorAll('.kgs-tabs__section'));
      let currentName = sections[0]?.getAttribute('data-tab');
      sections.forEach(section => {
        if (section.getBoundingClientRect().top - containerTop <= marker) {
          currentName = section.getAttribute('data-tab');
        }
      });

      if (currentName && currentName !== selectedTabRef.current) {
        dispatch(setTab(currentName));
      }
    };

    container.addEventListener('scroll', updateActiveFromScroll, { passive: true });
    return () => container.removeEventListener('scroll', updateActiveFromScroll);
  }, [dispatch, hasContent, isMobile]);

  const handleNavigate = useCallback(name => {
    if (isMobile && contentRef.current) {
      const section = contentRef.current.querySelector(getTabSectionSelector(name));
      if (section) {
        isProgrammaticScroll.current = true;
        section.scrollIntoView({ behavior: 'smooth', block: 'start' });
        window.clearTimeout(programmaticScrollTimeout.current);
        programmaticScrollTimeout.current = window.setTimeout(() => {
          isProgrammaticScroll.current = false;
        }, 800);
      }
    }
    onTabClick(name);
  }, [isMobile, onTabClick]);

  if (!hasContent) {
    return null;
  }

  return (
    <>
      <div className="kgs-tabs__buttons">
        <TabsDropdown tabs={tabs} activeTab={activeTab} onTabClick={handleNavigate} />
        {tabs.map(t => (
          <Tab key={t.name} tab={t} active={t && t.name === activeTab.name} onClick={handleNavigate}/>
        ))}
      </div>
      <div className="kgs-tabs__content" ref={contentRef}>
        {isMobile ? (
          tabs.map(tab => (
            <TabSection key={tab.name} tab={tab} />
          ))
        ) : (
          <TabsView tab={activeTab}/>
        )}
      </div>
    </>
  );
};

export default Tabs;