package com.atomist.rug.runtime.plans;

import com.atomist.rug.spi.Handlers;

public interface MessageDeliverer {

    /**
     * Deliver a message.
     *
     * @param message a message to deliver
     * @param callbackInput if this was a callback, the result of executing the instruction, null otherwise
     */
    void deliver(Handlers.Message message, Object callbackInput);
}
