/*
*   Copyright (c) 2018, EPFL/Human Brain Project PCO
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/

import React from "react";
import { connect } from "react-redux";
import { Route, Switch } from "react-router-dom";
import * as Sentry from "@sentry/browser";
import Cookies from "universal-cookie";

import * as actions from "../../actions/actions";
// import { Notification } from "../Notification/Notification";
import { Search } from "../Search/Search";
import { Instance } from "../Instance/Instance";
import { NotFound } from "../../components/NotFound/NotFound";
import { Preview } from "../Instance/Preview";
import { FetchingPanel } from "../Fetching/FetchingPanel";
import { InfoPanel } from "../Info/InfoPanel";
import "./App.css";
import { SessionExpiredErrorPanel } from "../Error/ErrorPanel";

class App extends React.Component {
  constructor(props) {
    super(props);
    this.props.initialize(this.props.location);
  }

  componentDidMount() {
    const cookies = new Cookies();
    const sentryUrl = cookies.get("sentry_url");
    if (sentryUrl) {
      Sentry.init({
        dsn: sentryUrl
      });
    }
  }

  render() {
    return (
      <div className="kgs-app">
        {/* <Notification /> */}
        {this.props.isReady && (
          <Switch>
            <Route path="/instances/:type/:id" exact component={Instance} />
            <Route path="/live/:org/:domain/:schema/:version/:id" exact component={Preview} />
            <Route path="/" exact component={Search} />
            <Route component={NotFound} />
          </Switch>
        )}
        <FetchingPanel />
        <SessionExpiredErrorPanel />
        <InfoPanel />
      </div>
    );
  }
}

export default connect(
  state => ({
    location: state.router.location,
    defaultGroup: state.groups.defaultGroup,
    isReady: state.application.isReady && !state.auth.error
  }),
  dispatch => ({
    initialize: location => dispatch(actions.initialize(location))
  })
)(App);