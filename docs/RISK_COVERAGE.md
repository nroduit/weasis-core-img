# Test coverage matrix — weasis-core-img

This document is a developer-facing index of which automated tests in this
repository exercise which categories of risk. It is **not** a certification
artefact, a conformity claim, or a substitute for any regulated process.
Readers integrating `weasis-core-img` into a regulated product are
responsible for their own software-requirement traceability and verification
evidence under the framework that applies to them
(e.g. ISO 13485 / ISO 14971 / IEC 62304 in a medical-device context).

> Last updated: 2026-05-21
> Test count at that date: 1510 automated tests, all passing on the active
> build OS/arch profile.
> Coverage tooling: JaCoCo via `mvn -Pcoverage verify`. SonarCloud reports
> the Linux-x86-64 native path only.

## 1. Scope

`weasis-core-img` is a Java 17 library that wraps OpenCV (loaded via JNI) and
adds DICOM-oriented operations: window/level lookup tables, segmentation
primitives, and a raw-image I/O format. The risk-relevant axes for a library
of this shape are native interop, resource lifecycle around `Mat`-backed
buffers, and pixel-exact data integrity of the lossless I/O path.

The build expects a per-platform native OpenCV library to be supplied at
runtime by the consumer via `-Djava.library.path`. The test harness unpacks
the matching native artefact into `target/lib/<os>-<arch>/` for Surefire.

## 2. Coverage matrix

The table maps a risk category to the production classes that own it and the
test classes that exercise it. The "Tests exercise" column is descriptive —
it states what is tested, not how good the coverage is.

| # | Risk category | Production class(es) | Test class(es) | Tests exercise |
|---|---------------|----------------------|----------------|----------------|
| R-01 | Input validation (null/empty/blank on public surface) | `EscapeChars`, `StringUtil`, `FileUtil`, `ZipUtil`, `FileRawImage`, `ImageIOHandler` | `EscapeCharsTest`, `StringUtilTest`, `FileUtilTest`, `ZipUtilTest`, `FileRawImageTest`, `ImageIOHandlerTest` | `@NullAndEmptySource` on public string APIs; explicit `NullPointerException` checks at `Objects.requireNonNull` boundaries. |
| R-02 | Path traversal / Zip-slip | `ZipUtil.resolveAndValidateEntryPath` | `ZipUtilTest$Security_Tests` | Relative and absolute traversal attempts. |
| R-03 | Zip-bomb (decompression ratio) | `ZipUtil.checkEntry`, threshold `ZipUtil.MAX_COMPRESSION_RATIO` | `ZipUtilTest$Security_Tests` | Above the threshold, at the boundary, and the unknown-size (`-1`) path. |
| R-04 | XSS / output encoding | `EscapeChars` | `EscapeCharsTest` | XML 1.0 boundary characters, Unicode surrogates, identity preservation for inputs that don't need escaping. |
| R-05 | Filename sanitisation | `FileUtil.getValidFileName`, `getValidFileNameWithoutHTML` | `FileUtilTest` | Illegal-character filtering across the full ASCII / Latin-1 range. |
| R-06 | Resource lifecycle (single thread) | `ImageCV.release/close`, `FileUtil` stream paths | `ImageCVTest$Resource_Management`, `FileUtilTest` | Idempotency of `release()` and `close()`; try-with-resources auto-close. |
| R-07 | Resource lifecycle under concurrent release | `ImageCV.release/close` (`synchronized`, `volatile` flags) | `ImageCVTest$Resource_Management` | 32-thread close/release race against a single `ImageCV`; observable `released` flag after the race. |
| R-08 | Concurrent access to caching helpers | `SoftHashMap` (documented "not thread-safe") | `SoftHashMapTest` | Single-threaded behaviour only — matches the documented contract. |
| R-09 | Platform / native-spec detection | `NativeLibrary` | `NativeLibraryTest` | 36 parameterised cases across OS/arch combinations; concurrent library-load. |
| R-10 | Native interop failure modes | `ImageIOHandler.readImage` (downgrades `CvException` / `OutOfMemoryError` to `null`) | `ImageIOHandlerTest$Read_Image_Failure_Contract` | Corrupted file, unrecognised format, empty file, and the throwing variant. |
| R-11 | Pixel-exact data integrity (lossless I/O round-trip) | `ImageIOHandler.writePNG` / `readImage`, `ImageConversion` | `ImageIOHandlerTest$Round_Trip_Integrity` | Bit-exact PNG round-trip for `CV_8UC1`, `CV_8UC3`, `CV_16UC1`. |
| R-12 | Numerical correctness | `ImageAnalyzer`, `LookupTableCV`, `MathUtil`, `ImageContentHash` | `ImageAnalyzerTest`, `LookupTableCVTest`, `MathUtilTest`, `ImageContentHashTest` | Epsilon-based float comparisons; LUT direct lookup; identical-image hash equality. |
| R-13 | Determinism / reproducibility | `ImageContentHash`, `ZipUtil` (sorted entries) | `ImageContentHashTest`, `ZipUtilTest` | Single-JVM determinism; consistent zip entry ordering. |
| R-14 | Error-handling contract (typed exceptions, messages) | I/O classes | `ZipUtilTest`, `ImageIOHandlerTest`, `FileUtilTest` | Exception types pinned; specific message substrings asserted where they are part of the API contract. |

