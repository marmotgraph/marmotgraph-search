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
import renderer from "react-test-renderer";
import Enzyme, { mount, shallow, render } from "enzyme";
import Adapter from "enzyme-adapter-react-16";
import Tabs from "./Tabs";

Enzyme.configure({ adapter: new Adapter() });

test('Tabs test className"', () => {
    const component = shallow(
        <Tabs className="className" tabs={[{id: "an id", title: "a label", counter: 11, hint: {show: true, value: "a hint"}},{id: "another id", title: "another label", counter: 32, hint: {show: true, value: "another hint"}}]}  viewComponent={() => null} />
    );
    expect(component.hasClass("className"));
});

test('Tabs test number of tabs', () => {
    const component = render(
        <Tabs className="className" tabs={[{id: "an id", title: "a label", counter: 11, hint: {show: true, value: "a hint"}},{id: "another id", title: "another label", counter: 32, hint: {show: true, value: "another hint"}}]}  viewComponent={() => null} />
    );
    expect(component.find(".kgs-tabs-button").length).toBe(2);
});

test('Tabs test active tab', () => {
    const component = mount(
        <Tabs className="className" tabs={[{id: "an id", title: "a label", counter: 11, hint: {show: true, value: "a hint"}},{id: "another id", title: "another label", counter: 32, hint: {show: true, value: "another hint"}}]}  viewComponent={() => null} />
    );
    
    expect(component.find("button.kgs-tabs-button").at(0).hasClass("is-active"));
    expect(component.find("button.kgs-tabs-button").at(1).hasClass("is-active")).toBe(false);

    component.find('button.kgs-tabs-button').at(1).simulate('click');

    expect(component.find("button.kgs-tabs-button").at(0).hasClass("is-active")).toBe(false);
    expect(component.find("button.kgs-tabs-button").at(1).hasClass("is-active"));
});