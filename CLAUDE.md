# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A Java 17 wrapping of OpenCV (currently 4.13.0) with DICOM-oriented additions (window/level LUTs, segmentation primitives, raw image I/O). Consumed downstream by Weasis. Packaged as an OSGi bundle that embeds the OpenCV Java classes and exports them — consumers do not need a separate OpenCV dependency.

## Build & test

Prerequisites: JDK 17+, Maven 3.6.3+.

- Full build (from repo root): `mvn clean install`
- Build/test the bundle module only: `mvn -f weasis-core-img clean install`
- Tests only: `mvn -f weasis-core-img test`
- Single test class / method: `mvn -f weasis-core-img test -Dtest=ImageProcessorTest` or `-Dtest=ImageProcessorTest#methodName`
- Coverage report (JaCoCo, used by CI/SonarCloud): `mvn -Pcoverage -f weasis-core-img verify`
- Apply formatting (**before committing only**, not as part of routine edits): `mvn -f weasis-core-img spotless:apply` (google-java-format + EPL-2.0/Apache-2.0 license header). Spotless is configured but not bound to a phase, so it is run explicitly and only when preparing a commit.

Tests require the OpenCV native library. Maven unpacks it into `weasis-core-img/target/lib/<os-name>-<cpu-name>/` during `generate-test-resources` and Surefire is invoked with `-Djava.library.path` pointing there (see `argLine` in `weasis-core-img/pom.xml`). The active OS/arch profile in the root `pom.xml` selects which native classifier is unpacked, so a plain `mvn test` only works on linux-x86_64/aarch64, macosx-x86_64/aarch64, or windows-x86_64.

## Project layout

Two-module Maven build:

- Root `pom.xml` — BOM (`weasis-core-img-bom`) that pins `weasis.opencv.version` and declares all five native-library classifiers (linux-aarch64, linux-x86-64, macosx-aarch64, macosx-x86-64, windows-x86-64). Version is computed as `${weasis.opencv.version}.1` so the artifact version tracks the underlying OpenCV native release.
- `weasis-core-img/` — the actual library (packaging `bundle`, the OSGi bundle plugin embeds dependencies inline under `lib/`).

Source under `weasis-core-img/src/main/java`:

- `org.weasis.core.util` — generic utilities (file/string/math/stream/zip helpers, `Pair`/`Triple`, `SoftHashMap`).
- `org.weasis.opencv.natives.NativeLibrary` — resolves `os.name`/`os.arch` to an OSGi-style `<os>-<arch>` spec and loads `libopencv_java` either by absolute path or by library name. Thread-safe with double-checked init; once loaded, repeat calls are no-ops.
- `org.weasis.opencv.data` — image data types. `PlanarImage` is the central `AutoCloseable` abstraction over an OpenCV `Mat`; `ImageCV` is the canonical implementation (`extends Mat implements PlanarImage`) and adds explicit release tracking. `FileRawImage` is a `Path`-backed record for the project's raw image format (46-byte header). `LookupTableCV`, `MetadataParser`, `ImageSize`.
- `org.weasis.opencv.op` — image operations. `ImageProcessor` is a **deprecated** facade kept for backward compatibility; new code should use the split classes: `ImageAnalyzer` (stats/measurements), `ImageTransformer` (geometric/visual ops), `ImageIOHandler` (read/write/thumbnails). Plus `ImageConversion`, `ImageContentHash`.
- `org.weasis.opencv.op.lut` — DICOM window/level + presentation LUT (`ByteLut`, `ColorLut`, `LutShape`, `LutParameters`, `WlParams`, `WlPresentation`, `DefaultWlPresentation`, `PresentationStateLut`).
- `org.weasis.opencv.seg` — segmentation primitives (`Region`, `Segment`, `ContourTopology`, `RegionAttributes`).

## Build-time mechanics worth knowing

- `module-info.java` declares JPMS module `org.weasis.core.img` and re-exports a curated subset of OpenCV packages (`org.opencv.core`, `imgcodecs`, `imgproc`, `img_hash`, `osgi`, `utils`). The OpenCV Java sources themselves are pulled at build time from the `opencv:sources` artifact, unpacked into `target/sources-import`, and added as an extra source root via `build-helper-maven-plugin` — they are not checked into this repo.
- All OpenCV native dependencies are `provided` scope. Downstream consumers (Weasis) are expected to ship a native library alongside their app and load it via `NativeLibrary.loadLibraryFromAbsolutePath(...)` or `NativeLibrary.loadLibraryFromLibraryName()` after setting `-Djava.library.path`.
- `flatten-maven-plugin` produces `.flattened-pom.xml` during `process-resources` (BOM mode at root, ossrh mode in the child). Don't hand-edit `.flattened-pom.xml`.
- CI (`.github/workflows/maven.yml`) runs `mvn -Pcoverage -f weasis-core-img -B verify ...sonar` on Ubuntu only, so SonarCloud coverage reflects the linux-x86-64 native path.

## Conventions

- Formatter: google-java-format via Spotless. License header (EPL-2.0 OR Apache-2.0) is also enforced by Spotless — keep it on new files.
- **Javadoc**: keep it minimal. **Private methods**: one-line comment max, or none if the name and signature are self-explanatory. **Public/protected API**: as compact as possible — one short sentence describing intent, `@param` / `@return` / `@throws` only when they add information the signature doesn't already convey. Never restate the method name in prose, never document obvious getters/setters, never leave `TODO`-style placeholders.
- **Code quality**: favor readability and maintainability — small focused methods, expressive names, early returns. Remove redundant code (unused imports/locals, dead branches, duplicated logic, defensive null checks for values that cannot be null, comments that duplicate the code). Prefer extracting a private helper over copy-pasting a block.
- **Tests**: use **JUnit 6** (`org.junit.jupiter.*`) and **Mockito** only. Do **not** add AssertJ (`org.assertj.*`) — use JUnit's built-in `Assertions` (`assertEquals`, `assertThrows`, `assertAll`, …) for assertions. Tests follow the same Spotless / formatting rules as production code.
