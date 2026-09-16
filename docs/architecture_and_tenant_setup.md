# Architecture and tenant customization setup

This document explains how the `marmotgraph-search` generic library relates to
tenant-specific implementations such as `~/ebrains/kg-search` (the EBRAINS
deployment of MarmotGraph Search). It complements
`setting_up_the_dev_environment.md` and `components_and_dependencies.md`.

## 1. Repository layout (this repo)

```
marmotgraph-search/
├── service/                 # Maven reactor (parent artifact: org.marmotgraph.search:search-parent)
│   ├── pom.xml               # parent pom, groupId org.marmotgraph.search, version 4.0.0-SNAPSHOT
│   ├── common/                artifactId "common"   – shared models, translation framework, customization SPI
│   ├── search/                artifactId "search"   – the search API + serves the built UI (Spring Boot jar)
│   └── indexing/              artifactId "indexing" – reads from KG Core API, translates, writes to Elasticsearch
└── ui/                       # React + Vite SPA (kg-search-ui), builds to ui/dist
```

`service/common`, `service/search` and `service/indexing` are three separate
Maven artifacts published together as a reactor build. `search` and
`indexing` are independently runnable Spring Boot applications (see
`setting_up_the_dev_environment.md`).

## 2. How the React UI ends up inside the `search` jar

The `search` module ships a **placeholder**
`service/search/src/main/resources/public/index.html` — literally just a
"this will be replaced during the build" stub. The real UI is injected only
by CI, not by the Maven build itself:

`.github/workflows/publish.yml` (triggered on push to branch
`search_as_a_library` or on `v*` tags):
1. `npm ci && npm run build` inside `ui/` → produces `ui/dist/*`
2. `cp -r ui/dist/* service/search/src/main/resources/public/` — overwrites
   the placeholder with the real compiled SPA
3. `mvn deploy` from `service/` → builds all 3 modules and publishes them as
   Maven artifacts to **GitHub Packages**
   (`https://maven.pkg.github.com/marmotgraph/marmotgraph-search`)

So: the jar you consume from GitHub Packages already contains the compiled,
tenant-agnostic UI baked into `BOOT-INF/classes/public/`. Locally, unless you
run this same copy step, `mvn spring-boot:run` on `search` will only serve
the placeholder page (this is expected — see §5 UI dev workflow).

`ui/Dockerfile` (nginx-based) is a second, independent way to run the raw UI
standalone (e.g. for pure frontend work/preview) — it is not part of the jar
publishing pipeline.

## 3. Runtime request routing (`search` module)

* `SPARouting` (service/search) registers a catch-all router: any request
  that is *not* `/api/**`, `/v3/api-docs*`, `/internal/**`, `/style/**`,
  `/sitemap/**`, `/error`, `/assets/**`, or a static-file extension
  (js/css/ico/png/jpg/gif/html/svg) falls through to `SPAController`, which
  returns the (possibly customized) `index.html`. This gives the SPA
  client-side routing.
* `SPAController` reads `classpath:public/index.html` and injects
  `Customization.getHeaderAdditions()` right before `</head>` (cached via
  `@Cacheable("index")`).
* `GET /api/settings` (`org.marmotgraph.search.api.Settings`) returns a JSON
  blob the UI fetches on load: Keycloak config, Sentry/Matomo config, and a
  `Customization.Configuration` + `CustomSections` record (terms of use,
  help text, navbar items, footer, etc.) plus the generated category/type
  mappings from `SettingsController`.
* `GET /api/settings/custom.css` returns `Customization.getCSSAdditions()`
  as raw CSS, loaded by the SPA at runtime.
* Static binary assets (logos, fonts, favicons) are plain Spring Boot static
  resources — a tenant just drops files under its own
  `search/src/main/resources/static/api/assets/...` and they're served at
  `/api/assets/...` (referenced from `Customization.getHeaderAdditions()` /
  `Configuration.logo()`).

## 4. The `Customization` extension point (branding/theming SPI)

`org.marmotgraph.search.common.customization.Customization` is a plain Spring
bean **interface with no default implementation** in this repo — every
tenant must provide exactly one `@Component` implementing it, or the app
context fails to start (`SPAController`, `Settings`, `Config` all
`@Autowired`-inject it directly, not as a list).

