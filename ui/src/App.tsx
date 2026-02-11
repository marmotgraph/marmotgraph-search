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

import React, {Suspense} from 'react';
import {Provider, useDispatch} from 'react-redux';
import {BrowserRouter, Navigate, Route, Routes} from 'react-router-dom';
import 'normalize.css/normalize.css';
import 'bootstrap/dist/css/bootstrap.min.css';
import 'bootstrap/dist/js/bootstrap.min.js';
import FetchingPanel from './components/FetchingPanel/FetchingPanel';
import Notification from './components/Notification/Notification';
import notification from './data/notification';
import ErrorBoundary from './features/ErrorBoundary';
import {InfoPanel} from './features/InfoPanel';
import AuthProvider from './features/auth/AuthProvider';
import Authenticate from './features/auth/Authenticate';
import Groups from './features/groups/Groups';
import Settings from './features/settings/Settings';
import Theme from './features/theme/Theme';
import Footer from './pages/Footer/Footer';
import Header from './pages/Header/Header';
import type AuthAdapter from './services/AuthAdapter';
import type {Store} from 'redux';

const SearchComp = React.lazy(() => import('./pages/Search.jsx'));
const InstanceComp = React.lazy(() => import('./pages/Instance.jsx'));
const PreviewComp = React.lazy(() => import('./pages/Preview.jsx'));

const App = ({authAdapter}: { authAdapter: AuthAdapter; }) => {

  return (
    <Settings authAdapter={authAdapter}>
      <AuthProvider adapter={authAdapter}>
        <Theme/>
        <Header/>
        <main>
          <Notification className={undefined} text={notification}/>
          <ErrorBoundary>
            <Authenticate>
              <Groups>
                <Suspense fallback={<FetchingPanel message="Loading resource..."/>}>
                  <Routes>
                    <Route path="/" element={<SearchComp/>}/>
                    <Route path="/instances/:id" element={<InstanceComp/>}/>
                    <Route path="/instances/:type/:id" element={<InstanceComp/>}/>
                    <Route path="/live/:org/:domain/:schema/:version/:id" element={<PreviewComp/>}/>
                    <Route path="/live/:id" element={<PreviewComp/>}/>
                    <Route path="*" element={<Navigate to="/" replace={true}/>}/>
                  </Routes>
                </Suspense>
              </Groups>
            </Authenticate>
          </ErrorBoundary>
          <InfoPanel/>
        </main>
        <Footer/>
      </AuthProvider>
    </Settings>
  );
};

const Component = ({store, authAdapter}: { store: Store, authAdapter: AuthAdapter; }) => (
  <Provider store={store}>
    <BrowserRouter>
      <App authAdapter={authAdapter}/>
    </BrowserRouter>
  </Provider>
);
export default Component;