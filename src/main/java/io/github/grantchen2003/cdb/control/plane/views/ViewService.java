package io.github.grantchen2003.cdb.control.plane.views;

import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleNotFoundException;
import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class ViewService {

    private final ChronicleService chronicleService;
    private final ViewRepository viewRepository;

    public ViewService(ChronicleService chronicleService, ViewRepository viewRepository) {
        this.chronicleService = chronicleService;
        this.viewRepository = viewRepository;
    }

    public View createView(String userId, String chronicleName, String viewName) {
        if (!chronicleService.existsByUserIdAndName(userId, chronicleName)) {
            throw new ChronicleNotFoundException();
        }

        if (viewRepository.exists(userId, chronicleName, viewName)) {
            throw new DuplicateViewException();
        }

        final View view = new View(
                UUID.randomUUID().toString(),
                userId,
                chronicleName,
                viewName,
                Instant.now()
        );

        viewRepository.save(view);

        return view;
    }

    public Optional<View> findByViewId(String viewId) {
        return viewRepository.findByViewId(viewId);
    }

    public boolean exists(String userId, String chronicleName, String viewName) {
        return viewRepository.exists(userId, chronicleName, viewName);
    }
}