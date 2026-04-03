package io.github.grantchen2003.cdb.control.plane.views;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class ViewService {

    private final ViewRepository viewRepository;

    public ViewService(ViewRepository viewRepository) {
        this.viewRepository = viewRepository;
    }

    public View createView(String userId, String chronicleName, String viewName) {
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

    public boolean exists(String userId, String chronicleName, String viewName) {
        return viewRepository.exists(userId, chronicleName, viewName);
    }
}
