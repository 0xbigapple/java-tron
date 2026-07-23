package org.tron.core.exception.jsonrpc;

public class JsonRpcExecutionRevertedException extends JsonRpcInternalException {

  public JsonRpcExecutionRevertedException(String message) {
    super(message);
  }

  public JsonRpcExecutionRevertedException(String message, Object data) {
    super(message, data);
  }
}
