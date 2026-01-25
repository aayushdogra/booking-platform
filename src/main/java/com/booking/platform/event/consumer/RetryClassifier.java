package com.booking.platform.event.consumer;

import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

public class RetryClassifier {

    public static RetryDecision classify(Exception ex) {

        if(ex instanceof ObjectOptimisticLockingFailureException ||
            ex instanceof  TransientDataAccessResourceException) {
            return RetryDecision.RETRYABLE;
        }

        return RetryDecision.NON_RETRYABLE;
    }
}
