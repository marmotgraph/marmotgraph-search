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

package org.marmotgraph.search.api;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.marmotgraph.sdk.MarmotGraphException;
import org.marmotgraph.sdk.auth.AuthenticationInformation;
import org.marmotgraph.search.common.configuration.CoreAuthentication;
import org.marmotgraph.search.common.model.CommitInfo;
import org.marmotgraph.search.common.customization.Customization;
import org.marmotgraph.search.controller.settings.AuthEndpointCheck;
import org.marmotgraph.search.controller.settings.SettingsController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@RestController
public class Settings {

    private static final Logger logger = LoggerFactory.getLogger(Settings.class);
    private static final String REALMS_PATH = "/realms/";

    private final CoreAuthentication coreAuthentication;
    private final SettingsController definitionController;
    private final AuthEndpointCheck authEndpointCheck;
    private final Customization customization;
    private final CommitInfo commitInfo;

    @GetMapping("/api/settings/custom.css")
    public String getCSS() {
        return customization.getCSSAdditions();
    }

    public record KeycloakConfig(String realm, String url, String clientId, boolean authEndpointAvailable) {

        /**
         * The configuration of the Keycloak client of the UI, derived from the authentication settings of the core:
         * The issuer of a Keycloak realm is "{keycloak base url}/realms/{realm}" and the client id is the login client
         * advertised by the core.
         */
        static KeycloakConfig from(AuthenticationInformation authentication, boolean authEndpointAvailable) {
            String issuer = StringUtils.removeEnd(authentication.issuer(), "/");
            int realmsIndex = issuer.lastIndexOf(REALMS_PATH);
            if (realmsIndex < 0) {
                logger.warn("The issuer {} is not the one of a Keycloak realm - can not determine the realm", issuer);
                return new KeycloakConfig(null, issuer, authentication.loginClientId(), authEndpointAvailable);
            }
            return new KeycloakConfig(issuer.substring(realmsIndex + REALMS_PATH.length()), issuer.substring(0, realmsIndex), authentication.loginClientId(), authEndpointAvailable);
        }
    }

    public record SentryConfig(String dsn, String release, String environment) {
    }

    public record MatomoConfig(String url, String siteId) {
    }

    public record CustomSections(String termsOfUse, String help, String navbarItems, String footerContent,
                                 String footerSocial, String editorEndpoint, String notFoundFooter,
                                 String dataIssueReportSlackUrl, Customization.LandingPage landingPage) {
    }

    public record Setting(String commit, SentryConfig sentry, KeycloakConfig keycloak, MatomoConfig matomo,
                          Customization.Configuration config, CustomSections custom, List<Object> types,
                          Map<String, Object> typeMappings) {
    }


    @GetMapping("/api/settings")
    public Setting getSettings(
            @Value("${eu.ebrains.kg.commit}") String commit,
            @Value("${sentry.dsn.ui}") String sentryDsnUi,
            @Value("${sentry.environment}") String sentryEnvironment,
            @Value("${matomo.url}") String matomoUrl,
            @Value("${matomo.siteId}") String matomoSiteId,
            @Value("${EDITOR_ENDPOINT:}") String editorEndpoint,
            @Value("${DATA_ISSUE_REPORT_SLACK_URL:}") String dataIssueReportSlackUrl
    ) {
        String finalCommit = null;
        SentryConfig sentryConfig = null;
        KeycloakConfig keycloakConfig = null;
        if (StringUtils.isNotBlank(commitInfo.toString())) {
            finalCommit = commitInfo.toString();
        }
        else if (StringUtils.isNotBlank(commit) && !commit.equals("\"\"")) {
            finalCommit = commit;
        }

        if (StringUtils.isNotBlank(sentryDsnUi)) {
            sentryConfig = new SentryConfig(sentryDsnUi, commit, sentryEnvironment);
        }

        try {
            keycloakConfig = KeycloakConfig.from(coreAuthentication.authenticationInformation(), authEndpointCheck.checkAuthEndpointIsAlive());
        } catch (MarmotGraphException e) {
            logger.warn("Was not able to load the authentication configuration from the MarmotGraph core", e);
        }

        MatomoConfig matomoConfig = null;
        if (StringUtils.isNotBlank(matomoUrl) && StringUtils.isNotBlank(matomoSiteId)) {
            matomoConfig = new MatomoConfig(matomoUrl, matomoSiteId);
        }
        CustomSections customSections = new CustomSections(customization.getTermsOfUse(), customization.getHelp(), customization.getNavBarItems(), customization.getFooterContent(), customization.getFooterSocial(), editorEndpoint, customization.getNotFoundFooter(), dataIssueReportSlackUrl, customization.getLandingPage());
        return new Setting(finalCommit, sentryConfig, keycloakConfig, matomoConfig, customization.getConfiguration(), customSections, definitionController.generateCategories(), definitionController.generateTypeMappings());
    }
}