```java
public interface Customization {
    String getHeaderAdditions();   // raw HTML injected before </head>
    String getCSSAdditions();      // served at /api/settings/custom.css
    Configuration getConfiguration(); // name, logo, copyright, login flag, ...
    String getTermsOfUse();
    String getHelp();              // markdown shown in the "help" panel
    String getNavBarItems();
    String getFooterContent();
    String getFooterSocial();
    String getNotFoundFooter();
    record Configuration(String home, String name, String copyright, String copyrightSince,
                          String copyrightAddition, String searchExample, String logo,
                          String logoDark, boolean login, boolean inProgressOnly) { ... }
}
```

EBRAINS' implementation:
`~/ebrains/kg-search/common/src/main/java/org/marmotgraph/search/ebrains/EBRAINSCustomization.java`
— a `@Component` that hardcodes EBRAINS branding (title/meta tags, favicons,
Knowledge-Space "not found" footer, `/api/assets/...` font preloads) and
loads `classpath:custom.css` (`ebrains/kg-search/common/src/main/resources/custom.css`).

This is the primary mechanism for a tenant to reskin/rebrand the generic UI
**without forking the React code** — everything is server-injected HTML/CSS
plus a `logo`/`logoDark` asset path.

## 5. The `Translator` extension point (metadata mapping SPI)

This is how tenants map their domain-specific KG types (e.g. `Dataset`,
`Person`, `Software`) into the generic search index/UI model.

* `org.marmotgraph.search.common.controller.translation.models.Translator<Source extends SourceInstance, Target extends TargetInstance>`
  is an abstract base class. Concrete tenant translators extend it and
  implement `translate(Source, Target, DataStage, boolean liveMode, TranslatorUtils)`.
* Each concrete translator is annotated `@Translator.Instance` (itself
  meta-annotated `@Component`), with attributes `autoRelease`,
  `addToSitemap`, `orderNumber`.
* `TranslatorBase` provides static helpers (`value(...)`, `ref(...)`,
  `link(...)`, `children(...)`) for building the target document fields.
* **Discovery**: `TranslatorRegistry` (`service/common`) is itself a
  `@Component` that, at startup, calls
  `applicationContext.getBeansWithAnnotation(Translator.Instance.class)` and
  reflects on each bean's generic `Source`/`Target` type parameters (via
  `ResolvableType`) to build a `TranslatorModel` (category, semantic types
  from the source's `@Query` annotation, `MetaInfo` from the target type,
  etc.). **This means translators are picked up purely by Spring component
  scanning — there is no manual registration list.**
* `TranslationController` (search + indexing) uses the registry to run
  translation both for indexing (ES) and for "live mode" (direct KG Core
  query, bypassing ES — see `components_and_dependencies.md` for the
  index-based vs. live-mode distinction).

EBRAINS' translators live in
`~/ebrains/kg-search/common/src/main/java/org/marmotgraph/search/ebrains/translators/`
— one class per KG type (`Dataset.java`, `Model.java`, `Software.java`,
`Project.java`, `Contributor.java`, `BrainAtlas.java`, ~22 translators total),
plus `source/` (35 files, tenant-specific source-graph query models) and a
handful of `target/` extensions (`HasMetaBadges`, `DOIReference`, etc.) and
`commons/` helpers (`SchemaOrgConverter`, `MetaBadgeUtils`, `Accessibility`).

## 6. How a tenant app is wired together (crucial trick: package name)

Both `EBRAINSSearchApplication` and `EBRAINSIndexingApplication` are
`@SpringBootApplication` classes placed in package **`org.marmotgraph.search`**
— i.e. the same root package the *library's* own classes live under
(`org.marmotgraph.search.api`, `org.marmotgraph.search.controller`, ...),
**not** under `org.marmotgraph.search.ebrains` where the tenant's own code
(`EBRAINSCustomization`, translators, ...) resides.

Since Spring Boot's default component scan covers the application class's
package *and all subpackages*, and `org.marmotgraph.search.ebrains` is a
subpackage of `org.marmotgraph.search`, putting the `@SpringBootApplication`
class at the shorter, shared root package causes a single scan to pick up
**both** the generic library beans and the tenant's `Customization`
implementation + `Translator` beans. If a tenant instead put its
`@SpringBootApplication` class under its own namespace only, the library's
controllers/services would never be found.

