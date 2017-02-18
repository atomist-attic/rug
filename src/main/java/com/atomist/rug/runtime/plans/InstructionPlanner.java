package com.atomist.rug.runtime.plans;

import com.atomist.rug.spi.JavaHandlers.Instruction;
import com.atomist.rug.spi.JavaHandlers.Response;

public interface InstructionPlanner {

    /**
     * Runs an instruction.
     *
     * @param instruction a declarative instruction to run
     * @param callbackInput if this was a callback, the result of running the instruction, null otherwise
     * @return response from running this instruction
     */
    Response run(Instruction instruction, Object callbackInput);
}
