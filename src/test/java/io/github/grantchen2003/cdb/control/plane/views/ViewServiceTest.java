package io.github.grantchen2003.cdb.control.plane.views;

import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleNotFoundException;
import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewServiceTest {

    private static final String VIEW_ID        = "view-123";
    private static final String USER_ID        = "user-123";
    private static final String CHRONICLE_NAME = "my-chronicle";
    private static final String VIEW_NAME      = "my-view";
    private static final View   VIEW           = new View(VIEW_ID, USER_ID, CHRONICLE_NAME, VIEW_NAME, Instant.parse("2024-01-01T00:00:00Z"));

    @Mock
    private ViewRepository viewRepository;

    @Mock
    private ChronicleService chronicleService;

    @InjectMocks
    private ViewService viewService;

    @Test
    void createView_savesViewAndReturnsIt() {
        when(chronicleService.existsByUserIdAndName(USER_ID, CHRONICLE_NAME)).thenReturn(true);

        final ArgumentCaptor<View> captor = ArgumentCaptor.forClass(View.class);

        final View result = viewService.createView(USER_ID, CHRONICLE_NAME, VIEW_NAME);

        verify(viewRepository).save(captor.capture());
        final View saved = captor.getValue();

        assertThat(saved.viewId()).isNotNull();
        assertThat(saved.userId()).isEqualTo(USER_ID);
        assertThat(saved.chronicleName()).isEqualTo(CHRONICLE_NAME);
        assertThat(saved.viewName()).isEqualTo(VIEW_NAME);
        assertThat(saved.createdAt()).isNotNull();
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void exists_viewExists_returnsTrue() {
        when(viewRepository.exists(USER_ID, CHRONICLE_NAME, VIEW_NAME)).thenReturn(true);

        assertThat(viewService.exists(USER_ID, CHRONICLE_NAME, VIEW_NAME)).isTrue();
    }

    @Test
    void exists_viewDoesNotExist_returnsFalse() {
        when(viewRepository.exists(USER_ID, CHRONICLE_NAME, VIEW_NAME)).thenReturn(false);

        assertThat(viewService.exists(USER_ID, CHRONICLE_NAME, VIEW_NAME)).isFalse();
    }

    @Test
    void findByViewId_viewFound_returnsView() {
        when(viewRepository.findByViewId(VIEW_ID)).thenReturn(Optional.of(VIEW));

        assertThat(viewService.findByViewId(VIEW_ID)).contains(VIEW);
    }

    @Test
    void findByViewId_viewNotFound_returnsEmpty() {
        when(viewRepository.findByViewId(VIEW_ID)).thenReturn(Optional.empty());

        assertThat(viewService.findByViewId(VIEW_ID)).isEmpty();
    }

    @Test
    void createView_chronicleNotFound_throwsChronicleNotFoundException() {
        when(chronicleService.existsByUserIdAndName(USER_ID, CHRONICLE_NAME)).thenReturn(false);

        assertThatThrownBy(() -> viewService.createView(USER_ID, CHRONICLE_NAME, VIEW_NAME))
                .isInstanceOf(ChronicleNotFoundException.class);
    }

    @Test
    void createView_duplicateView_throwsDuplicateViewException() {
        when(chronicleService.existsByUserIdAndName(USER_ID, CHRONICLE_NAME)).thenReturn(true);
        when(viewRepository.exists(USER_ID, CHRONICLE_NAME, VIEW_NAME)).thenReturn(true);

        assertThatThrownBy(() -> viewService.createView(USER_ID, CHRONICLE_NAME, VIEW_NAME))
                .isInstanceOf(DuplicateViewException.class);
    }
}