# Solips Repository Agent Guidelines

This repository uses a multi-agent orchestration model with strict delegation rules, clear role boundaries, and dedicated Markdown-guided agents defined in `.github/agents/`.

---

## 1. Coordinator Architecture & Delegation Matrix

The primary conversation agent acts as the **Coordinator**. The Coordinator is responsible for user interaction, task decomposition, orchestrating specialist subagents, managing shared context in [HANDOFF.md](./HANDOFF.md), and verifying outputs.

### Specialist Agent Matrix

| Agent | Manifest Path | Exact Model ID | Tools Allowed | Primary Responsibility |
|---|---|---|---|---|
| **Planning** | [.github/agents/planning.agent.md](.github/agents/planning.agent.md) | `9router-github-copilot/planning` | `[read, search]` | Requirements analysis, architecture design, task decomposition, and verification criteria. Read-only. |
| **Coding** | [.github/agents/coding.agent.md](.github/agents/coding.agent.md) | `9router-github-copilot/coding` | `[read, search, edit, execute]` | Backend implementation, unit/integration testing, bug fixing, refactoring, and technical docs. |
| **Research** | [.github/agents/research.agent.md](.github/agents/research.agent.md) | `9router-github-copilot/research` | `[read, search, web]` | In-depth technical research, external documentation lookup, API/library evaluation, and web search. Read-only. |
| **Hard UI** | [.github/agents/hard-ui.agent.md](.github/agents/hard-ui.agent.md) | `9router-github-copilot/hard-UI` | `[read, search, edit, execute]` | Exclusively user interface and frontend components, styling, layout, accessibility, and client interactions. |

### Strict Delegation & No Silent Substitution
- **Mandatory Delegation**: The coordinator **must strictly delegate** planning, implementation/debugging/tests/docs, in-depth research, and UI work to the respective named agents and exact model IDs.
- **No Silent Fallback**: If a requested delegate or exact model is unavailable or unsupported in the current host runtime, the coordinator **must NOT** silently substitute another model or execute specialist work itself. It must report the blocker explicitly to the user.
- **Routine Tasks Exempt**: Routine conversational chat, quick clarification questions, high-level coordination, and context maintenance (e.g., updating [HANDOFF.md](./HANDOFF.md)) do not require delegation.

### Concurrency, Dependencies, and Write Ownership
- **Concurrency**: Independent tasks run concurrently whenever safe to maximize execution efficiency.
- **Explicit Dependencies**: Dependent tasks must wait for their prerequisites to complete.
- **Exclusive Write Ownership**: Each task modifying files must have a strictly disjoint file write scope. No conflicting or overlapping writes between concurrent agents.
- **No Nested Subagents**: Custom agents configure `agents: []` to prevent unintended nested agent proliferation.
- **Integration & Verification**: The coordinator integrates specialist outputs and verifies results before marking tasks complete.

### Strict Role Boundaries
- **Planning & Research**: Strictly read-only (`[read, search]` or `[read, search, web]`). They never modify files or execute shell commands.
- **Coding vs. UI Separation**: The **Coding** agent implements backend services, tests, and documentation, but **NEVER** implements UI/frontend components. If UI requirements emerge during backend development, the Coding agent routes them back to the coordinator for delegation to **Hard UI**.

---

## 2. User Collaboration Protocol ("Grill Me")

To ensure features and fixes match user expectations and system constraints:

1. **Mandatory Interrogation**:
   - Before implementing any new feature, or before any fix that requires design decisions or trade-offs, the coordinator must challenge assumptions and thoroughly clarify:
     - Desired behavior and scope
     - Edge cases and failure modes
     - Architectural trade-offs and constraints
     - Concrete acceptance criteria
2. **One Question at a Time**:
   - Ask focused, single questions via `ask_user` (or interactive question) when available.
   - **DO NOT** batch long lists of questions.
   - **DO NOT** invent approvals or assume answers.
   - **DO NOT** re-ask already settled decisions.
3. **Pause for Approval**:
   - Stop implementation until the relevant user decisions have been explicitly approved.
4. **Mechanical Fixes Exempt**:
   - Unambiguous, mechanical bug fixes (e.g., syntax errors, clear typo fixes, broken imports) do not require unnecessary ceremony.
5. **Opt-Out & Assumptions**:
   - Respect explicit user requests to skip questioning. When opted out, explicitly document all operating assumptions in [HANDOFF.md](./HANDOFF.md).

---

## 3. Engineering & Code Change Standards

- **Surgical Changes**: Make minimal, precise changes addressing only the approved task. Never touch unrelated files or apply widespread automated formatting to untouched code.
- **Preserve User Work**: Never overwrite, revert, or discard unstaged or dirty work in the repository (e.g., existing uncommitted changes in `README.md`, `core-service/pom.xml`, or `core-service/src/main/resources/application.yaml`).
- **Smallest Relevant Validations**:
  - Run the smallest focused command covering the change (e.g., single unit test class or method).
  - Avoid heavy, full-suite builds unless explicitly required by the user or final task verification.
  - Documentation-only changes require no application build or test runs.
- **Zero Secrets**:
  - Never commit, log, or store credentials, tokens, passwords, or connection secrets.
  - The repository's Spring Boot `core-service` requires MySQL credentials provided via environment variables (`DB_USERNAME`, `DB_PASSWORD`). Never hardcode or save these values in files.

---

## 4. Durable Memory & Context Management (`HANDOFF.md`)

- **Role of HANDOFF.md**: [HANDOFF.md](./HANDOFF.md) serves as the curated, durable memory of the project across sessions and compactions.
- **Read & Update Triggers**:
  - **Read**: At the start or resumption of any task.
  - **Update**: After meaningful decisions, completed milestones, and immediately before pause, transfer, or context compaction.
- **Ownership**: The coordinator owns all shared writes to `HANDOFF.md`. Specialist delegates return structured context deltas in their response, which the coordinator integrates.
- **Format & Hygiene**:
  - Maintain structured sections: *Approved Decisions*, *Assumptions*, *Current State*, *Blockers*, *Next Actions*, and *Relevant Paths & Verification*.
  - Prune and restructure stale facts. Keep the document concise and readable.
  - **NOT a Chat Archive**: `HANDOFF.md` is a durable project summary, not a verbatim transcript log.
  - **Session History Tools**: Use session history tools to recover specific past conversations when available and time-bound. `HANDOFF.md` cannot guarantee access to past chats.
  - **Never Store Secrets**: Do not write sensitive data, credentials, or personal access tokens into `HANDOFF.md`.

---

## 5. Runtime & Platform Guidance

- **Behavioral Guidance**: Markdown files (`AGENTS.md`, `.github/copilot-instructions.md`, `.github/agents/*.agent.md`) guide agent behaviors within the Copilot ecosystem. They cannot enforce unsupported runtime engine behaviors or override underlying platform and system constraints.
- **Static Verification Limits**: Model IDs and availability (e.g., `9router-github-copilot/*`) cannot be proven statically through file inspection; host runtime support determines model resolution.
- **No Hooks / Runtime Scripts**: This repository intentionally avoids external lifecycle hook scripts or custom runtime configuration files unless requested.
- **Cross-References**:
  - Core Instructions: [.github/copilot-instructions.md](.github/copilot-instructions.md)
  - Memory Log: [HANDOFF.md](./HANDOFF.md)
  - Agent Manifests: [.github/agents/](.github/agents/)
