package io.github.daggerok.springbootintegrationfileexample.infrastructure;

import io.github.daggerok.springbootintegrationfileexample.SpringBootIntegrationFileExampleApplication;
import java.util.UUID;
import lombok.val;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@SpringBootTest(classes = SpringBootIntegrationFileExampleApplication.class)
public abstract class AbstractSpringBootTest {

    @DynamicPropertySource
    static void setupSpringBootProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        val prefix = UUID.randomUUID().toString();
        dynamicPropertyRegistry.add("app.input-dir", () -> String.format("target/%s/input-dir", prefix));
        dynamicPropertyRegistry.add("app.output-dir", () -> String.format("target/%s/output-dir", prefix));
    }
}
