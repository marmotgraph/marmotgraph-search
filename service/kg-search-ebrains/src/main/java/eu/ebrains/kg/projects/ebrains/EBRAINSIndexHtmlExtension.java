package eu.ebrains.kg.projects.ebrains;

import eu.ebrains.kg.common.controller.translation.IndexHtmlExtension;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Profile("ebrains")
public class EBRAINSIndexHtmlExtension implements IndexHtmlExtension {

    @Override
    public String getHeaderAdditions() {
        return """
                <title>EBRAINS - Knowledge Graph Search</title>
                <meta name="og:title" content="EBRAINS">
                <meta name="og:image" content="/static/img/apple-touch-icon.png">
                <link rel="apple-touch-icon" href="/static/img/apple-touch-icon.png" sizes="180x180">
                <link rel="icon" type="image/png" href="/static/img/favicon-32x32.png" sizes="32x32">
                <link rel="icon" type="image/png" href="/static/img/favicon-16x16.png" sizes="16x16">
                <link rel="mask-icon" href="/static/img/safari-pinned-tab.svg" color="#111111">
                <meta name="theme-color" content="#e6e6e6">
                <meta name="description" content="EBRAINS offers advanced tools and resources for brain research, allowing scientists to study the brain at various scales. Join us in better understanding the brain's complexity.">
                <meta name="og:url" content="https://www.ebrains.eu/">
                <meta name="og:description" content="EBRAINS offers advanced tools and resources for brain research, allowing scientists to study the brain at various scales. Join us in better understanding the brain's complexity.">
                <meta name="og:type" content="article">
                <meta name="og:site_name" content="EBRAINS">
                <meta name="twitter:site" content="@EBRAINS_eu">
                <meta name="og:image" content="https://www.ebrains.eu/cover.png">
                <meta name="og:image:width" content="1200">
                <meta name="og:image:height" content="630">
                <meta name="twitter:card" content="summary_large_image">
                """;
    }
}
