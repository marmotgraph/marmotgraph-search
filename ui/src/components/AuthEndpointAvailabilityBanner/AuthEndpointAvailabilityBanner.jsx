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

import React, { useEffect, useState } from 'react';
import useAuth from '../../hooks/useAuth';
import {faCircleExclamation} from '@fortawesome/free-solid-svg-icons/faCircleExclamation';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import './AuthEndpointAvailabilityBanner.css';

const AuthEndpointAvailabilityBanner = ({ badges }) => {
  const {
        isAuthEndpointAvailable
      } = useAuth();

  const [isPageLoaded, setIsPageLoaded] = useState(false);
    // This complexity is here just to handle cases where the endpoint availability
    // check is not yet completed - otherwise the banner will flicker.
    useEffect(() => {
      const handlePageLoad = () => {
        // We use a timeout since we need to delay showing the banner
        // until the page is fully loaded.
        setTimeout(() => {
            setIsPageLoaded(true);
        }, 1000);
      };

      if (document.readyState === 'complete') {
        handlePageLoad();
      } else {
        window.addEventListener('load', handlePageLoad);
        return () => window.removeEventListener('load', handlePageLoad);
      }
    }, []);

    if (!isPageLoaded || isAuthEndpointAvailable) {
      return null;
    }

  if(!isAuthEndpointAvailable) {
      return (
          <div className="container-fluid">
              <div className="alert text-center">
                  <span><FontAwesomeIcon icon={faCircleExclamation}/> Authentication endpoint is temporarily unavailable.</span>
              </div>
          </div>
    );
  }

};

export default AuthEndpointAvailabilityBanner;