# Project rules: cpt-lab1-krushelnytskyi

Java 21 console application, domain "Electronics store".
Record: `name;category;price;warrantyMonths;stock` (String;String;double;int;int),
separator `;`, UTF-8, no header line and no comments in the CSV.
Metrics: count of valid records, average price, longest warranty,
total stock (sum of `stock`, type long).

## Language
- Code, javadoc, docs, commits, issues, PRs, logs: English.
- Program report and skipped-line messages: Ukrainian, money in "грн".
- REPORT.md is written in Ukrainian; do not edit it unless asked.

## Build
- Maven only through the Wrapper: `./mvnw -B clean verify` (Git Bash/Unix), `.\mvnw.cmd -B clean verify` (cmd).
- JUnit 5 (test scope), SpotBugs bound to `verify`, Maven Shade for the executable JAR.
- No runtime dependencies. Package `ua.lpnu.kzp`, entry point `Main`.
- Lifecycle order: compile, test, package, verify.

## Hard rules
- No entity classes (that is lab 2): records are String[] and primitives. Reading, validation,
  calculation and formatting are separate methods or utility classes.
- Use `Path`, explicit UTF-8, `Locale.ROOT`, `%n`. No hard-coded path separators.
- Validation: trim fields; name and category not blank; numbers must match `-?\d+(\.\d+)?`
  (no exponent, no f/d suffix, no NaN/Infinity); price > 0; warrantyMonths >= 0; stock >= 0;
  int overflow is rejected.
- Every invalid line (including an empty one) yields a message with line number and reason
  and never stops processing. No valid records: report says so, metrics are not computed,
  exit code 1. Argument and I/O errors: exit code 2.
- Report numbers: 2 decimals, locale-independent.
- Logging: English, `java.util.logging`, file `out/app.log`, `--verbose` mirrors to stderr.
  Format: `ISO-8601 time | LEVEL | Class.method | line=N field=X | message`.
  Log startup, arguments, every skipped line with reason, summary, exceptions with stack traces.
- Never disable SpotBugs rules; `@SuppressFBWarnings` only with a justification in the PR.
- Do not commit `target/` or `out/`.
- Javadoc for public classes, `main` and non-trivial methods.
- Docs: concise and factual, no emoji, no filler.

## Workflow
- Tasks live in GitHub Issues. Before starting, read the Issue with `gh issue view <N>` and follow its acceptance criteria.
- One PR covers a small group of related Issues; branch `<type>/<N>-<M>-<slug>`, one logical change per commit. Stay within the scope of the listed Issues.
- Write tests from the Issue acceptance criteria first, then the implementation.
- `clean verify` must be green before finishing a step.
- Conventional Commits (`feat(parser): ...`, `test:`, `ci:`, `build:`, `docs:`, `fix:`),
  one logical change per commit. PR body contains `Closes #N`.
- Show the diff and explain each change briefly. Do not push without confirmation.
