package org.tron.core.utils;

import java.lang.reflect.Method;
import org.junit.Assert;
import org.junit.Test;
import org.tron.common.runtime.ProgramResult;
import org.tron.common.runtime.RuntimeImpl;
import org.tron.core.vm.program.Program;
import org.tron.core.vm.program.Program.BadJumpDestinationException;
import org.tron.core.vm.program.Program.OutOfEnergyException;
import org.tron.core.vm.program.Program.OutOfTimeException;
import org.tron.protos.Protocol.Transaction.Result.contractResult;

public class ResultCodeUtilTest {

  @Test
  public void testTypedExceptionsResolveToTheirCodes() {
    Assert.assertEquals(contractResult.OUT_OF_ENERGY,
        ResultCodeUtil.resolve(new OutOfEnergyException("out of energy")));
    Assert.assertEquals(contractResult.OUT_OF_TIME,
        ResultCodeUtil.resolve(new OutOfTimeException("out of time")));
    Assert.assertEquals(contractResult.BAD_JUMP_DESTINATION,
        ResultCodeUtil.resolve(new BadJumpDestinationException("bad jump")));
  }

  @Test
  public void testUntypedExceptionResolvesToUnknown() {
    Assert.assertEquals(contractResult.UNKNOWN,
        ResultCodeUtil.resolve(new RuntimeException("untyped failure")));
  }

  @Test
  public void testNullExceptionResolvesToUnknown() {
    Assert.assertEquals(contractResult.UNKNOWN, ResultCodeUtil.resolve(null));
  }

  @Test
  public void testStaysInSyncWithConsensusClassifier() throws Exception {
    RuntimeException[] samples = {
        new Program.IllegalOperationException("op"),
        new Program.OutOfEnergyException("energy"),
        new Program.BadJumpDestinationException("jump"),
        new Program.OutOfTimeException("time"),
        new Program.OutOfMemoryException("mem"),
        new Program.PrecompiledContractException("pre"),
        new Program.StackTooSmallException("small"),
        new Program.JVMStackOverFlowException(),
        new Program.TransferException("transfer"),
        new Program.InvalidCodeException("code"),
        new Program.StaticCallModificationException(),
        new RuntimeException("untyped"),
    };

    Method consensus = RuntimeImpl.class.getDeclaredMethod("setResultCode", ProgramResult.class);
    consensus.setAccessible(true);
    RuntimeImpl runtime = new RuntimeImpl();

    for (RuntimeException e : samples) {
      ProgramResult result = new ProgramResult();
      result.setException(e);
      consensus.invoke(runtime, result);
      Assert.assertEquals("classifiers diverged for " + e.getClass().getSimpleName(),
          result.getResultCode(), ResultCodeUtil.resolve(e));
    }
  }
}
