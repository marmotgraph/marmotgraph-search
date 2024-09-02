import type { KeycloakConfig } from 'keycloak-js';

interface ExtendedKeycloakConfig extends KeycloakConfig {
    authEndpointAvailable: boolean;
}

export default ExtendedKeycloakConfig;