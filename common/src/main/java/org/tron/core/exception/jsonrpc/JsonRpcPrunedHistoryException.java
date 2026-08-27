package org.tron.core.exception.jsonrpc;

/**
 * Thrown when a request targets historical state that a LiteNode has pruned.
 * Maps to JSON-RPC error code 4444 "Pruned history unavailable", as standardized
 * by the Ethereum Execution API (EIP-4444).
 */
public class JsonRpcPrunedHistoryException extends JsonRpcInternalException {

  public JsonRpcPrunedHistoryException(String message) {
    super(message);
  }

  public JsonRpcPrunedHistoryException(String message, Object data) {
    super(message, data);
  }
}
