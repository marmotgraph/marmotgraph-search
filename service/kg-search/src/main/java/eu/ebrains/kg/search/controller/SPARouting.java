package eu.ebrains.kg.search.controller;

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
    RouterFunction<ServerResponse> spaRouter() {
        ClassPathResource index = new ClassPathResource("public/index.html");
        List<String> extensions = Arrays.asList("js", "css", "ico", "png", "jpg", "gif", "html", "svg");
        RequestPredicate spaPredicate = path("/api/**").or(path("/internal/**")).or(path("/sitemap/**")).or(path("/error")).or(pathExtension(extensions::contains)).negate();
        return route(spaPredicate, request -> ServerResponse.ok().contentType(MediaType.TEXT_HTML).body(index));
    }
}
