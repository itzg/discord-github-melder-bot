package me.itzg.melderbot.bot;

import java.util.Collections;
import me.itzg.melderbot.config.AppProperties;
import me.itzg.melderbot.integrations.RoleHandlingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataMongoTest
@Import({MeldLinkService.class})
@Testcontainers
@ActiveProfiles("test")
class MeldLinkServiceTest {

    @TestConfiguration
    static class Config {

        @Bean
        public AppProperties appProperties() {
            return new AppProperties()
                .setDiscordApplicationId(0L)
                .setGithubRepos(Collections.singletonList("org/name"))
                .setBaseUrl("http://localhost:8080")
                .setDiscordBotToken("TEST");
        }
    }

    @Container
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:4");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    MeldLinkService meldLinkService;

    @MockBean
    RoleHandlingService roleHandlingService;

    @Test
    void testGenerateMeldLink() {

    }
}