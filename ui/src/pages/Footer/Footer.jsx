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
import { connect } from 'react-redux';
import './Footer.css';

const Footer = ({ commit, configuration, custom, theme }) => (
    <footer className="site-footer">
      <div className="footer__header">
        <div className="footer__primary">
          <a href={configuration.home} aria-label="Homepage" title="Homepage"
                                              className="logo nuxt-link-exact-active nuxt-link-active"> <img
                  src={`/api/assets/img/${theme === 'dark' ? configuration.logoDark : configuration.logo}`}
                  height="100"/>
          </a>
        </div>
      </div>
      <div className="footer__content" dangerouslySetInnerHTML={{__html: custom.footerContent}}></div>
      <hr className="full-width"/>
      <div className="footer__end">
        <div className="footer__copyright">
          &copy;{configuration.copyrightSince !== new Date().getFullYear().toString() && configuration.copyrightSince + '-'}{new Date().getFullYear()}&nbsp;{configuration.copyright}
        </div>
        <div className="commit">
          {commit && <span>build: <i>{commit}</i></span>}
        </div>
        <ul className="footer__social" dangerouslySetInnerHTML={{__html: custom.footerSocial}}>
        </ul>
      </div>
      {configuration.copyrightAddition && <p className="footer__copyright_addition">{configuration.copyrightAddition}</p>}
    </footer>
);

export default connect(
    state => ({
      commit: state.application.commit,
      configuration: state.application.config,
      custom: state.application.custom,
      theme: state.application.theme
  })
)(Footer);