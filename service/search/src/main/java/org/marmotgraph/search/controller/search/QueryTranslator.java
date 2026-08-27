package org.marmotgraph.search.controller.search;

import lombok.AllArgsConstructor;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.util.*;

@AllArgsConstructor
@Component
public class QueryTranslator {

    private final SearchFieldsController searchFieldsController;


    private static final String IDENTIFIER_CATCHALL_FIELD = "identifiers_keyword_catchall";
    // If you want a few identifiers individually boosted rather than only the
    // catch-all, add them here; they'll get their own term clause too.
//    private static final List<String> BOOSTED_IDENTIFIER_FIELDS = List.of(
//            "nid.value",
//            "xname"
//    );

    // ---- Scoring configuration --------------------------------------------

    private static final float EXACT_MATCH_BOOST = 5.0f;
    private static final float PREFIX_MATCH_BOOST = 1.0f;
    private static final float PHRASE_MATCH_BOOST = 6.0f;
    private static final float IDENTIFIER_EXACT_BOOST = 8.0f;
    private static final float IDENTIFIER_PREFIX_BOOST = 0.5f;

    // ---- Parser configuration ------------------------------------------------

    private static final String DEFAULT_FIELD_PLACEHOLDER = "_all"; // unused downstream; we discard Lucene's own field matching

    /**
     * Parses the raw user input and returns a complete Elasticsearch query body
     * (i.e. the value that would sit under the top-level "query" key).
     *
     * @throws QueryTranslationException if the input cannot be parsed as a boolean query
     */
    public Map<String, Object> translate(String userInput, List<Type> targetTypes) {
        final List<String> textFields = targetTypes.stream().map(searchFieldsController::getEsQueryFields).flatMap(Collection::stream).distinct().toList();

        if (userInput == null || userInput.isBlank()) {
            return matchAll();
        }

        Query luceneQuery;
        try {
            QueryParser parser = newParser();
            luceneQuery = parser.parse(userInput);
        } catch (ParseException e) {
            // Fallback strategy: treat the whole input as a literal phrase/term
            // rather than failing the search outright. Remove this fallback if
            // you'd rather surface a 400 to the caller.
            return exactPlusPartialClause(userInput.trim(), textFields);
        }

        return visit(luceneQuery, textFields);
    }

    private QueryParser newParser() {
        Analyzer analyzer = new StandardAnalyzer();
        QueryParser parser = new QueryParser(DEFAULT_FIELD_PLACEHOLDER, analyzer);
        parser.setDefaultOperator(QueryParser.Operator.AND); // bare "a b" => AND; switch to OR if you prefer recall over precision
        parser.setAllowLeadingWildcard(false);                // block leading '*'/'?': expensive and usually unintentional
        // setLowercaseExpandedTerms no longer exists in Lucene 10 - the analyzer
        // (StandardAnalyzer here) already lowercases via its built-in LowerCaseFilter.
        return parser;
    }

    // ---- Query tree visitor ----------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String, Object> visit(Query query, List<String> textFields) {
        if (query instanceof BooleanQuery bq) {
            return visitBoolean(bq, textFields);
        }
        if (query instanceof PhraseQuery pq) {
            return visitPhrase(pq, textFields);
        }
        if (query instanceof TermQuery tq) {
            return visitTerm(tq, textFields);
        }
        if (query instanceof PrefixQuery pq) {
            return visitPrefix(pq, textFields);
        }
        if (query instanceof MatchAllDocsQuery) {
            return matchAll();
        }

