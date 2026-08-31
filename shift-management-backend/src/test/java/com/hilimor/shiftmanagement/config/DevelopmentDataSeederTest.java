package com.hilimor.shiftmanagement.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DevelopmentDataSeederTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(DevelopmentDataSeeder.class);

    @Test
    void missingOptInDoesNotRegisterInitializer() {
        context.run(application -> {
            assertThat(application).hasNotFailed();
            assertThat(application).doesNotHaveBean("seedInitialData");
        });
    }

    @Test
    void packagedConfigurationDisablesDemoInitialization() {
        context.withInitializer(new ConfigDataApplicationContextInitializer()).run(application -> {
            assertThat(application).hasNotFailed();
            assertThat(application.getEnvironment().getProperty("app.seed.enabled")).isEqualTo("false");
            assertThat(application).doesNotHaveBean("seedInitialData");
        });
    }
}
