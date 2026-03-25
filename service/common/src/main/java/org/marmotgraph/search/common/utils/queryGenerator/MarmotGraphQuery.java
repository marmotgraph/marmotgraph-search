package org.marmotgraph.search.common.utils.queryGenerator;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;


@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
public class MarmotGraphQuery {

    public MarmotGraphQuery(String contextVocab, String contextQuery, String targetType) {
        this.context = Map.of(
                "@vocab", contextVocab,
                "query", contextQuery,
                "propertyName", Map.of("@id", "propertyName", "@type", "@id")
        );
        this.meta = Map.of(
                "responseVocab", contextQuery,
                "type", targetType
        );
    }

    @JsonProperty("@context")
    private Map<String, Object> context;

    @JsonProperty("meta")
    private Map<String, Object> meta;

    @Getter
    @Setter
    private List<Property> structure;

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Property {
        private String propertyName;
        private List<Path> path;
        private String singleValue;
        private List<Property> structure;
        private Boolean required;
    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Path {
        @JsonProperty("@id")
        private String id;
        private Boolean reverse;
        private List<Ref> typeFilter;
    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Ref {
        @JsonProperty("@id")
        private String id;
    }

}
