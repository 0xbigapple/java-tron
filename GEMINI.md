# Gemini PR Review Instructions — java-tron

This document defines how Gemini should review **Pull Requests** in the `java-tron` repository.

The goal is to identify **consensus risks, state inconsistencies, lifecycle issues, and system-level regressions**, not style problems.

---

## 1. Project Context

`java-tron` is a **blockchain full node implementation**.

Any code change may affect:

* Consensus safety
* State machine correctness
* Cross-node determinism
* Network stability
* Long-running node lifecycle

PR review must therefore focus on **system-level correctness and safety**, not just local logic.

---

## 2. Primary Review Objectives (Highest Priority)

### 2.1 Consensus & Determinism

Look for changes that may cause **different nodes to produce different results given the same input**.

Examples include:

* Time-dependent logic
* Randomness
* Unstable iteration order
* Floating-point arithmetic
* Inconsistent handling of `Proposal / Block / View / Round`

---

### 2.2 State Machine Integrity

* Partial or inconsistent state updates
* State mutation before all validations complete
* Missing rollback or compensation on failure paths

---

### 2.3 Lifecycle & Resource Safety

* Thread, executor, database, or network resource leaks
* Incorrect initialization, startup, or shutdown ordering
* Assumptions that components are “always alive” or “single-use”

---

### 2.4 Concurrency & Hidden Races

* Unsynchronized shared mutable state
* Reentrancy risks
* Unexpected interleavings between asynchronous callbacks

---

### 2.5 Silent Failure

* Exceptions swallowed or only logged
* Errors converted into default values
* Node continues running in degraded or inconsistent state

---

## 3. DoS Risk Consideration

For any change involving **external input** (network messages, blocks, transactions, proposals):

* Watch for CPU, memory, or IO amplification
* Identify unbounded loops, queues, or retries
* Highlight paths triggerable repeatedly by malicious peers

---

## 4. Scope Discipline

* Focus primarily on **the PR diff**
* Do not re-review unrelated legacy code
* Expand context only when required for correctness or safety
* Avoid large refactors or style-only suggestions

---

## 5. Change-Type Awareness (Critical)

Before detailed review, **first classify the PR**:

### 5.1 Modification (Behavior Change)

When existing logic is modified, focus on:

* Whether behavioral semantics remain consistent
* Whether implicit invariants are broken
* Whether upstream callers still satisfy new assumptions
* Whether downstream behavior changes silently

Even small diffs may cause major consensus or state impact.

---

### 5.2 Addition (New Logic)

When new logic is introduced, focus on:

* Whether all required call paths integrate it correctly
* Whether lifecycle, concurrency, and error handling are complete
* Whether initialization, cleanup, or configuration is missing
* Whether new implicit dependencies are introduced

New code risk usually lies in **integration boundaries**, not internal implementation.

---

## 6. Review Output Expectations

When reporting issues, clearly explain:

* What could go wrong
* Under what conditions
* Why it matters for node correctness or network safety

Prefer **high-signal, concise, critical feedback**.

If no major issues are found, explicitly state that the change appears safe with respect to:

* Consensus
* State consistency
* Lifecycle safety

---

## 7. What Not To Do

* Do not focus on formatting or naming
* Do not suggest large refactors unrelated to PR intent
* Do not assume single-node or happy-path execution