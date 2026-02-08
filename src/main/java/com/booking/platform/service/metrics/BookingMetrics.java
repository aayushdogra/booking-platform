package com.booking.platform.service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class BookingMetrics {

    private final Counter bookingCreatedCounter;
    private final Counter bookingFailedCounter;

    private final Counter paymentSuccessCounter;
    private final Counter paymentFailureCounter;

    private final Counter refundSuccessCounter;
    private final Counter refundFailureCounter;

    private final Timer paymentLatencyTimer;
    private final Timer refundLatencyTimer;

    private final Counter paymentRetryCounter;
    private final Counter refundRetryCounter;

    private final Counter refundRequestedCounter;

    public BookingMetrics(MeterRegistry registry) {
        this.bookingCreatedCounter = registry.counter("booking.created");
        this.bookingFailedCounter = registry.counter("booking.failed");

        this.paymentSuccessCounter = registry.counter("payment.success");
        this.paymentFailureCounter = registry.counter("payment.failed");

        this.refundSuccessCounter = registry.counter("refund.completed");
        this.refundFailureCounter = registry.counter("refund.failed");

        this.paymentLatencyTimer = registry.timer("payment.latency");
        this.refundLatencyTimer = registry.timer("refund.latency");

        this.paymentRetryCounter = registry.counter("payment.retry");
        this.refundRetryCounter = registry.counter("refund.retry");

        this.refundRequestedCounter = registry.counter("refund.requested");
    }

    public void incrementBookingCreated() {
        bookingCreatedCounter.increment();
    }

    public void incrementBookingFailed() {
        bookingFailedCounter.increment();
    }

    public void incrementPaymentSuccess() {
        paymentSuccessCounter.increment();
    }

    public void incrementPaymentFailure() {
        paymentFailureCounter.increment();
    }

    public void incrementRefundSuccess() {
        refundSuccessCounter.increment();
    }

    public void incrementRefundFailure() {
        refundFailureCounter.increment();
    }

    public Timer.Sample startTimer(MeterRegistry registry) {
        return Timer.start(registry);
    }

    public void stopPaymentTimer(Timer.Sample sample) {
        sample.stop(paymentLatencyTimer);
    }

    public void stopRefundTimer(Timer.Sample sample) {
        sample.stop(refundLatencyTimer);
    }

    public void incrementPaymentRetry() {
        paymentRetryCounter.increment();
    }

    public void incrementRefundRetry() {
        refundRetryCounter.increment();
    }

    public void incrementRefundRequested() {
        refundRequestedCounter.increment();
    }
}
