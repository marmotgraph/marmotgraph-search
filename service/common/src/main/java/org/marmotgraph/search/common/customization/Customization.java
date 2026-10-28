/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2024 EBRAINS AISBL
 * Copyright 2024 - 2026 ETH Zurich
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  This open source software code was developed in part or in whole in the
 *  Human Brain Project, funded from the European Union's Horizon 2020
 *  Framework Programme for Research and Innovation under
 *  Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 *  (Human Brain Project SGA1, SGA2 and SGA3).
 */

package org.marmotgraph.search.common.customization;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public interface Customization {
    String getHeaderAdditions();
    String getCSSAdditions();
    Configuration getConfiguration();
    String getTermsOfUse();
    String getHelp();
    String getNavBarItems();
    String getFooterContent();
    String getFooterSocial();
    String getNotFoundFooter();

    /**
     * Optional blocks for the search landing page.
     * <p>
     * Return null to keep the default landing: the search bar and category browse, with no hero copy,
     * cards, or columns. The search bar is always shown. Every other block is omitted when it is null
     * or blank. Set {@link LandingPage#categoryBrowse()} to {@code false} to hide category browse.
     * <p>
     * A card {@code icon} is a Font Awesome solid name ({@code database}, {@code chart-bar}, {@code fa-flask})
     * or an image URL. A call to action needs both a label and a url ({@code /search}, {@code https://…}, or {@code mailto:…}).
     * A card shows {@code cta} and every entry in {@code ctas}.
     * {@link Hero#background()} is a path or URL of an SVG file, or the SVG markup. Animations in that SVG play.
     */
    default LandingPage getLandingPage() {
        return null;
    }

    record Configuration(String home, String name, String copyright, String copyrightSince, String copyrightAddition,
                         String searchExample, String logo, String logoDark, boolean login, boolean inProgressOnly) {

        public String technicalName(){
            return name().toLowerCase().replaceAll("[^A-Za-z0-9]", "");
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record LandingPage(Hero hero, Boolean categoryBrowse, CardGrid cards, ColumnPair columns) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Hero(String title, String text, String background) {
        public Hero(String title, String text) {
            this(title, text, null);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record CardGrid(String title, List<LandingCard> items) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record LandingCard(String title, String icon, String text, CallToAction cta, List<CallToAction> ctas) {
        public LandingCard(String title, String icon, String text, CallToAction cta) {
            this(title, icon, text, cta, null);
        }

        public LandingCard(String title, String icon, String text, List<CallToAction> ctas) {
            this(title, icon, text, null, ctas);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ColumnPair(LandingColumn left, LandingColumn right) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record LandingColumn(String title, String text, CallToAction cta) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record CallToAction(String label, String url) {
    }

}
