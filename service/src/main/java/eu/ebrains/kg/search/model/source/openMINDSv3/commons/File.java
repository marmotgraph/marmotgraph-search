package eu.ebrains.kg.search.model.source.openMINDSv3.commons;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class File {
    private List<String> roles;
    private String iri;
    private List<String> formats;
    private String name;
}