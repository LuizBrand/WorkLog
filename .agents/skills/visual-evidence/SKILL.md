---
name: visual-evidence
description: Use for UI changes in an agent-md repository; captures a screenshot and writes the structured markdown evidence required by the visual hook.
---

# Visual Evidence

Use this skill for any UI-facing change.

1. Build or start the app in the normal project-specific way.
2. Capture the changed route with:

```bash
./.agent-md/bin/playwright-capture.sh <url> .agent/visual/<name>.png
```

3. Write a markdown note next to the image under `.agent/visual/`.
4. The note must reference the image filename and include these fields:

```markdown
# Visual Check

Changed files:
- <path>

Route: <url or route>
Viewport: <width>x<height>
Artifact: <image filename>
Observed result: <what was verified>
```

5. Use an independent verifier when possible: another agent, a browser screenshot inspection, or the human.
6. Do not self-grade visual correctness from code alone.
