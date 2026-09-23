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

import org.springframework.stereotype.Component;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;

@Component
public class LuceneParserFactory {


    private static final String DEFAULT_FIELD_PLACEHOLDER = "_all"; // unused downstream; callers discard Lucene's own field matching

    public QueryParser newParser() {
        // EMPTY_SET: do NOT strip stopwords ("of", "and", "the", ...) at parse time.
        // Stopword handling belongs to the ES-side field analyzers (index/search time),
        // not to this syntax parser, which is only decoding AND/OR/NOT/quotes here.
        Analyzer analyzer = new StandardAnalyzer(CharArraySet.EMPTY_SET);
        QueryParser parser = new QueryParser(DEFAULT_FIELD_PLACEHOLDER, analyzer);
        parser.setDefaultOperator(QueryParser.Operator.AND); // bare "a b" => AND; switch to OR if you prefer recall over precision
        parser.setAllowLeadingWildcard(false);                // block leading '*'/'?': expensive and usually unintentional
        // setLowercaseExpandedTerms no longer exists in Lucene 10 - the analyzer
        // (StandardAnalyzer here) already lowercases via its built-in LowerCaseFilter.
        return parser;
    }

}
