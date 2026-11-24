/*
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
 */

package org.marmotgraph.search.api;

import org.junit.jupiter.api.Test;
import org.marmotgraph.search.controller.sitemap.SitemapController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.ForwardedHeaderFilter;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SitemapTest {

    private MockMvc mockMvc(List<String> ids) {
        SitemapController sitemapController = mock(SitemapController.class);
        when(sitemapController.getSitemapInstanceIds()).thenReturn(ids);
        // ForwardedHeaderFilter is what "server.forward-headers-strategy=framework" registers
        return MockMvcBuilders.standaloneSetup(new Sitemap(sitemapController)).addFilters(new ForwardedHeaderFilter()).build();
    }

    @Test
    void urlsAreAbsoluteAndBasedOnTheRequestedHost() throws Exception {
        mockMvc(List.of("abc", "def")).perform(get("https://search.example.org:443/sitemap"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\"><url><loc>https://search.example.org/instances/abc?noSilentSSO=true</loc></url>")))
                .andExpect(content().string(containsString("<loc>https://search.example.org/instances/def?noSilentSSO=true</loc>")));
    }

    @Test
    void urlsRespectForwardedHeadersOfTheProxy() throws Exception {
        mockMvc(List.of("abc")).perform(get("http://internal:8080/sitemap")
                        .header("X-Forwarded-Proto", "https")
                        .header("X-Forwarded-Host", "search.example.org")
                        .header("X-Forwarded-Port", "443")
                        .header("X-Forwarded-Prefix", "/kg"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<loc>https://search.example.org/kg/instances/abc?noSilentSSO=true</loc>")))
                .andExpect(content().string(not(containsString("internal"))));
    }

    @Test
    void noInstancesResultsInServiceUnavailable() throws Exception {
        mockMvc(null).perform(get("/sitemap")).andExpect(status().isServiceUnavailable());
    }
}
