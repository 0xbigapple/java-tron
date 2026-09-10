package org.tron.core.jsonrpc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.tron.common.BaseTest;
import org.tron.common.TestConstants;
import org.tron.common.logsfilter.capsule.LogsFilterCapsule;
import org.tron.common.logsfilter.queue.FilterCapsuleQueue;
import org.tron.common.parameter.CommonParameter;
import org.tron.common.runtime.vm.DataWord;
import org.tron.common.runtime.vm.LogInfo;
import org.tron.core.config.args.Args;
import org.tron.core.services.jsonrpc.TronJsonRpc.FilterRequest;
import org.tron.core.services.jsonrpc.TronJsonRpcImpl;
import org.tron.core.services.jsonrpc.filters.LogFilterAndResult;
import org.tron.protos.Protocol.TransactionInfo;

/**
 * Shutdown semantics of the decoupled filter pipeline. Method order matters:
 * test1 closes the shared TronJsonRpcImpl bean, test2 asserts the second close
 * is a guarded no-op, so they run in name order.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FilterPipelineShutdownTest extends BaseTest {

  static {
    Args.setParam(new String[] {"--output-directory", dbPath()}, TestConstants.TEST_CONF);
    // isJsonRpcFilterEnabled() must hold at context startup so the consumer thread starts
    CommonParameter.getInstance().setJsonRpcHttpFullNodeEnable(true);
  }

  @Resource
  private TronJsonRpcImpl tronJsonRpc;
  @Resource
  private FilterCapsuleQueue filterCapsuleQueue;

  private static TransactionInfo buildTxInfoWithLog() {
    LogInfo logInfo = new LogInfo(new byte[20],
        Collections.singletonList(new DataWord(new byte[32])), new byte[0]);
    return TransactionInfo.newBuilder().addLog(LogInfo.buildLog(logInfo)).build();
  }

  /**
   * close() while the consumer is mid-capsule on the parallel (logsFilterPool) path:
   * the in-flight capsule must complete instead of dying on a RejectedExecutionException,
   * which is exactly the filterEs-before-logsFilterPool ordering constraint.
   */
  @Test
  public void test1CloseDuringParallelProcessingLosesNoEvent() throws Exception {
    tronJsonRpc.setFilterParallelThreshold(0);
    LogFilterAndResult filter = new LogFilterAndResult(new FilterRequest(), 100L, null);
    tronJsonRpc.getEventFilter2ResultFull().put("shutdown-race", filter);

    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    LogsFilterCapsule capsule = new LogsFilterCapsule(150L, "0xrace", null,
        Collections.singletonList(buildTxInfoWithLog()), false, false) {
      @Override
      public boolean isSolidified() {
        // first call is the head of handleLogsFilter, on the consumer thread
        entered.countDown();
        try {
          release.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        return false;
      }
    };
    filterCapsuleQueue.offer(capsule);
    Assert.assertTrue("consumer did not pick up the capsule",
        entered.await(10, TimeUnit.SECONDS));

    Thread closer = new Thread(() -> {
      try {
        tronJsonRpc.close();
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }, "test-closer");
    closer.start();
    // close() must be parked awaiting the consumer, with logsFilterPool still open
    Thread.sleep(500);
    Assert.assertTrue("close() finished while a capsule was in flight", closer.isAlive());

    release.countDown();
    closer.join(150_000);
    Assert.assertFalse("close() did not finish", closer.isAlive());
    Assert.assertEquals("in-flight capsule was lost during close()",
        1, filter.getResult().size());
  }

  @Test
  public void test2RepeatedCloseIsIdempotentAndThreadStopped() throws Exception {
    long deadline = System.currentTimeMillis() + 10_000;
    while (System.currentTimeMillis() < deadline && filterThreadAlive()) {
      Thread.sleep(50);
    }
    Assert.assertFalse("consumer thread still alive after close()", filterThreadAlive());

    long t0 = System.nanoTime();
    tronJsonRpc.close();
    long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
    Assert.assertTrue("repeated close should be a fast no-op, took " + elapsedMs + "ms",
        elapsedMs < 1_000);
  }

  private static boolean filterThreadAlive() {
    return Thread.getAllStackTraces().keySet().stream()
        .anyMatch(t -> "filter".equals(t.getName()) && t.isAlive());
  }
}
