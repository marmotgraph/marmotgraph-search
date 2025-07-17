package eu.ebrains.kg.search.controller;

import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RequestPredicate;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Arrays;
import java.util.List;

import static org.springframework.web.servlet.function.RequestPredicates.path;
import static org.springframework.web.servlet.function.RequestPredicates.pathExtension;
import static org.springframework.web.servlet.function.RouterFunctions.route;

@Configuration
public class SPARouting {

    @Bean
    RouterFunction<ServerResponse> spaRouter(SPAController controller) {
        List<String> extensions = Arrays.asList("js", "css", "ico", "png", "jpg", "gif", "html", "svg");
        RequestPredicate spaPredicate = path("/api/**").or(path("/internal/**")).or(path("/sitemap/**")).or(path("/error")).or(pathExtension(extensions::contains)).negate();
        return route(spaPredicate, request -> ServerResponse.ok().contentType(MediaType.TEXT_HTML).body(controller.getIndexHTML()));
    }

    /**
     * Tomcat prevents the use of square brackets in query param - we currently rely on this on the UI though.
     * @return
     */
    //TODO revisit the way we represent filters in the UI
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return (tomcat) -> {
            tomcat.addConnectorCustomizers((connector) ->
                    ((AbstractHttp11Protocol<?>)connector.getProtocolHandler()).setRelaxedQueryChars("[]"));
        };
    }
}