        // Anything else (WildcardQuery, FuzzyQuery, TermRangeQuery, RegexpQuery, ...)
        // is intentionally unsupported by default - decide explicitly whether to
        // support it rather than silently mistranslating it.
        throw new QueryTranslationException(
                "Unsupported query construct: " + query.getClass().getSimpleName()
                + ". Extend QueryTranslator.visit(...) if this should be allowed.");
    }

    private Map<String, Object> visitBoolean(BooleanQuery bq, List<String> textFields) {
        List<Map<String, Object>> must = new ArrayList<>();
        List<Map<String, Object>> should = new ArrayList<>();
        List<Map<String, Object>> mustNot = new ArrayList<>();
        List<Map<String, Object>> filter = new ArrayList<>();

        for (BooleanClause clause : bq.clauses()) {
            Map<String, Object> translated = visit(clause.query(), textFields);
            switch (clause.occur()) {
                case MUST -> must.add(translated);
                case SHOULD -> should.add(translated);
                case MUST_NOT -> mustNot.add(translated);
                case FILTER -> filter.add(translated);
            }
        }

        Map<String, Object> boolBody = new LinkedHashMap<>();
        if (!must.isEmpty()) boolBody.put("must", must);
        if (!should.isEmpty()) {
            boolBody.put("should", should);
            // If there are no MUST/FILTER clauses, at least one SHOULD must match
            // (mirrors default Lucene semantics for a bare OR chain).
            if (must.isEmpty() && filter.isEmpty()) {
                boolBody.put("minimum_should_match", 1);
            }
        }
        if (!mustNot.isEmpty()) boolBody.put("must_not", mustNot);
        if (!filter.isEmpty()) boolBody.put("filter", filter);

        return Map.of("bool", boolBody);
    }

    private Map<String, Object> visitPhrase(PhraseQuery pq, List<String> textFields) {
        String phraseText = String.join(" ",
                Arrays.stream(pq.getTerms()).map(t -> t.text()).toList());

        List<Map<String, Object>> should = new ArrayList<>();
        for (String field : textFields) {
            should.add(Map.of("match_phrase", Map.of(
                    field, Map.of("query", phraseText, "boost", PHRASE_MATCH_BOOST)
            )));
        }
        return Map.of("bool", Map.of("should", should, "minimum_should_match", 1));
    }

    private Map<String, Object> visitTerm(TermQuery tq, List<String> textFields) {
        return exactPlusPartialClause(tq.getTerm().text(), textFields);
    }

    private Map<String, Object> visitPrefix(PrefixQuery pq, List<String> textFields) {
        // User explicitly typed a trailing '*' (or Lucene inferred a prefix query) -
        // treat it purely as partial-match intent, skip the exact-match clause.
        return prefixOnlyClause(pq.getPrefix().text(), textFields);
    }

    // ---- Leaf clause builders -------------------------------------------------

    /** Exact match (high boost) OR prefix match (low boost) OR identifier exact match, across configured fields. */
    private Map<String, Object> exactPlusPartialClause(String term, List<String> textFields) {
        List<Map<String, Object>> should = new ArrayList<>();

        should.add(Map.of("multi_match", Map.ofEntries(
                Map.entry("query", term),
                Map.entry("type", "best_fields"),
                Map.entry("fields", textFields),
                Map.entry("boost", EXACT_MATCH_BOOST)
        )));

        should.add(Map.of("multi_match", Map.ofEntries(
                Map.entry("query", term),
                Map.entry("type", "bool_prefix"),
                Map.entry("fields", textFields),
                Map.entry("boost", PREFIX_MATCH_BOOST)
        )));

        should.add(Map.of("term", Map.of(
                IDENTIFIER_CATCHALL_FIELD, Map.of("value", term, "boost", IDENTIFIER_EXACT_BOOST)
        )));

//        for (String field : BOOSTED_IDENTIFIER_FIELDS) {
//            should.add(Map.of("term", Map.of(
//                    field, Map.of("value", term, "boost", IDENTIFIER_EXACT_BOOST)
//            )));
//        }

        return Map.of("bool", Map.of("should", should, "minimum_should_match", 1));
    }

    /** User explicitly asked for a prefix/partial match (trailing '*'). */
    private Map<String, Object> prefixOnlyClause(String prefix, List<String> textFields) {
        List<Map<String, Object>> should = new ArrayList<>();

        should.add(Map.of("multi_match", Map.ofEntries(
                Map.entry("query", prefix),
                Map.entry("type", "bool_prefix"),
                Map.entry("fields", textFields),
                Map.entry("boost", PREFIX_MATCH_BOOST)
        )));

        // Identifier fields: only if you deliberately want substring/prefix search
        // on IDs. Requires IDENTIFIER_CATCHALL_FIELD to be mapped as ES "wildcard"
        // type, and should be run as a filter (no scoring contribution) since
        // wildcard queries don't score meaningfully.
        should.add(Map.of("constant_score", Map.of(
                "filter", Map.of("wildcard", Map.of(
                        IDENTIFIER_CATCHALL_FIELD, Map.of("value", prefix + "*")
                )),
                "boost", IDENTIFIER_PREFIX_BOOST
        )));

        return Map.of("bool", Map.of("should", should, "minimum_should_match", 1));
    }

    private Map<String, Object> matchAll() {
        return Map.of("match_all", Map.of());
    }

    /** Thrown when the parsed query contains a construct this translator doesn't (yet) support. */
    public static class QueryTranslationException extends RuntimeException {
        public QueryTranslationException(String message) {
            super(message);
        }
    }
}
