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

import React, {useContext} from 'react';

import { Select } from '../Select/Select';

import './VersionSelector.css';
import {requestInstance} from '../../features/instance/instanceSlice';
import {useNavigate} from 'react-router-dom';
import {useDispatch, useSelector} from 'react-redux';
import OutdatedVersionDisclaimer from '../OutdatedVersionDisclaimer';
import {useInstance} from '../../contexts/InstanceContext';

const getVersionValue = (versions, version) => {
  if (!Array.isArray(versions)) {
    return null;
  }
  const res = versions.find(v => v.label === version);
  if (res) {
    return res.value;
  }
  return null;
};



export const VersionSelector = () => {
  const { isSearch, path } = useInstance();
  const navigate = useNavigate();
  const data = useSelector(state => state.instance.data);
  const selectedTab = useSelector(state => state.instance.tab);
  const dispatch = useDispatch();
  const group = useSelector(state => state.groups.group);
  const defaultGroup = useSelector(state => state.groups.defaultGroup);
  const category = data?.category;

  const getVersions = (latestVersion, versions) => {
    const result = (Array.isArray(versions) ? versions : [])
      .map(v => ({
        label: v.value && latestVersion && v.value === latestVersion.value ? v.value + " - latest" : v.value ?? 'Current',
        value: v.reference
      }));
    return result.length > 1 ? result : [];
  };

  const onVersionChange = version => {
    const context = {
      tab: selectedTab
    };
    if(isSearch) {
      dispatch(requestInstance({
        instanceId: version,
        context: context
      }));
    } else {
      navigate(`${path}${version}${(group && group !== defaultGroup)?('?group=' + group ):''}`, { state: context});
    }
  };
  const latestVersion = data?.versions?.length>0 ? data?.versions[0] : null;
  const version = data?.version??'Current';
  const versions = getVersions(latestVersion, data?.versions);

  if(!versions?.length){
    return null;
  }
  if (!Array.isArray(versions)) {
    if(!version || typeof version !== 'string' || version === 'Current') {
      return null;
    }
    return version;
  }
  const value = getVersionValue(versions, version);
  let outdated = !latestVersion || latestVersion.reference !== data?.id;
  return (
    <div className="kgs-version_selector">
       <OutdatedVersionDisclaimer latestVersion={latestVersion} type={category} isOutdated={outdated} />
       <Select value={value} list={versions} onChange={onVersionChange} />

    </div>
  );
};