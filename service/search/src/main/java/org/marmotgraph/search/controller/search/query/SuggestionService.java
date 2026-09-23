/*
 * Copyright 2018 - 2021 Swiss Federal Institute of Technology Lausanne (EPFL)
 * Copyright 2021 - 2024 EBRAINS AISBL
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
 *
 *  This open source software code was developed in part or in whole in the
 *  Human Brain Project, funded from the European Union's Horizon 2020
 *  Framework Programme for Research and Innovation under
 *  Specific Grant Agreements No. 720270, No. 785907, and No. 945539
 *  (Human Brain Project SGA1, SGA2 and SGA3).
 */

package org.marmotgraph.search.controller.search.query;

import io.micrometer.common.util.StringUtils;
import lombok.AllArgsConstructor;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.marmotgraph.search.common.configuration.Constants;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Builds Elasticsearch/OpenSearch "term suggester" requests for single-word
 * "did you mean" typo detection, and parses the resulting suggest response
 * back into a simple original-term -> candidate-list map.
 * <p>
 * suggest_mode "missing" is used deliberately: a field-level suggester only
 * returns candidates for a word that doesn't exist AT ALL in that field's term
 * dictionary, so it only fires for genuine-looking typos rather than every
 * rare-but-real word.
 * <p>
 * Because each field's dictionary is different, a word missing from just one
 * field isn't necessarily a typo (it might be a real word that simply never
 * appears in that particular field). extractTypoSuggestions() only flags a
 * term as a likely typo when it was reported missing across EVERY configured
 * suggestion field.
 */
@Service
@AllArgsConstructor
public class SuggestionService {


    private static final String SUGGEST_MODE = "missing"; // only suggest for words absent from the field entirely
    private static final int SUGGEST_MAX_EDITS = 2;        // Levenshtein distance ceiling (1 or 2; ES caps at 2)
    private static final int SUGGEST_SIZE = 5;              // candidates returned per word per field

    private final LuceneParserFactory parserFactory;

    /**
     * Builds the "suggest" section of a search request: one term suggester per
     * configured field, all evaluated against the same extracted term list.
     * Returns an empty map if there's nothing worth suggesting against.
     */
    public Optional<Map<String, Object>> buildSuggestions(String userInput) {
        if (!StringUtils.isBlank(userInput)) {
            List<String> terms = extractTerms(userInput);
            if (!terms.isEmpty()) {
                String suggestionText = String.join(" ", terms);
                Map<String, Object> suggest = Map.of(
                        "text", suggestionText,
                        "suggestions", Map.of(
                                "term", Map.of(
                                        "field", Constants.SUGGEST_TEXT_PROPERTY,
                                        "suggest_mode", SUGGEST_MODE,
                                        "max_edits", SUGGEST_MAX_EDITS,
                                        "size", SUGGEST_SIZE
                                )
                        ));
                return Optional.of(suggest);
            }
        }
        return Optional.empty();
    }


    /**
     * Extracts the actual search terms (skipping boolean operators, which never
     * survive parsing as terms anyway, and skipping explicit prefix/wildcard terms,
     * which represent deliberate partial-match intent rather than a possible typo)
     * by walking the same Lucene parse tree used for the main query.
     */
    private List<String> extractTerms(String userInput) {
        List<String> terms = new ArrayList<>();
        try {
            Query query = parserFactory.newParser().parse(userInput);
            collectTerms(query, terms);
        } catch (ParseException e) {
            // Same fallback spirit as QueryTranslator.translate(): fall back to
            // naive whitespace splitting rather than returning no suggestions at all.
            terms.addAll(Arrays.asList(userInput.trim().split("\\s+")));
        }
        return terms;
    }

    private void collectTerms(Query query, List<String> out) {
        if (query instanceof BooleanQuery bq) {
            for (BooleanClause clause : bq.clauses()) {
                collectTerms(clause.query(), out);
            }
        } else if (query instanceof PhraseQuery pq) {
            for (var term : pq.getTerms()) {
                out.add(term.text());
            }
        } else if (query instanceof TermQuery tq) {
            out.add(tq.getTerm().text());
        }
        // PrefixQuery / WildcardQuery / etc. intentionally skipped here.
    }

}
