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

import {faClipboard} from '@fortawesome/free-solid-svg-icons/faClipboard';
import React from 'react';
import CopyToClipboardButton from '../CopyToClipboard/CopyToClipboardButton';
import FavoriteButton from '../../features/FavoriteButton';
import {useSelector} from 'react-redux';

const getShareEmailToLink = url => {
  const to = '';
  const subject = 'Knowledge Graph Search Request';
  const body = 'Please have a look to the following Knowledge Graph search request';
  return `mailto:${to}?subject=${subject}&body=${body} ${encodeURIComponent(url)}.`;
};

const ShareButtons = ({ url, instanceId }) => {
  const editorEndpoint = useSelector(state => state.custom?.editorEndpoint);

  const link = getShareEmailToLink(url);
  return (
    <span className="kgs-share-links" >
      <span className="kgs-share-links-panel">
        <FavoriteButton />
        <span className="item">
          {editorEndpoint != null ?
        <a href={editorEndpoint+"/instances/" + instanceId} target={'_blank'}>
         <svg className="svg-inline--fa" viewBox="0 0 470 509" fill="none" xmlns="http://www.w3.org/2000/svg">
<rect x="23.5" y="23.5" width="181" height="154" rx="25" stroke="currentColor" stroke-width="47"/>
<rect x="23.5" y="236.5" width="181" height="249" rx="25" stroke="currentColor" stroke-width="47"/>
<rect x="265.5" y="331.5" width="181" height="154" rx="25" stroke="currentColor" stroke-width="47"/>
<rect x="265.5" y="23.5" width="181" height="249" rx="25" stroke="currentColor" stroke-width="47"/>
</svg>
</a> : null }
        </span>
        <CopyToClipboardButton icon={faClipboard} title="Copy search link to clipboard"
                               confirmationText="search link copied to clipoard" content={url} className="item"/>
      </span>
    </span>
  );
};

export default ShareButtons;