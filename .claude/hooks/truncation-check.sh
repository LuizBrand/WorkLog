#!/bin/bash
# truncation-check.sh
# Runs after Grep and Bash tool calls.
# Detects when tool output was truncated (>50K chars -> 2KB preview).
# Injects a warning so the agent knows to read the full file or narrow scope.

INPUT=$(cat)

# Extract tool_response as string - handles both string and object responses
TOOL_RESPONSE=$(echo "$INPUT" | jq -r '
  if (.tool_response | type) == "string" then .tool_response
  elif (.tool_response | type) == "object" then (.tool_response | tostring)
  else empty
  end
')

# Check for the persisted-output truncation marker
if echo "$TOOL_RESPONSE" | grep -q "Output too large"; then
  # Warn but don't block - the tool already ran, blocking won't undo it
  MSG="WARNING: Tool output was truncated to a 2KB preview. The full output was saved to disk. Read the full file at the given path before acting on these results, or re-run with narrower scope (single directory, stricter pattern)."
  jq -n --arg m "$MSG" '{hookSpecificOutput: {hookEventName: "PostToolUse", additionalContext: $m}}'
  exit 0
fi

# Note: previous versions warned on low Grep result counts as "possibly
# truncated". That was backwards — truncation happens from TOO MUCH output.
# Precise searches returning 0–4 hits are legitimate and common. Removed.

exit 0
