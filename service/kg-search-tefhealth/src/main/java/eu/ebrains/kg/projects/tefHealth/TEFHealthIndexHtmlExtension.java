package eu.ebrains.kg.projects.tefHealth;

import eu.ebrains.kg.common.controller.translation.IndexHtmlExtension;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Profile("tef-health")
public class TEFHealthIndexHtmlExtension implements IndexHtmlExtension {

    @Override
    public String getHeaderAdditions() {
        return """
                <link rel="apple-touch-icon" sizes="57x57" href="https://tefhealth.eu/files/tef-health/layout/favicon/apple-icon-57x57.png">
                <link rel="apple-touch-icon" sizes="60x60" href="https://tefhealth.eu/files/tef-health/layout/favicon/apple-icon-60x60.png">
                <link rel="apple-touch-icon" sizes="72x72" href="https://tefhealth.eu/files/tef-health/layout/favicon/apple-icon-72x72.png">
                <link rel="apple-touch-icon" sizes="76x76" href="https://tefhealth.eu/files/tef-health/layout/favicon/apple-icon-76x76.png">
                <link rel="icon" type="image/png" sizes="16x16" href="https://tefhealth.eu/files/tef-health/layout/favicon/favicon-16x16.png"><link rel="apple-touch-icon" sizes="114x114" href="https://tefhealth.eu/files/tef-health/layout/favicon/apple-icon-114x114.png">
                <link rel="apple-touch-icon" sizes="120x120" href="https://tefhealth.eu/files/tef-health/layout/favicon/apple-icon-120x120.png">
                <link rel="apple-touch-icon" sizes="144x144" href="https://tefhealth.eu/files/tef-health/layout/favicon/apple-icon-144x144.png">
                <link rel="apple-touch-icon" sizes="152x152" href="https://tefhealth.eu/files/tef-health/layout/favicon/apple-icon-152x152.png">
                <link rel="apple-touch-icon" sizes="180x180" href="https://tefhealth.eu/files/tef-health/layout/favicon/apple-icon-180x180.png">
                <link rel="icon" type="image/png" sizes="192x192" href="https://tefhealth.eu/files/tef-health/layout/favicon/android-icon-192x192.png">
                <link rel="icon" type="image/png" sizes="32x32" href="https://tefhealth.eu/files/tef-health/layout/favicon/favicon-32x32.png">
                <link rel="icon" type="image/png" sizes="96x96" href="https://tefhealth.eu/files/tef-health/layout/favicon/favicon-96x96.png">
                <meta name="msapplication-TileColor" content="#ffffff">
                <meta name="msapplication-TileImage" content="https://tefhealth.eu/files/tef-health/layout/favicon/ms-icon-144x144.png">            
                <title>TEF-Health Service Catalogue</title>
                <meta name="og:title" content="TEF-Health Service Catalogue">
                <meta name="og:image" content="https://tefhealth.eu/files/tef-health/layout/favicon/apple-icon-180x180.png">
                """;
    }
}
