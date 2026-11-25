# Changelog

## 4.0.0-marmotgraph-v4 (unreleased)

The service no longer talks to the KG core v3 API through self-written `WebClient` connectors. It uses the
[MarmotGraph Java SDK](https://github.com/marmotgraph/marmotgraph-core-v4/tree/main/sdks/java) against the
**MarmotGraph v4 API** instead. Instances are fetched with marker based pagination, and all authentication settings
are loaded from the core at runtime.

This version requires a MarmotGraph **v4** core. It doesn't work against a KG core v3.

### Migration guide

#### 1. Dependencies and build

- Change the parent version of consuming projects (e.g. `kg-search`) to `4.0.0-marmotgraph-v4-SNAPSHOT`.
- `common` now depends on `org.marmotgraph:marmotgraph-sdk:0.1.0-SNAPSHOT`, which is published to the GitHub Packages
  repository of `marmotgraph/marmotgraph-core-v4` (`https://maven.pkg.github.com/marmotgraph/marmotgraph-core-v4`).
  The build environment (local `settings.xml`, CI) needs this repository and credentials for it.
- The SDK version must contain the lazy OIDC discovery for client credentials. With an older SDK snapshot the service
  contacts the core while starting up again and fails to start if the core is unavailable.
- `maven-surefire-plugin` is pinned to 3.5.4 in the parent pom. The previous default (2.17) didn't detect JUnit 5
  tests, so `mvn test` silently ran nothing.

#### 2. Configuration (environment variables)

| Before | Now | Notes |
|---|---|---|
| `KG_CORE_ENDPOINT` | `MARMOTGRAPH_HOST` | The **bare host including the scheme**, e.g. `https://core.marmotgraph.org`. The SDK adds `/v4/...` itself, so a value ending with `/v3` breaks every call. |
| *(hardcoded `kg-search`)* | `MARMOTGRAPH_CLIENT_ID` | The client id of the service account. It is also the space of the uploaded queries and of the badge instances. Set it to `kg-search` to keep the previous space. |
| `CLIENT_SECRET` | `MARMOTGRAPH_CLIENT_SECRET` | The client secret of the service account. |
| *(new)* | `MARMOTGRAPH_TENANT` | Optional (default: not set). The tenant this search instance is serving. It prefixes the scope registered in the core, e.g. `foo` gives the scope `foo-search` (without a tenant it's `search`), so several search instances can run against the same core. Further tenant resources (fonts, color schemes, logos) are planned for later. |
| `KEYCLOAK_ISSUER_URI` | *(removed)* | The issuer, the signing keys (JWKS), the token / authorization / userinfo endpoints are loaded from the core (`GET /v4/setup/authentication` and the advertised OpenID Connect discovery document). |
| `KG_EBRAINS_ENDPOINT` (`kgebrains.endpoint`) | *(removed)* | The absolute URLs of the sitemap are derived from the request to `/sitemap`, see below. |

**Precondition for the sitemap:** the URLs in `/sitemap` are built from the scheme, host, port and context path of the
incoming request. Behind a reverse proxy / ingress:

- The proxy has to send `X-Forwarded-Proto`, `X-Forwarded-Host` (and `X-Forwarded-Port` / `X-Forwarded-Prefix` if
  applicable).
- The consuming application has to evaluate them, e.g. with `server.forward-headers-strategy=framework` (or `native`
  for the headers supported by the embedded server). Only enable it if the service is reachable through the proxy
  only, since it makes the service trust these headers from any client.

Otherwise, the sitemap contains the internal address of the service (e.g. `http://my-service:8080/instances/...`).

The SDK also understands its other `MARMOTGRAPH_*` variables (e.g. `MARMOTGRAPH_TIMEOUT`,
`MARMOTGRAPH_DEFAULT_STAGE`), see the SDK's `docs/configuration.md`.

All `spring.security.oauth2.*` properties have been removed from `common.properties`:

- `spring.security.oauth2.client.provider.keycloak.issuer-uri`
- `spring.security.oauth2.resourceserver.jwt.issuer-uri`
- `spring.security.oauth2.client.registration.kg.*`

If a consuming project or a deployment still sets them, remove them. In particular, a leftover
`spring.security.oauth2.client.registration.*` with a provider `issuer-uri` makes Spring Boot contact the identity
provider while starting up, so the service would again fail to start without it. A leftover
`spring.security.oauth2.resourceserver.jwt.issuer-uri` is ignored, because the service provides its own `JwtDecoder`.

The properties `keycloak.realm` and `keycloak.resource` have been removed as well. The Keycloak settings of the UI and
the OAuth client of the Swagger UI are now loaded from the core (see "Behavioral changes"). An explicitly set
`springdoc.swagger-ui.oauth.client-id` still takes precedence for the Swagger UI.

#### 3. Startup and availability of the core

- The service starts even if the core (or its identity provider) isn't available. The authentication settings and the
  token of the service account are loaded on first use, cached afterwards, and retried on the next use if loading fails.
- While the core is unreachable:
  - Requests with a bearer token fail with a server error (not with a `401`), because the token itself isn't at fault.
  - Calls on behalf of the service account fail with a `MarmotGraphException`.
- The OAuth URLs of the Swagger UI come from the core when the API docs are first requested. If the core is down at
  that moment, springdoc may cache the docs without those URLs until the service is restarted.

#### 4. Behavioral changes

- **Bookmarks are backed by the favorites of the user in MarmotGraph v4.** The previous `Bookmark` instances in
  `myspace` are no longer read or written, so existing bookmarks have to be migrated to favorites if they should be
  kept. The REST endpoints (`GET/PUT/DELETE .../{id}/bookmark`) are unchanged.
- **Invitations** ("is invited for the file repository") are now the instance-level permissions of the user
  (`GET /v4/users/me/roles`).
- **The `keycloak` section of `/api/settings`** is derived from the authentication settings of the core:
  - `url` and `realm` come from the `issuer` of the OpenID Connect discovery document, which for Keycloak is
    `{url}/realms/{realm}` (e.g. `https://iam.example.org/auth/realms/hbp` gives the url `https://iam.example.org/auth`
    and the realm `hbp`). If the issuer doesn't contain `/realms/`, the `realm` is `null` and the `url` is the issuer.
  - `clientId` is the `loginClientId` of the core (`GET /v4/setup/authentication`).
  - If the core isn't reachable, `keycloak` is `null` (the UI then can't log in), and it's retried on the next request.
- **The OAuth client id of the Swagger UI** is the `loginClientId` of the core, too (previously `keycloak.resource`,
  defaulting to `kg`).
- **Queries** are generated as v4 query specifications. The response keys are the plain field names of the source
  classes (previously `query:<field>` with a response vocabulary). Queries are uploaded with the same (name-based)
  ids as before into the space `MARMOTGRAPH_CLIENT_ID`. A query in a wrong space is moved via the dedicated v4
  endpoint (no more unrelease / release around the move).
- **Scope definitions** are uploaded together with the queries. For every indexed type, the queries are reduced to the
  paths leading to further instances (plain values are dropped) and registered as the scope `search` (or
  `<MARMOTGRAPH_TENANT>-search`) of the type via `PUT /v4/types/scopes/{name}`. If several queries serve the same type,
  their scopes are combined. The service account needs the `DEFINE_SCOPES` permission. Like the queries, scopes aren't
  uploaded with `localQueries=true`.
- **Indexing** uses marker based pagination (`nextMarker`) instead of `from`/`size`:
  - Indexing of a type ends when the server doesn't provide a next marker, or when a page is empty.
  - If a page can't be read after all retries, indexing of that type stops. Before, it skipped to the next offset,
    but there is no marker to continue from.
  - Totals aren't requested anymore, so the progress log shows the number of processed instances without a
    percentage.

- **The root element of `/sitemap`** is `<urlset>` again, as the sitemap protocol requires. With Jackson 3 it had
  become `<SitemapXML>`.

#### 5. Java API changes (for code built on top of this library)

**Removed**

| Removed | Replacement |
|---|---|
| `WebClient` beans `@Qualifier("asServiceAccount")` and `@Qualifier("asUser")` | The `MarmotGraphClient` bean (service account). For the current user, use `KGServiceClient`, which derives a client from the incoming `Authorization` header. The `@Primary` `WebClient` (used for Elasticsearch) still exists. |
| Spring `ClientRegistrationRepository` registration `kg` | `CoreAuthentication#clientRegistration()` |
| `common.utils.queryGenerator.MarmotGraphQuery` | `org.marmotgraph.sdk.model.QuerySpecification` |
| `KGServiceClient#addBookmark`, `#deleteBookmark`, `#getBookmarkIdsFromInstance`, `#getBookmarkedInstancesOfType` | `#addFavorite`, `#removeFavorite`, `#isFavorite`, `#getFavoriteInstancesOfType` (likewise on `KG`) |
| `SearchController#getBookmarkIdsFromInstance` | `SearchController#isBookmarked` |
| `ResultsOfKG#getFrom` / `#setFrom`, `TargetInstancesResult#getFrom` / `#setFrom` | `#getNextMarker` / `#setNextMarker` (null if there are no more results) |
| `SitemapController#getSitemap()` returning `SitemapXML` | `SitemapController#getSitemapInstanceIds()` returning the ids (the cache `sitemap` now holds a `java.util.List`). `#updateSitemapCache()` returns the ids as well. |

**Changed signatures**

| Before | Now |
|---|---|
| `KG#executeQuery(clazz, stage, queryId, semanticType, int from, int size)` | `KG#executeQuery(clazz, stage, queryId, semanticType, String marker, int size)`: `null` marker for the first page |
| `KGServiceClient#executeQueryForIndexing(..., int from, int size)` | `KGServiceClient#executeQueryForIndexing(..., String marker, int size)` |
| `TranslationController#translateToTargetInstances(kg, model, queryId, stage, int from, int size, trendingThreshold, context)` | `TranslationController#translateToTargetInstances(kg, model, queryId, stage, String marker, int size, int alreadyProcessed, trendingThreshold, context)`: `alreadyProcessed` is used for statistics only |
| `TranslationUtils#getStats(result, int from)` | `TranslationUtils#getStats(result, int alreadyProcessed)` |
| `KGServiceClient#getInstance(...)` returns a `Map` with the instance in `data` | Returns `org.marmotgraph.sdk.model.Instance` (the instance itself, `null` if not found) |
| `KGServiceClient#uploadQuery(String, MarmotGraphQuery)` | `KGServiceClient#uploadQuery(String, QuerySpecification)` |
| `QueryGenerator#generate(...)` returns `MarmotGraphQuery` | Returns `QuerySpecification` |
| `KGServiceClient` constructor | `KGServiceClient(MarmotGraphClient, clientId, developmentMode, AppConfig, QueryGenerator)` |

A paging loop over the KG therefore changes from

```java
int from = 0;
do {
    TargetInstancesResult result = translationController.translateToTargetInstances(kg, model, queryId, stage, from, size, trendThreshold, context);
    // ...
    from = result.getFrom() + result.getSize();
} while (/* from < total */);
```

to

```java
String marker = null;
int processed = 0;
do {
    TargetInstancesResult result = translationController.translateToTargetInstances(kg, model, queryId, stage, marker, size, processed, trendThreshold, context);
    // ...
    processed += result.getSize();
    marker = result.getNextMarker();
} while (marker != null);
```

**New beans** (in `common.configuration`)

- `MarmotGraphClient`: the client of the service account, created by the SDK's default builder from the environment.
- `CoreAuthentication`: the authentication settings of the core (loaded lazily) and the client registration for the
  userinfo lookup.
- `JwtDecoder`: validates the tokens of the users against the signing keys and the issuer of the core. A consuming
  project which defines its own `JwtDecoder` gets a conflicting bean definition. Remove yours, or tell us what it's
  needed for.
- `OpenApiCustomizer` (`oAuthFlowCustomizer`): fills in the OAuth URLs of the API docs.
- `SwaggerIndexTransformer` (`indexPageTransformer`, a `SwaggerIndexPageWithLoginClient`): replaces springdoc's
  transformer of the Swagger UI index page to fill in the OAuth client id.

**Exceptions**

Errors of the core are now raised as `org.marmotgraph.sdk.MarmotGraphApiException` (HTTP errors, with
`statusCode()`) or `org.marmotgraph.sdk.MarmotGraphException` (e.g. the core isn't reachable). They replace
`WebClientResponseException`. `RestControllerAdvice` maps a `MarmotGraphApiException` to a response with the same status. Code catching
`WebClientResponseException` around KG calls has to catch the SDK exceptions instead. Elasticsearch calls still
raise `WebClientResponseException`.
