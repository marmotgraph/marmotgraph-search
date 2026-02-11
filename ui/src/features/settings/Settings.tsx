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

import React, {useEffect, useRef, useState} from 'react';
import { useDispatch } from 'react-redux';

import BgError from '../../components/BgError/BgError';
import FetchingPanel from '../../components/FetchingPanel/FetchingPanel';
import Matomo from '../../services/Matomo';
import Sentry from '../../services/Sentry';
import { useGetSettingsQuery, getError } from '../../services/api';
import {setCommit, setConfig, setCustom} from '../application/applicationSlice';
import type AuthAdapter from '../../services/AuthAdapter';
import type { ReactNode } from 'react';
import {matchPath, useLocation, useNavigate} from 'react-router-dom';
import {getHashKey, searchToObj} from '../../helpers/BrowserHelpers';
import {setInitialGroup, setUseGroups} from '../groups/groupsSlice';
interface SettingsProps {
  authAdapter: AuthAdapter;
  children?: ReactNode;
}

const Settings = ({ authAdapter, children}: SettingsProps) => {

  const [isReady, setReady] = useState(false);
  const initializedRef = useRef(false);
  const dispatch = useDispatch();
  const [loginRequired, setLoginRequired] = useState(false);
  const [isNoSilentSSO, setIsNoSilentSSO] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  const {
    data: settings,
    error,
    isUninitialized,
    //isLoading,
    isFetching,
    isError,
    refetch,
  } = useGetSettingsQuery(undefined);

  useEffect(() => {
    if (settings && !isReady) {
      Matomo.initialize(settings?.matomo);
      Sentry.initialize(settings?.sentry);
      dispatch(setCommit(settings?.commit));
      dispatch(setConfig(settings?.config));
      dispatch(setCustom(settings?.custom));
      authAdapter.setConfig(settings.keycloak);
      const isLive = !!matchPath({path: '/live/*'}, location.pathname);
      const group = settings?.config.inProgressOnly ? "curated" : (searchToObj() as { [key: string]: string })['group'];
      const hasGroup = !isLive && (group === 'public' || group === 'curated');
      const hasAuthSession = !!getHashKey('session_state');
      const noSilentSSO = (searchToObj() as { [key: string]: string })['noSilentSSO'];

      setIsNoSilentSSO(window.location.host.startsWith('localhost') || (noSilentSSO === 'true' && !isLive && !group));
      const instance = !hasAuthSession && location.pathname === '/' && !location.hash.startsWith('#error') && location.hash.substring(1);
      if (instance) {
        const url = `/instances/${instance}${hasGroup ? ('?group=' + group) : ''}`;
        navigate(url, {replace: true});
      }

      const authMode = hasAuthSession || isLive || hasGroup || settings?.config.inProgressOnly;
      const useGroups = hasAuthSession && !isLive;
      if (hasGroup) {
        dispatch(setInitialGroup(group));
      }

      if (authMode) {
        if (useGroups) {
          dispatch(setUseGroups());
        }
        setLoginRequired(true);
        setReady(true);
      } else {
        setReady(true);
      }
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [settings, isReady, dispatch, location.hash, location.pathname, navigate]);

  if (isError) {
    return (
      <BgError message={getError(error)} onRetryClick={refetch} retryLabel="Retry" retryVariant="primary" />
    );
  }

  if(isUninitialized || isFetching) {
    return (
      <FetchingPanel message="Retrieving application configuration..." />
    );
  }

  if (isReady) {
    return (
      <>
        {children}
      </>
    );
  }

  return null;
};

export default Settings;