# AGENTS.md

Guidance for AI coding agents and human contributors working in this repository.
The nearest `AGENTS.md` up the directory tree applies.

## Project overview

A Java 17 wrapping of OpenCV with DICOM-oriented additions (window/level LUTs, segmentation primitives, raw image I/O), used downstream by [Weasis](https://github.com/nroduit/Weasis) and [weasis-dicom-tools](https://github.com/nroduit/weasis-dicom-tools).
It is packaged as an OSGi bundle that embeds the OpenCV Java classes and exports them, so consumers need no separate OpenCV dependency.
The OpenCV version is `weasis.opencv.version` in the root `pom.xml`.
It requires **JDK 17+** (`java-version` in `weasis-core-img/pom.xml`) and Maven **3.6.3+** (enforced).

## Commands

```bash
mvn clean install                                           # full build, from the repository root
mvn -f weasis-core-img clean install                        # the bundle module only
mvn -f weasis-core-img test                                 # tests only
mvn -f weasis-core-img test -Dtest=ImageProcessorTest#methodName   # one test class or method
mvn -Pcoverage -f weasis-core-img verify                    # JaCoCo coverage, as CI and SonarCloud
mvn -f weasis-core-img spotless:apply                       # format and license header, before committing
```

Spotless is configured but not bound to a phase: run it explicitly when preparing a commit, not as part of routine edits.

Tests need the OpenCV native library.
Maven unpacks it into `weasis-core-img/target/lib/<os-name>-<cpu-name>/` during `generate-test-resources`, and Surefire runs with `-Djava.library.path` pointing there (see `argLine` in `weasis-core-img/pom.xml`).
The active OS and architecture profile of the root `pom.xml` selects the native classifier, so a plain `mvn test` works only on linux-x86_64/aarch64, macosx-x86_64/aarch64 or windows-x86_64.

## Definition of done

A change is complete when:

- `mvn -f weasis-core-img spotless:apply` has been run and `mvn -f weasis-core-img verify` passes.
- New behavior has tests, and a fixed bug has a test that failed before the fix.
- The build adds no new compiler or Sonar warning in the changed code.
- `docs/RISK_COVERAGE.md` is updated when a change adds or removes tests for a risk category it lists.
- The description of the change says how it was verified.

## Project layout

A two-module Maven build:

- Root `pom.xml`: the BOM (`weasis-core-img-bom`) that pins `weasis.opencv.version` and declares the five native library classifiers (linux-aarch64, linux-x86-64, macosx-aarch64, macosx-x86-64, windows-x86-64).
  The version is `${weasis.core.version}.1`, so the artifact version tracks the OpenCV native release.
- `weasis-core-img/`: the library itself (packaging `bundle`; the OSGi bundle plugin embeds dependencies inline under `lib/`).

Packages under `weasis-core-img/src/main/java`:

- `org.weasis.core.util`: generic utilities (file, string, math, stream and zip helpers, `Pair` / `Triple`, `SoftHashMap`).
- `org.weasis.opencv.natives.NativeLibrary`: resolves `os.name` / `os.arch` to an OSGi-style `<os>-<arch>` spec and loads `libopencv_java` by absolute path or by library name.
  It is thread-safe with a double-checked initialization; once loaded, further calls do nothing.
- `org.weasis.opencv.data`: image data types.
  `PlanarImage` is the central `AutoCloseable` abstraction over an OpenCV `Mat`; `ImageCV` is its canonical implementation (`extends Mat implements PlanarImage`) and adds explicit release tracking.
  `FileRawImage` is a `Path`-backed record for the raw image format of the project (46-byte header).
  Also `LookupTableCV`, `MetadataParser`, `ImageSize`.
- `org.weasis.opencv.op`: image operations.
  `ImageProcessor` is a **deprecated** facade kept for backward compatibility; new code uses `ImageAnalyzer` (statistics and measurements), `ImageTransformer` (geometric and visual operations) and `ImageIOHandler` (read, write, thumbnails), plus `ImageConversion` and `ImageContentHash`.
- `org.weasis.opencv.op.lut`: DICOM window/level and presentation LUT (`ByteLut`, `ColorLut`, `LutShape`, `LutParameters`, `WlParams`, `WlPresentation`, `DefaultWlPresentation`, `PresentationStateLut`).
- `org.weasis.opencv.seg`: segmentation primitives (`Region`, `Segment`, `ContourTopology`, `RegionAttributes`).

### Build mechanics

- `module-info.java` declares the JPMS module `org.weasis.core.img` and re-exports a curated subset of OpenCV packages (`org.opencv.core`, `imgcodecs`, `imgproc`, `img_hash`, `osgi`, `utils`).
  The OpenCV Java sources come at build time from the `opencv:sources` artifact, unpacked into `target/sources-import` and added as a source root by `build-helper-maven-plugin`; they are not in this repository.
- All OpenCV native dependencies have `provided` scope.
  Consumers ship a native library with their application and load it through `NativeLibrary.loadLibraryFromAbsolutePath(...)` or `NativeLibrary.loadLibraryFromLibraryName()` after setting `-Djava.library.path`.
- `flatten-maven-plugin` produces `.flattened-pom.xml` during `process-resources` (BOM mode at the root, ossrh mode in the child).
- CI (`.github/workflows/maven.yml`) runs `mvn -Pcoverage -f weasis-core-img -B verify` on Ubuntu only, so SonarCloud coverage reflects the linux-x86-64 native path.

## Code conventions

- **Formatting and headers**: google-java-format through Spotless, which also enforces the EPL-2.0 OR Apache-2.0 license header; keep it on new files.
- **Native resources**: every `Mat` and `PlanarImage` is released deterministically, with try-with-resources or an explicit `release()`; a leaked native buffer is not collected in time by the garbage collector.
  Never assume a native library is loaded outside the paths `NativeLibrary` controls.
- **Pixel exactness**: lossless operations must stay bit-exact; a change to rounding, depth conversion or channel order needs a test that pins the expected pixel values.
- **Logging**: use SLF4J with `{}` placeholders, never `System.out`, `System.err` or `printStackTrace()` in library code (a `main` diagnostic entry point is the exception).
  Pass the exception as the last argument so its stack trace is kept.
- **Javadoc**: keep it minimal.
  A private method gets at most a one-line comment, or none when its name and signature are clear.
  A public or protected API gets one short sentence on its intent, with `@param` / `@return` / `@throws` only when they add what the signature does not say.
  Never restate the method name, never document obvious getters and setters, never leave `TODO` placeholders.
- **Code quality**: small focused methods, expressive names, early returns.
  Remove redundant code: unused imports and locals, dead branches, duplicated logic, null checks for values that cannot be null, comments that repeat the code.
  Extract a private helper rather than copy a block.
- **Cross-platform**: the library runs on Windows, macOS and Linux; build paths with `Path` and pass charsets explicitly.

## Testing

- Use **JUnit 6** (`org.junit.jupiter.*`) and **Mockito** only.
  Do not add AssertJ (`org.assertj.*`): use JUnit's `Assertions` (`assertEquals`, `assertThrows`, `assertAll`, …).
- Tests mirror the main packages under `weasis-core-img/src/test/java`; resources go in `src/test/resources`.
- Tests write files only under a `@TempDir`, need no network, and restore any JVM-global state they change (default `Locale`, `TimeZone`, system properties).
- Tests follow the same Spotless rules as production code.

## Writing conventions

These rules apply to Markdown docs, Javadoc, comments and commit messages.

- **American English**: center, color, gray, behavior, normalize, initialize, artifact, license (noun).
  OpenCV and DICOM names keep their own spelling.
- **Markdown**:
  - One sentence per line, so that diffs show the changed sentence only.
  - A blank line before and after headings, lists, tables, code blocks and quotes; never more than one blank line.
  - A hard line break is a trailing `\`, never two trailing spaces.
  - CommonMark with GFM tables and fenced code blocks that name their language (`java`, `bash`, `text`, …); no raw HTML.
  - Relative links between files in the repository.
- `.editorconfig` sets UTF-8, LF and a final newline; `.markdownlint.yaml` holds the Markdown rules a linter can check.

## Git and commits

- One logical change per commit; never mix formatting or refactoring with a functional change.
- Subject line: a type prefix (`feat:`, `fix:`, `build:`, `ci:`, `docs:`, `test:`, `refactor:`) and an imperative summary without a trailing period, at most about 72 characters (for example `fix: correct shape masks, BGR order, PNG depth and native Mat releases`).
- A body, after a blank line, explains why when the subject is not enough.
- Never push, force-push, rewrite published history or create a release unless explicitly asked.

## Boundaries

### Never

- Commit patient data: no real DICOM-derived images or fixtures containing identifiers; keep test images small and synthetic.
- Commit the OpenCV Java sources or native libraries, or edit generated content (`target/`, `.flattened-pom.xml`).
- Disable or skip Spotless, tests or the enforcer to make a build pass.

### Ask first

- Adding, upgrading or removing a dependency or a Maven plugin, or changing `weasis.opencv.version`.
  A new dependency must have a license compatible with EPL-2.0 OR Apache-2.0, and downstream OSGi consumers have to ship it.
- Changing or removing a public API: Weasis and weasis-dicom-tools use it.
  Deprecate with `@Deprecated` rather than remove, as for `ImageProcessor`.
- Changing the exported packages in `module-info.java` or the bundle manifest.
- Changing the CI or release workflows.
