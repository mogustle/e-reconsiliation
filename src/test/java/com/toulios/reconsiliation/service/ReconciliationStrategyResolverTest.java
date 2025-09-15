package com.toulios.reconsiliation.service;

import com.toulios.reconsiliation.config.ApiVersioningProperties;
import com.toulios.reconsiliation.exception.UnsupportedApiVersionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ReconciliationStrategyResolver}.
 */
@ExtendWith(MockitoExtension.class)
class ReconciliationStrategyResolverTest {

    @Mock
    private ReconciliationStrategy strategyV1;

    @Mock
    private ReconciliationStrategy strategyV2;

    private ApiVersioningProperties versioningProperties;
    private ReconciliationStrategyResolver resolver;

    @BeforeEach
    void setUp() {
        // Set up versioning properties
        versioningProperties = new ApiVersioningProperties();
        versioningProperties.setDefaultVersion("1");
        versioningProperties.setSupported(List.of("1", "2"));

        // Set up mock strategies
        when(strategyV1.version()).thenReturn("1");
        when(strategyV2.version()).thenReturn("2");

        // Create resolver with mock strategies
        resolver = new ReconciliationStrategyResolver(
            List.of(strategyV1, strategyV2), 
            versioningProperties
        );
    }

    @Test
    void resolve_shouldReturnV1Strategy_whenVersion1Requested() {
        // When
        ReconciliationStrategy result = resolver.resolve("1", "1");

        // Then
        assertThat(result).isEqualTo(strategyV1);
    }

    @Test
    void resolve_shouldReturnV2Strategy_whenVersion2Requested() {
        // When
        ReconciliationStrategy result = resolver.resolve("2", "1");

        // Then
        assertThat(result).isEqualTo(strategyV2);
    }

    @Test
    void resolve_shouldReturnDefaultStrategy_whenNullVersionProvided() {
        // When
        ReconciliationStrategy result = resolver.resolve(null, "1");

        // Then
        assertThat(result).isEqualTo(strategyV1);
    }

    @Test
    void resolve_shouldReturnDefaultStrategy_whenBlankVersionProvided() {
        // When
        ReconciliationStrategy result = resolver.resolve("  ", "1");

        // Then
        assertThat(result).isEqualTo(strategyV1);
    }

    @Test
    void resolve_shouldThrowException_whenUnsupportedVersionRequested() {
        // When & Then
        assertThatThrownBy(() -> resolver.resolve("3", "1"))
            .isInstanceOf(UnsupportedApiVersionException.class)
            .hasMessageContaining("API version '3' is not supported")
            .hasMessageContaining("Supported versions: 1, 2");
    }

    @Test
    void resolve_shouldReturnDefaultStrategy_whenSupportedVersionHasNoStrategy() {
        // Given - resolver with supported version but no strategy implementation
        versioningProperties.setSupported(List.of("1", "2", "99")); // 99 is supported in config but no strategy exists
        resolver = new ReconciliationStrategyResolver(List.of(strategyV1, strategyV2), versioningProperties);

        // When - request version 99 (supported but no strategy), should fall back to default
        ReconciliationStrategy result = resolver.resolve("99", "1");

        // Then - should return default strategy (version 1)
        assertThat(result).isEqualTo(strategyV1);
    }

    @Test
    void resolve_shouldAllowAnyVersion_whenSupportedListIsEmpty() {
        // Given
        versioningProperties.setSupported(List.of()); // Empty list means any version is allowed
        resolver = new ReconciliationStrategyResolver(List.of(strategyV1, strategyV2), versioningProperties);

        // When
        ReconciliationStrategy result = resolver.resolve("1", "1");

        // Then
        assertThat(result).isEqualTo(strategyV1);
    }

    @Test
    void resolve_shouldThrowException_whenRequestedVersionHasNoStrategyAndDefaultAlsoMissing() {
        // Given - create resolver with only V2 strategy
        when(strategyV2.version()).thenReturn("2");
        resolver = new ReconciliationStrategyResolver(List.of(strategyV2), versioningProperties);

        // When & Then - request V1 (supported but no strategy), default is also V1 (no strategy)
        assertThatThrownBy(() -> resolver.resolve("1", "1"))
            .isInstanceOf(UnsupportedApiVersionException.class)
            .hasMessageContaining("API version '1' is not supported");
    }
}
