package com.atomist.rug.runtime.execution;


import com.atomist.rug.spi.JavaHandlers.Instruction;
import com.atomist.rug.spi.JavaHandlers.Response;

public interface InstructionExecutor {

    /**
     * Executes an instruction.
     *
     * @param  instruction a declarative instruction to execute
     * @param  callbackInput if this was a callback, the result of executing the instruction, null otherwise
     * @return response from executing this instruction
     */
    Response execute(Instruction instruction, Object callbackInput);

}
