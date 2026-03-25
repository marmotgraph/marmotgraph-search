package org.marmotgraph.search.common.utils.queryGenerator;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Query {
    String defaultPropertyNamespace();
    String defaultTypeNamespace();
    int bulkSize() default 1000;
    boolean addToSitemap() default true;
    boolean autoRelease() default false;
    String[] semanticTypes() default {};

    @Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Field {
        /**
         * @return the namespace for the field - falling back to default namespace of the query.
         */
        String namespace() default "";

        /**
         * @return the path - if not set, a single traversal to the property corresponding to the model property name and default namespace is assumed.
         */
        PathElement[] path() default {};

        boolean required() default false;
    }


    @Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface PathElement{
        boolean reverse() default false;
        String[] typeFilter() default {};
        /**
         * @return the namespace of the property. If not set, the namespace of the wrapping query is reused
         */
        String namespace() default "";
        /**
         * @return the name of the property. If not set, the property name of the class is reused
         */
        String name() default "";
    }

    @interface Ignore{}
}
