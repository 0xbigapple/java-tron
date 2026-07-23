package org.tron.core.utils;

import org.tron.core.vm.program.Program;
import org.tron.protos.Protocol.Transaction.Result.contractResult;

/**
 * Maps a TVM execution exception to its {@code contractResult} code for constant-call
 * responses. Deliberately independent of RuntimeImpl's consensus-path classification.
 */
public class ResultCodeUtil {

  public static contractResult resolve(RuntimeException exception) {
    if (exception instanceof Program.IllegalOperationException) {
      return contractResult.ILLEGAL_OPERATION;
    }
    if (exception instanceof Program.OutOfEnergyException) {
      return contractResult.OUT_OF_ENERGY;
    }
    if (exception instanceof Program.BadJumpDestinationException) {
      return contractResult.BAD_JUMP_DESTINATION;
    }
    if (exception instanceof Program.OutOfTimeException) {
      return contractResult.OUT_OF_TIME;
    }
    if (exception instanceof Program.OutOfMemoryException) {
      return contractResult.OUT_OF_MEMORY;
    }
    if (exception instanceof Program.PrecompiledContractException) {
      return contractResult.PRECOMPILED_CONTRACT;
    }
    if (exception instanceof Program.StackTooSmallException) {
      return contractResult.STACK_TOO_SMALL;
    }
    if (exception instanceof Program.StackTooLargeException) {
      return contractResult.STACK_TOO_LARGE;
    }
    if (exception instanceof Program.JVMStackOverFlowException) {
      return contractResult.JVM_STACK_OVER_FLOW;
    }
    if (exception instanceof Program.TransferException) {
      return contractResult.TRANSFER_FAILED;
    }
    if (exception instanceof Program.InvalidCodeException) {
      return contractResult.INVALID_CODE;
    }
    return contractResult.UNKNOWN;
  }
}
