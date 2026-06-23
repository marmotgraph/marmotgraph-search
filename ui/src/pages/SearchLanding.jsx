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

import { faInfoCircle } from '@fortawesome/free-solid-svg-icons/faInfoCircle';
import { faSearch } from '@fortawesome/free-solid-svg-icons/faSearch';
import { faTimes } from '@fortawesome/free-solid-svg-icons/faTimes';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useLocation, useNavigate } from 'react-router-dom';

import { setInfo } from '../features/application/applicationSlice';
import { setQueryString } from '../features/search/searchSlice';
import { getLocationSearchFromQuery, searchToObj } from '../helpers/BrowserHelpers';

import CategoryBrowse from './SearchLanding/CategoryBrowse';
import '../features/search/SearchBox.css';
import './SearchLanding.css';

const isFacetParam = key => /\[\d+\]$/.test(key);

const isAuthHash = hash => {
  if (!hash || hash === '#') {
    return false;
  }
  const value = hash.slice(1);
  return value.startsWith('error')
    || /(^|[&;])(state|session_state|code|iss)=/.test(value);
};

const isSearchDetailHash = hash => {
  if (!hash || hash === '#') {
    return false;
  }
  const value = hash.slice(1);
  return value.length > 0 && !value.includes('=');
};

const shouldRedirectToSearch = () => {
  const params = searchToObj();

  if (params.q?.trim()) {
    return true;
  }

  if (Object.keys(params).some(key => key === 'category' || key.startsWith('category['))) {
    return true;
  }

  if (Object.keys(params).some(key => isFacetParam(key))) {
    return true;
  }

  return isSearchDetailHash(window.location.hash);
};

const SearchLanding = () => {
  const configuration = useSelector(state => state.application.config);
  const group = useSelector(state => state.groups.group);
  const defaultGroup = useSelector(state => state.groups.defaultGroup);
  const help = useSelector(state => state.application.custom.help);

  const dispatch = useDispatch();
  const navigate = useNavigate();
  const location = useLocation();

  const inputRef = useRef(null);
  const [value, setValue] = useState('');

  useEffect(() => {
    const hash = location.hash;
    if (shouldRedirectToSearch() && !isAuthHash(hash)) {
      navigate(
        { pathname: '/search', search: location.search, hash },
        { replace: true }
      );
    }
  }, [location.hash, location.search, navigate]);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  const buildSearchPath = useCallback(query => {
    const queryParams = {};
    if (query) {
      queryParams.q = query;
    }
    if (group && group !== defaultGroup) {
      queryParams.group = group;
    }
    const search = getLocationSearchFromQuery(queryParams);
    return `/search${search}`;
  }, [group, defaultGroup]);

  const handleSearch = useCallback(() => {
    const query = value.trim();
    dispatch(setQueryString(query));
    navigate(buildSearchPath(query));
  }, [buildSearchPath, dispatch, navigate, value]);

  const handleCategorySelect = useCallback(typeName => {
    const queryParams = { category: typeName };
    if (group && group !== defaultGroup) {
      queryParams.group = group;
    }
    dispatch(setQueryString(''));
    navigate(`/search${getLocationSearchFromQuery(queryParams)}`);
  }, [defaultGroup, dispatch, group, navigate]);

  const handleReset = () => {
    setValue('');
    inputRef.current?.focus();
  };

  const handleKeyDown = e => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  const handleHelp = () => {
    dispatch(setInfo(help));
  };

  return (
    <div className="kgs-search-landing">
      <div className="kgs-search-landing__content">
        <div className="kgs-search-panel kgs-search-landing__search">
          <div>
            <div>
              <FontAwesomeIcon icon={faSearch} size="1x" className="kg-search-bar__icon" />
              <input
                ref={inputRef}
                className="kg-search-bar"
                type="text"
                placeholder={`Search ${configuration.searchExample}`}
                aria-label="Search"
                value={value}
                onChange={e => setValue(e.target.value)}
                onKeyDown={handleKeyDown}
              />
              {!!value.length && (
                <button type="button" className="kgs-search-panel-reset__button" title="Clear" onClick={handleReset}>
                  <FontAwesomeIcon icon={faTimes} size="2x" />
                </button>
              )}
              <button type="button" className="kgs-search-panel-help__button" title="Help" onClick={handleHelp}>
                <FontAwesomeIcon icon={faInfoCircle} size="2x" />
              </button>
            </div>
            <button type="button" className="kgs-search-panel-button" onClick={handleSearch}>
              Search
            </button>
          </div>
        </div>
        <CategoryBrowse onCategorySelect={handleCategorySelect} />
      </div>
    </div>
  );
};

export default SearchLanding;
