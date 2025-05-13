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

import React from 'react';

import './Badges.css';

const BadgesEnum = [
  { name: 'isNew', title: 'New', color: '#0DCAF0FF', fontColor: '#FFFFFFFF' },
  { name: 'isTrending', title: 'Top trending', color: '#20C997FF', fontColor: '#FFFFFFFF' }
];

const Badge = ({ name, title, style }) => <span className={`badge rounded-pill kgs-badge kgs-badge-${name}`} style={style}>{title}</span>;

const Badges = ({ badges }) => {
  const allBadges = [];
  badges.forEach(b => {
    const fixedBadge = BadgesEnum.filter(fixedBadge => fixedBadge.name === b)[0]
    if(fixedBadge !== undefined){
      allBadges.push(fixedBadge);
    }
    else{
      const splittedBadge = b.split(";");
      if(splittedBadge.length>1) {
        allBadges.push({
          name: splittedBadge[0].trim(),
          title: splittedBadge[0].trim(),
          color: splittedBadge[1].trim(),
          fontColor: splittedBadge.length > 2 ? splittedBadge[2].trim() : '#FFFFFFFF'
        })
      }
    }
  })
  return (
    <div className="kgs-badges">
      {allBadges.map(badge => <Badge key={badge.name} name={badge.name} title={badge.title} style={{
        background: badge.color,
        color: badge.fontColor
      }} />)}
    </div>
  );
};

export default Badges;