## 3. Changes recorded in this revision (2026-05-21)

Test and production-code changes made in this revision, by risk category:

1. **R-07** — `ImageCV.released` and `ImageCV.releasedAfterProcessing` are now
   `volatile`; `release()` is `synchronized`. A concurrent-close test was
   added in `ImageCVTest`.
2. **R-03** — `ZipUtil.checkEntry` and the `MAX_COMPRESSION_RATIO` constant
   were made package-visible so the threshold is directly testable. Three
   new boundary tests were added in `ZipUtilTest`.
3. **R-11** — Three bit-exact PNG round-trip tests were added in
   `ImageIOHandlerTest$Round_Trip_Integrity` for `CV_8UC1`, `CV_8UC3`,
   `CV_16UC1`.
4. **R-10** — Four new failure-path tests were added in
   `ImageIOHandlerTest$Read_Image_Failure_Contract` covering corrupted,
   unrecognised, throw-variant, and zero-byte inputs.
5. **Deprecated API removal** — `ImageProcessor` (a deprecated facade) was
   first pinned with six equality tests, then removed entirely along with
   every other member marked `@Deprecated(since = "4.12", forRemoval = true)`.
   See §3.1 for the migration matrix.

Test count: 1508 → 1510 (+17 added, −15 retired with the removed
`ImageProcessorTest`). The OSGi bundle still installs cleanly via
`mvn install`.

### 3.1 Deprecated-API removal — migration matrix

All members previously marked `@Deprecated(forRemoval = true)` are gone in
this revision. Code that consumed them must migrate as follows:

