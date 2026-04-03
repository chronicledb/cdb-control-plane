package io.github.grantchen2003.cdb.control.plane.chronicles;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChronicleServiceTest {
    private static final String userId = "3e30e447-ecd4-48b0-b592-207cd16b0609";
    private static final String chronicleName = "my-chronicle";

    @Mock
    private ChronicleRepository chronicleRepository;

    @InjectMocks
    private ChronicleService chronicleService;

    @Test
    void createChronicle_success() {
        final Chronicle chronicle = chronicleService.createChronicle(userId, chronicleName);

        assertThat(chronicle.userId()).isEqualTo(userId);
        assertThat(chronicle.name()).isEqualTo(chronicleName);
        assertThat(chronicle.createdAt()).isNotNull();
        verify(chronicleRepository).save(any(Chronicle.class));
    }

    @Test
    void createChronicle_propagatesDuplicateChronicleException() {
        doThrow(new DuplicateChronicleException(userId, chronicleName))
                .when(chronicleRepository).save(any(Chronicle.class));

        assertThatThrownBy(() -> chronicleService.createChronicle(userId, chronicleName))
                .isInstanceOf(DuplicateChronicleException.class);
    }

    @Test
    void existsByUserIdAndName_returnsTrue_whenRepositoryReturnsTrue() {
        when(chronicleRepository.existsByUserIdAndName(userId, chronicleName)).thenReturn(true);

        assertThat(chronicleService.existsByUserIdAndName(userId, chronicleName)).isTrue();
    }

    @Test
    void existsByUserIdAndName_returnsFalse_whenRepositoryReturnsFalse() {
        when(chronicleRepository.existsByUserIdAndName(userId, chronicleName)).thenReturn(false);

        assertThat(chronicleService.existsByUserIdAndName(userId, chronicleName)).isFalse();
    }
}