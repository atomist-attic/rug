package com.atomist.rug.runtime.plans;

import com.atomist.rug.spi.Handlers.Instruction;
import com.atomist.rug.spi.Handlers.Response;
import scala.Option;

public interface InstructionRunner {

    /**
     * Runs an instruction.
     *
     * @param instruction a declarative instruction to run
     * @param callbackInput if this was a callback, the result of running the instruction, null otherwise
     * @return response from running this instruction
     */
    Response run(Instruction instruction, Option<Response> callbackInput);
}
