---
name: pr-review-stability
description:
    Enforces stable, repeatable, system-level pull request reviews for java-tron, focusing on lifecycle safety, executor shutdown semantics, business logic correctness, numeric determinism, and cross-JVM consistency.
---

# PR Review Stability & Determinism Checks (java-tron)

This skill defines **mandatory review behavior** for Gemini when reviewing
Pull Requests in the `java-tron` repository.

The goal is to prevent:

* Lifecycle and resource leaks
* Executor shutdown races
* Silent behavior changes
* Business logic bugs
* Numeric precision, overflow, and cross-JVM non-determinism
* Consensus and state inconsistency risks

---

## Scope

Applies to **all Pull Requests**, regardless of size or perceived risk.

---

## Execution Rules (Must Be Followed in Order)

### Step 1: Change Type Classification (Mandatory)

Before any detailed analysis, the PR **must be classified** as one of the following:

* Modification of existing behavior
* Introduction of new logic
* Mixed (modification + addition)

This classification **must be explicitly stated** in the review output.

---

### Step 2: Mandatory Safety Checks (No Skipping)

Each section below **must be explicitly executed and reported**.
If no issues are found, explicitly state: **“Checked, no issues found.”**

---

### 2.1 Lifecycle & Resource Safety (Mandatory)

When code involves:

* `open / init / start`
* `close / shutdown / stop`
* Long-lived resources created during initialization or construction

The review **must**:

* Explicitly list all resources created (objects, connections, executors,
  threads, subscriptions, caches, etc.)
* Verify each resource is released in:

    * Normal shutdown paths
    * Exception paths
    * Early return paths
* Never assume GC or external components perform cleanup

Flag cases where:

* Multiple resources are created but only partially released
* Cleanup depends on implicit preconditions
* Failure paths skip cleanup logic

---

### 2.2 Executor / Thread Pool Shutdown Semantics (Mandatory)

When `ExecutorService` or thread pools are involved, the review **must check**:

* Whether `submit / execute` paths remain reachable after shutdown
* Whether tasks can be submitted during shutdown due to race windows
* Whether callers may continue submitting tasks without knowing the executor is closed
* Whether tasks may be silently dropped, delayed indefinitely, or block permanently

The review **must explicitly describe**:

* Whether the risk is real
* The triggering conditions
* The potential system impact

---

### 2.3 Behavioral Compatibility & Implicit Invariants (Mandatory)

For PRs modifying existing logic, the review **must evaluate**:

* Externally observable behavior changes
* Broken implicit invariants (ordering, cardinality, idempotency, state assumptions)
* Whether upstream callers still satisfy new assumptions
* Whether downstream behavior may change silently

Even improvements must be evaluated for compatibility risk.

---

### 2.4 Business Logic Correctness (Mandatory)

The review **must check**:

* Missing validation paths
* Incorrect branching or state transitions
* Partial state updates or intermediate state exposure
* Error handling paths that allow execution to continue inconsistently

Pay special attention to:

* Multi-step updates
* Early returns
* Exception handling followed by continued execution

---

### 2.5 Concurrency & Execution Interleaving Risks (Mandatory)

When concurrency or async behavior exists, the review **must assess**:

* Thread safety of shared mutable state
* Race conditions or ordering dependencies
* Reentrancy through callbacks or listeners
* Unexpected interleavings across execution paths

---

### 2.6 Numeric Precision & Cross-JVM Determinism (Mandatory)

When code involves:

* `float` / `double`
* Accumulation, division, ratios, or scaling
* Type conversions (`long → int`, `double → long`)
* Monetary values, weights, scores, counters, or critical numeric state

The review **must evaluate**:

* Precision loss or rounding differences
* Overflow or underflow risks
* JVM- or platform-dependent computation differences
* Use of non-deterministic numeric results in
  consensus, ordering, comparison, or state transitions

Explicitly call out any cross-node determinism risk.

---

### Step 3: System-Level Risk Summary (Mandatory)

Summarize whether the PR introduces or amplifies:

* Consensus risk
* State inconsistency risk
* Lifecycle or resource leak risk
* Concurrency or execution-order risk
* Cross-JVM non-determinism risk

---

## Output Format Requirements (Mandatory)

The review output **must follow this exact order**:

1. PR Change Type Classification
2. Lifecycle & Resource Safety
3. Executor / Thread Pool Shutdown Semantics
4. Behavioral Compatibility & Implicit Invariants
5. Business Logic Correctness
6. Concurrency & Execution Interleaving Risks
7. Numeric Precision & Determinism
8. System-Level Risk Summary

No section may be merged, skipped, or omitted.

---

## Tone & Style

* Strict
* Deterministic
* Senior system reviewer mindset
* Conservative: prefer flagging risks over missing them
