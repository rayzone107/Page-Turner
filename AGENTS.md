# Agent Definitions — PageTurner

This project uses the CRAFT framework for AI-assisted development.
Each agent has a defined role, context, and output contract.

## CRAFT Pipeline
C -> Clarify (PM Agent)     -> produces: feature spec
R -> Render (Design Agent)  -> produces: component specs
A -> Assemble (Dev Agent)   -> produces: implementation
F -> Fortify (Test Agent)   -> produces: test suite
T -> Tighten (Review Agent) -> produces: review report and sign-off

## Branch naming
agent/pm-{feature}
agent/design-{feature}
agent/dev-{feature}
agent/test-{feature}
agent/review-{feature}

## Agent system prompts
Section 13 of docs/ANDROID_PROJECT_PLAN.md contains full system prompts.
Individual prompt files are stored in docs/agents/:
  pm-agent.md
  design-agent.md
  dev-agent.md
  test-agent.md
  review-agent.md

## How to invoke an agent in Claude Code
1. git checkout -b agent/{role}-{feature} from develop
2. Paste the agent's system prompt as the first message
3. Provide the task template with context filled in
4. Commit the output: git commit -m "agent({role}): {description}"
5. Open PR to develop with MAARS checklist completed
