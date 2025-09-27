package com.Abhinav.backend.features.submissions.events;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.util.UUID;


@Getter
public class SubmissionCreatedEvent extends ApplicationEvent {
    private final UUID submissionId;

    public SubmissionCreatedEvent(Object source, UUID submissionId) {
        super(source);
        this.submissionId = submissionId;
    }

}