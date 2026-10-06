package org.marmotgraph.search.common.configuration;

import org.marmotgraph.search.common.model.CommitInfo;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Configuration
public class AppInfo {

    @Bean
    CommitInfo commitInfo(ObjectProvider<GitProperties> appGit) {
        GitProperties git = appGit.getIfAvailable();
        String libraryCommit = load().getProperty("search-lib.commit.id.abbrev", "unknown");
        return new CommitInfo((git != null) ? git.getShortCommitId() : null, libraryCommit);
    }

    private static Properties load() {
        Properties p = new Properties();
        try (InputStream in = AppInfo.class.getResourceAsStream("/META-INF/search-lib.properties")) {
            if (in != null) p.load(in);
        } catch (IOException ignored) {
        }
        return p;
    }
}
