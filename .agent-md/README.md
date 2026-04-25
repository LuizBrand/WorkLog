# agent-md Helpers

These are plain shell helpers installed by agent-md. They are not Codex
skills. Native Codex skills live under `.agents/skills/<name>/SKILL.md`.

## Usage

    ./.agent-md/bin/discover_helpers.sh          # list helpers
    ./.agent-md/bin/discover_helpers.sh visual   # search helpers
    ./.agent-md/bin/doctor.sh                    # check install wiring
    ./.agent-md/bin/playwright-capture.sh <url>  # capture UI evidence

## Bundled Helpers

- `discover_helpers.sh` — lists/searches local helper scripts
- `doctor.sh` — checks common install wiring problems
- `playwright-capture.sh` — headless browser screenshot for UI validation

## Adding a Helper

1. Create `.agent-md/bin/<name>.sh`.
2. The first 1-2 comment lines must describe what the helper does.
   `discover_helpers` uses them as the search snippet.
3. Keep each helper self-contained. No side effects on load, only on
   execution.
4. Document required dependencies in the header.

## Philosophy

Do not load every possible tool schema or workflow into the model's
prompt. Keep reusable mechanics as scripts, then let the agent discover
and run the one it needs for the current task.
