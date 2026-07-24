# Liubai Repository Guidance

Before making changes, read `DEVELOPMENT_MEMORY.md` completely. It is the
project handoff for architecture, compatibility guarantees, completed tests,
known defects, and current priorities.

After a material implementation, compatibility, release, or behavior change,
update `DEVELOPMENT_MEMORY.md` in the same task. Keep confirmed current behavior
separate from plans, and remove stale statements instead of accumulating an
unreliable changelog.

Do not claim planned LOD, proxy-model, GPU occlusion, or broad mod integration
as implemented. Verify release metadata inside the built JAR, not only in the
Gradle template.
