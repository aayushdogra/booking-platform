package com.booking.platform.event.consumer;

import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

public class RetryClassifier {

    public static RetryDecision classify(Exception ex) {

        Throwable root = rootCause(ex);

        if(root instanceof ObjectOptimisticLockingFailureException ||
            root instanceof  TransientDataAccessResourceException) {
            return RetryDecision.RETRYABLE;
        }

        return RetryDecision.NON_RETRYABLE;
    }

    private static Throwable rootCause(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
}
