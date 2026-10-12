# Rule: User Confirmation Required for Builds

## Constraint
Never autonomously run build, assemble, or packaging commands without explicit user permission.

## Applicable Commands
- Gradle builds (e.g., `./gradlew build`, `./gradlew jar`, `./gradlew remapJar`, etc.)
- Maven builds (e.g., `mvn package`, `mvn compile`, etc.)
- Node / Web builds (e.g., `npm run build`, `pnpm build`, `yarn build`)
- Other language build tools (e.g., `cargo build`, `go build`, `make`)

## Guidelines
1. **Always Ask First**: Before executing any build command on behalf of the user, explicitly ask the user for confirmation.
2. **Offer the Command**: When files are ready to be built, present the exact command to the user so they have the choice to run it themselves or ask the agent to run it.
3. **Non-Build Commands Allowed**: File reading/writing, code editing, git inspection, directory listings, and static checks do not require build approval unless they trigger a full build task.
