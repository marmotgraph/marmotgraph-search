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

import React, {useMemo} from 'react';
import { useSelector, useDispatch } from 'react-redux';

import BgError from '../../components/BgError/BgError';
import Disclaimer from '../../components/Disclaimer/Disclaimer';
import TermsShortNotice from '../../features/TermsShortNotice';
import { selectIsDefaultGroup, selectGroupLabel } from '../../features/groups/groupsSlice';
import ImagePopup from '../../features/image/ImagePopup';
import { requestInstance, setTab, selectTypeMapping } from '../../features/instance/instanceSlice';
import Matomo from '../../services/Matomo';

import Header from './Header/Header';
import Tabs from './Tabs/Tabs';
import { createContext } from 'react';
import './InstanceView.css';
import './Fields.css';
import {InstanceContext} from '../../contexts/InstanceContext';

const getField = (type, name, data, mapping) => {
  if (name === 'type') {
    return {
      name: 'type',
      data: { value: type },
      mapping: { },
      type: type
    };
  }
  return {
    name: name,
    data: data,
    mapping: mapping,
    type: type
  };
};

const getHeaderFields = (type, data, mapping) => {
  if (!data || !mapping) {
    return [];
  }

  return Object.entries(mapping.fields || {})
    .filter(
      ([name, fieldsMapping]) =>
        fieldsMapping &&
        data?.[name] &&
        fieldsMapping.layout === 'header' &&
        name !== 'title'
    )
    .map(([name, fieldsMapping]) =>
      getField(type, name, data[name], fieldsMapping)
    );
};

const getFieldsByTabs = (type, data, typeMapping, previews) => {
  if (!data || !typeMapping) {
    return [];
  }

  const overviewFields = [];
  const tabs = Object.entries(typeMapping.fields || {})
    .filter(
      ([name, mapping]) =>
        mapping &&
        data?.[name] &&
        mapping.layout !== 'header' &&
        name !== 'title' // title is displayed in the header
    )
    .reduce((acc, [name, mapping]) => {
      const groupName =
        !mapping.layout || mapping.layout === 'summary' ? null : mapping.layout;
      const field = getField(type, name, data[name], mapping);
      if (!groupName) {
        overviewFields.push(field);
      } else {
        if (!acc[groupName]) {
          acc[groupName] = {
            name: groupName,
            fields: []
          };
        }
        acc[groupName].fields.push(field);
      }
      return acc;
    }, {});

  if (overviewFields.length) {
    return [
      {
        name: 'Overview',
        fields: overviewFields,
        previews: previews
      },
      ...Object.values(tabs)
    ];
  }
  return Object.values(tabs);
};


const getTags = (groupLabel, isDefaultGroup, category) => {
  const tags = [];
  // if (!isDefaultGroup && groupLabel) {
  //   tags.push(groupLabel);
  // }
    // if (category) {
    //   tags.push(category);
    // }
  return tags;
};


const InstanceView = ({ data, isSearch, path, customNavigationComponent }) => {
  const dispatch = useDispatch();
  const type = data?.type;
  const category = data?.category;
  const fields = data?.fields;
  const mapping =  useSelector(state => selectTypeMapping(state, fields?.mappingKey ? fields?.mappingKey : category));
  const hasNoData = !fields;
  const hasUnknownData = !mapping;
  const group = useSelector(state => state.groups.group);
  const isDefaultGroup = useSelector(state => selectIsDefaultGroup(state));
  const groupLabel = useSelector(state => selectGroupLabel(state, group));
  const headerFields = getHeaderFields(type, fields, mapping);
  const selectedTab = useSelector(state => state.instance.tab);
  const tabs = getFieldsByTabs(type, data?.fields, mapping, data?.previews);
  const tags = getTags(groupLabel, isDefaultGroup, data?.category);
  const badges = data?.badges;


  const handleTabClick = tab => {
    if(tab !== selectedTab) {
      Matomo.trackEvent('Tab', 'Clicked', tab);
      dispatch(setTab(tab));
    }
  };

  if (hasNoData) {
    return(
      <BgError message="This data is currently not available." />
    );
  }

  if (hasUnknownData) {
    return(
      <BgError message="This type of data is currently not supported." />
    );
  }
  const instanceConfig = { isSearch: isSearch, path: path };
  return (
    <InstanceContext.Provider value={instanceConfig}>
    <div className="kgs-instance" data-type={type}>
      <Header title={data?.title} tags={tags} badges={badges} fields={headerFields} customNavigationComponent={customNavigationComponent} highlightColor={data?.highlightColor} category={category} />
      <Tabs tabs={tabs} selectedTab={selectedTab} onTabClick={handleTabClick} />
      <div className="kgs-instance__footer">
        <Disclaimer content={data?.disclaimer} />
      </div>
      <TermsShortNotice />
      <ImagePopup className="kgs-instance__image_popup" />
    </div>
    </InstanceContext.Provider>
  );
};

export default InstanceView;