## 7. Multi-module Maven structure of a tenant repo (EBRAINS example)

```
kg-search/                       (~/ebrains/kg-search)
├── pom.xml                       parent "ebrains-search-parent", <parent> = org.marmotgraph.search:search-parent:4.0.0-SNAPSHOT
├── common/    → ebrains-common    depends on org.marmotgraph.search:common
├── search/    → ebrains-search    depends on org.marmotgraph.search:search + ebrains-common; has spring-boot-maven-plugin (repackage) → runnable jar
├── indexing/  → ebrains-indexing  depends on org.marmotgraph.search:indexing + ebrains-common
├── search/Dockerfile, indexing/Dockerfile   (eclipse-temurin JRE, COPY target/*.jar, java -jar)
├── .gitlab-ci.yml                 mvn verify with local .m2 cache, then kaniko docker builds pushed to Harbor
└── .m2/settings.template.xml      injects GITHUB_USER/GITHUB_TOKEN to auth against the github-packages Maven repo
```

Key points:
- The tenant's parent pom inherits from `search-parent` (this repo's root
  pom) *by artifact coordinates only* (no `relativePath` override that
  works locally) — resolved from the GitHub Packages Maven repository
  published by this repo's CI. **Practical implication for local dev**: to
  build the EBRAINS repo against unreleased local changes in
  `marmotgraph-search`, you must `mvn install` this repo's `service/` reactor
  into your local `~/.m2` first (or push to `search_as_a_library`/a tag to
  get it published to GitHub Packages), otherwise Maven will pull whatever
  SNAPSHOT/version is available in the remote repo.
- GitLab CI (`~/ebrains/kg-search/.gitlab-ci.yml`) authenticates against
  GitHub Packages using `GITHUB_USER`/`GITHUB_TOKEN` CI variables templated
  into `.m2/settings.xml`, runs `mvn verify`, then builds two separate Docker
  images (search, indexing) via Kaniko and pushes to a Harbor registry.
- Runtime images are minimal: `COPY target/*.jar` + `java -jar`, no UI build
  step here — the UI is already inside the `search` artifact's jar (as
  described in §2), so `ebrains-search`'s jar = generic `search` jar's
  classes/resources + EBRAINS' `Customization`/translator beans layered on
  top by Spring Boot's repackage (fat jar merges dependency jars).

## 8. Practical implications / gotchas for future work

- **Editing UI look-and-feel for a single tenant**: prefer the
  `Customization` SPI (`getHeaderAdditions`, `getCSSAdditions`, `custom.css`,
  static assets) over touching `ui/` — that keeps the change tenant-local
  and out of the shared library. Only touch `ui/` for behavior/feature
  changes meant to apply to *all* tenants.
- **Adding a new indexable type for a tenant**: add a `SourceInstance`
  subtype (with `@Query`), a `TargetInstance` subtype (with `@MetaInfo`),
  and a `Translator` subclass annotated `@Translator.Instance` in the
  tenant's `common` module — no registration wiring needed beyond that
  (component scan + `TranslatorRegistry` handle discovery automatically).
- **Local UI dev against a real backend**: run the `search` Spring Boot app
  (serves the placeholder index.html, but all `/api/**` endpoints work) and
  run `ui/` via `npm run start` (Vite dev server) separately — see
  `ui/src/services/setupProxy.js`-style config mentioned in
  `setting_up_the_dev_environment.md` for pointing the dev UI at the running
  API.
- **Local UI+jar integration testing**: you must manually replicate the CI
  copy step (`npm run build` in `ui/`, then copy `ui/dist/*` into
  `service/search/src/main/resources/public/`) before `mvn package`/`mvn
  spring-boot:run`, otherwise you'll only see the placeholder page.
- **Version coupling**: both repos are currently pinned to
  `4.0.0-SNAPSHOT` / `search-parent` — check `service/pom.xml` `<version>`
  here vs. the `<parent><version>` in `~/ebrains/kg-search/pom.xml` before
  assuming compatibility across changes.
