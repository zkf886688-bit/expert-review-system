package com.review.system.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class AppMetrics {

    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter emailSentCounter;
    private final Counter emailFailedCounter;
    private final Counter smsSentCounter;
    private final Counter smsFailedCounter;

    private final AtomicLong tasksTotal = new AtomicLong();
    private final AtomicLong tasksInReview = new AtomicLong();
    private final AtomicLong assignmentsOverdue = new AtomicLong();

    public AppMetrics(MeterRegistry registry) {
        loginSuccessCounter = Counter.builder("auth.login.success")
                .description("Successful login attempts")
                .register(registry);
        loginFailureCounter = Counter.builder("auth.login.failure")
                .description("Failed login attempts")
                .register(registry);
        emailSentCounter = Counter.builder("notification.email.sent")
                .description("Email notifications sent successfully")
                .register(registry);
        emailFailedCounter = Counter.builder("notification.email.failed")
                .description("Email notification send failures")
                .register(registry);
        smsSentCounter = Counter.builder("notification.sms.sent")
                .description("SMS notifications sent successfully")
                .register(registry);
        smsFailedCounter = Counter.builder("notification.sms.failed")
                .description("SMS notification send failures")
                .register(registry);

        Gauge.builder("review.tasks.total", tasksTotal, AtomicLong::get)
                .description("Total review tasks in system")
                .register(registry);
        Gauge.builder("review.tasks.in_review", tasksInReview, AtomicLong::get)
                .description("Tasks currently in review")
                .register(registry);
        Gauge.builder("review.assignments.overdue", assignmentsOverdue, AtomicLong::get)
                .description("Overdue expert assignments")
                .register(registry);
    }

    public void recordLoginSuccess() {
        loginSuccessCounter.increment();
    }

    public void recordLoginFailure() {
        loginFailureCounter.increment();
    }

    public void recordEmail(boolean success) {
        if (success) {
            emailSentCounter.increment();
        } else {
            emailFailedCounter.increment();
        }
    }

    public void recordSms(boolean success) {
        if (success) {
            smsSentCounter.increment();
        } else {
            smsFailedCounter.increment();
        }
    }

    public void updateReviewSnapshot(long totalTasks, long inReviewTasks, long overdueAssignments) {
        tasksTotal.set(totalTasks);
        tasksInReview.set(inReviewTasks);
        assignmentsOverdue.set(overdueAssignments);
    }
}
