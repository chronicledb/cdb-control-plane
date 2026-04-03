package io.github.grantchen2003.cdb.control.plane.views;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewServiceTest {

    private static final String USER_ID        = "user-123";
    private static final String CHRONICLE_NAME = "my-chronicle";
    private static final String VIEW_NAME      = "my-view";

    @Mock
    private ViewRepository viewRepository;

    @InjectMocks
    private ViewService viewService;

    @Test
    void createView_savesViewAndReturnsIt() {
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
}