| Removed (was) | Use instead |
|---------------|-------------|
| `ImageProcessor` (whole class) — facade for image ops | `ImageAnalyzer` (statistics), `ImageTransformer` (geometric/visual ops), `ImageIOHandler` (I/O / thumbnails) |
| `ImageCV.isHasBeenReleased()` | `ImageCV.isReleased()` |
| `ImageCV.toImageCV(Mat)` static | `ImageCV.fromMat(Mat)` |
| `PlanarImage.isHasBeenReleased()` (interface method) | `PlanarImage.isReleased()` |
| `FileUtil.safeClose(...)` (3 overloads) | `StreamUtil.safeClose(...)` |
| `FileUtil.nioWriteFile`, `nioCopyFile` | `StreamUtil.copyWithNIO` / `StreamUtil.copyFile` |
| `FileUtil.readProperties(File, Properties)`, `storeProperties(File, ...)` | `PropertiesUtil.loadProperties(Path, ...)` / `PropertiesUtil.storeProperties(Path, ...)` |
| `FileUtil.zip(File, File)`, `unzip(InputStream, File)` | `ZipUtil.zip(Path, Path)` / `ZipUtil.unzip(InputStream, Path)` |
| `FileUtil.createTempDir(File)` | `FileUtil.createTempDir(Path)` |
| `FileUtil.getAllFilesInDirectory(File, List<File>, ...)` | `FileUtil.getAllFilesInDirectory(Path, List<Path>, ...)` |
| `FileUtil.delete(File)`, `deleteDirectoryContents(File, ...)`, `recursiveDelete(File, ...)` | their `Path` overloads |
| `FileUtil.prepareToWriteFile(File)`, `isFileExtensionMatching(File, ...)`, `writeStream(..., File, ...)`, `writeStreamWithIOException(..., File)`, `writeFile(..., File)` | corresponding `Path` overloads |
| `LangUtil.getNULLtoFalse / getNULLtoTrue` | `LangUtil.nullToFalse / nullToTrue` |
| `LangUtil.getEmptytoFalse / geEmptytoTrue` | `LangUtil.emptyToFalse / emptyToTrue` |
| `LangUtil.getOptionalDouble / getOptionalInteger` | `LangUtil.toOptional(Double)` / `LangUtil.toOptional(Integer)` |

The non-deprecated APIs are exercised directly by `ImageAnalyzerTest`,
`ImageTransformerTest`, `ImageIOHandlerTest`, `FileUtilTest`, `LangUtilTest`
and `ZipUtilTest`.

## 4. Items not covered at unit level

The following are intentionally out of scope for unit tests in this
repository. Consumers should decide whether they need to address them in
their own integration / system test layer.

- **R-08, `SoftHashMap` under contention.** The class is documented as not
  thread-safe; the unit tests reflect that contract.
- **R-13, cross-JVM / cross-platform hash determinism.** CI runs on one
  OS/arch per invocation. Cross-platform reproducibility belongs in a
  consumer's compatibility matrix.
- **R-12, exotic numeric edge cases.** `MathUtil` could be extended with
  `NaN` / `Infinity` / `MIN_VALUE` / `MAX_VALUE` cases if a downstream caller
  relies on them. Current internal usage does not.
- **Non-PNG round-trips.** Only PNG is asserted bit-exact in R-11. JPEG /
  TIFF / JP2 paths depend on the OpenCV codec implementation; assessing that
  external dependency is the consumer's responsibility.

## 5. Test-suite consistency observations

- All 28 test classes share the same conventions:
  `@DisplayNameGeneration(ReplaceUnderscores.class)`, `@Nested` for behavioural
  grouping, `assertAll` for multi-assertion atomicity, `@ParameterizedTest`
  with `@NullAndEmptySource` on string-accepting public APIs.
- One wall-clock latency assertion exists (`EscapeCharsTest`, `< 200ms`).
  Performance assertions are sensitive to shared-CI load and would ideally
  live in a separately tagged suite.
- A few structural-only assertions remain
  (`assertTrue(result.length >= N)`). These could be tightened to exact
  equality where the specification is deterministic.

## 6. Optional traceability convention

If you maintain a software-requirements or hazard register in your own
project that consumes this library, you can tag tests with
`@Tag("SR-xxx")` (or your own ID scheme). SonarCloud and Surefire XML both
surface JUnit tags, which lets a reviewer cross-reference requirement IDs
to test methods without restructuring the suite.

## 7. Running the verification locally

```
mvn -f weasis-core-img -B test                      # full suite
mvn -f weasis-core-img -B -Pcoverage verify         # with JaCoCo report
mvn -f weasis-core-img test -Dtest=ImageCVTest      # single class
mvn -f weasis-core-img test -Dtest=ImageIOHandlerTest$Round_Trip_Integrity
```

The native OpenCV library is required and is unpacked automatically by the
build to `weasis-core-img/target/lib/<os-name>-<cpu-name>/`.