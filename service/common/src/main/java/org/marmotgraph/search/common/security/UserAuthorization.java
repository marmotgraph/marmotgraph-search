package org.marmotgraph.search.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface UserAuthorization {

    Set<? extends GrantedAuthority> extractAuthorities(OAuth2User oAuth2User);

    boolean canReadLiveFiles(JwtAuthenticationToken token, UUID repositoryUUID);

    record GroupResponse(String value, String label){}

    List<GroupResponse> getGroups(JwtAuthenticationToken token);
}
