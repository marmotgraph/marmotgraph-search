import { KeycloakConfig } from 'keycloak-js';

interface ExtendedKeycloakConfig extends KeycloakConfig {
    authEndpointAvailable: boolean;
}