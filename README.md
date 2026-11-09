# weasis-core-img

[![License](https://img.shields.io/badge/License-EPL%202.0-blue.svg)](https://opensource.org/licenses/EPL-2.0) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) ![Maven build](https://github.com/nroduit/weasis-core-img/workflows/Build/badge.svg?branch=master)

[![Sonar](https://sonarcloud.io/api/project_badges/measure?project=weasis-core-img&metric=ncloc)](https://sonarcloud.io/component_measures?id=weasis-core-img) [![Sonar](https://sonarcloud.io/api/project_badges/measure?project=weasis-core-img&metric=reliability_rating)](https://sonarcloud.io/component_measures?id=weasis-core-img) [![Sonar](https://sonarcloud.io/api/project_badges/measure?project=weasis-core-img&metric=sqale_rating)](https://sonarcloud.io/component_measures?id=weasis-core-img) [![Sonar](https://sonarcloud.io/api/project_badges/measure?project=weasis-core-img&metric=security_rating)](https://sonarcloud.io/component_measures?id=weasis-core-img) [![Sonar](https://sonarcloud.io/api/project_badges/measure?project=weasis-core-img&metric=alert_status)](https://sonarcloud.io/dashboard?id=weasis-core-img) [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=weasis-core-img&metric=coverage)](https://sonarcloud.io/summary/new_code?id=weasis-core-img)

A Java wrapper around [OpenCV](https://opencv.org/) with additions oriented toward medical imaging (window/level LUTs, segmentation primitives, raw image I/O).
Packaged as an OSGi bundle that embeds the OpenCV Java classes and re-exports them, so consumers do not need a separate OpenCV dependency on the classpath.
Used downstream by [Weasis](https://github.com/nroduit/Weasis).

## Features

- Java 17 module (`org.weasis.core.img`) re-exporting `org.opencv.core`, `geometry`, `imgcodecs`, `imgproc`, `img_hash`, `osgi`, `utils`.
- `PlanarImage` / `ImageCV` — `AutoCloseable` abstraction over an OpenCV `Mat` with explicit release tracking.
- Image operations split into focused entry points: `ImageAnalyzer` (stats and measurements), `ImageTransformer` (geometric and visual operations), `ImageIOHandler` (read/write/thumbnails).
- DICOM-oriented LUTs and presentation state helpers (`ByteLut`, `ColorLut`, `LutShape`, `LutParameters`, `WlParams`, `WlPresentation`, `DefaultWlPresentation`, `PresentationStateLut`).
- Color maps for pseudo-color and volume rendering (`ColorMap`, `ColorMapCompiler`, `ColorMapSampler`, `GradientOpacity`, `Material`).
- Segmentation primitives (`Region`, `Segment`, `ContourTopology`, `RegionAttributes`).
- DICOM waveform readers and the T.261 streaming codec of Supplement 253 (`WaveformReader`, `NativeWaveformReader`, `T261WaveformReader`, `T261WaveformWriter`, `T261Parameters`).
- Raw image format with a 46-byte header (`FileRawImage`) and OpenCV-backed I/O.
- Native-library resolver (`NativeLibrary`) that maps `os.name` / `os.arch` to an OSGi-style `<os>-<arch>` spec and loads `libopencv_java` from an absolute path or by library name.

## Requirements

- JDK 17 or later
- Maven 3.6.3 or later
- One of the supported OS/architecture pairs for tests: `linux-x86-64`, `linux-aarch64`, `macosx-x86-64`, `macosx-aarch64`, `windows-x86-64`.

## Build

From the repository root:

```bash
mvn clean install
```

Build and test the bundle module only:

```bash
mvn -f weasis-core-img clean install
```

Run tests:

```bash
mvn -f weasis-core-img test
```

Run a single test class or method:

```bash
mvn -f weasis-core-img test -Dtest=ImageCVTest
mvn -f weasis-core-img test -Dtest=ImageCVTest#methodName
```

Produce a coverage report (JaCoCo, used by CI and SonarCloud):

```bash
mvn -Pcoverage -f weasis-core-img verify
```

Apply formatting before committing (google-java-format + EPL-2.0 / Apache-2.0 license header):

```bash
mvn -f weasis-core-img spotless:apply
```

Spotless is not bound to a build phase, so it only runs when invoked explicitly.

## Native library

All OpenCV native dependencies are declared with `provided` scope.
A downstream application is expected to ship the matching native library and load it at runtime by either:

- setting `-Djava.library.path="path/of/native/lib"` and calling `NativeLibrary.loadLibraryFromLibraryName()`, or
- calling `NativeLibrary.loadLibraryFromAbsolutePath(path)` with the absolute path to `libopencv_java`.

During tests, Maven unpacks the native library into `weasis-core-img/target/lib/<os-name>-<cpu-name>/` and Surefire is configured to point `-Djava.library.path` there.
The OS/arch profile in the root `pom.xml` selects which native classifier is unpacked.

Additional native binaries (other systems and architectures) are published in [this Maven repository](https://github.com/nroduit/mvn-repo/tree/master/org/weasis/thirdparty/org/opencv/libopencv_java).

### Platform support and limitations

The native library is a JNI build of OpenCV restricted to the modules used by this project (`core`, `imgproc`, `imgcodecs`, `img_hash`, `java` and the modules they depend on).
It is CPU-only: consumers drive it through CPU-side `Mat` (no `UMat` / OpenCL / GPU path).
The following classifiers are published:

| Classifier | File | Build | Runs on |
|---|---|---|---|
| `linux-x86-64` | `libopencv_java.so` | GCC, against glibc; C++ runtime linked statically | glibc-based Linux at or above the glibc floor of the library |
| `linux-aarch64` | `libopencv_java.so` | GCC (cross), against glibc; C++ runtime linked statically | glibc-based Linux at or above the glibc floor of the library |
| `macosx-x86-64` | `libopencv_java.dylib` | Apple Clang, with a macOS deployment target | Intel macOS at or above the deployment target |
| `macosx-aarch64` | `libopencv_java.dylib` | Apple Clang, with a macOS deployment target | Apple Silicon macOS at or above the deployment target |
| `windows-x86-64` | `opencv_java.dll` | MSVC, static C and C++ runtime | 64-bit Windows 10 and later, no Visual C++ redistributable needed |

The Linux libraries load the C library of the system (`libc`, `libm`, `libdl`, `libpthread`) and nothing else: `libstdc++` and `libgcc` are linked in statically.
Their glibc floor is the highest symbol version they reference, set by the build toolchain; they do not load on a musl system such as Alpine.
A linker version script exports only the JNI symbols (`Java_*`, `JNI_*`), so the bundled C++ runtime stays isolated when the library is loaded in the same JVM as another native library that ships its own `libstdc++`.
The macOS libraries link only the system `libc++` and frameworks; the Windows library imports only system DLLs.

Check the requirements of a given library:

```bash
objdump -T libopencv_java.so | grep -oE 'GLIBC_[0-9.]+' | sort -uV | tail -1   # Linux: glibc floor
vtool -show-build libopencv_java.dylib                                          # macOS: minos
strings libopencv_java.so | grep -A60 'General configuration'                   # build: compiler, modules, SIMD
```

SIMD code has a baseline that the CPU must support and faster paths chosen at run time (both are listed in the build configuration above):

- x86-64: the baseline is SSE4.2 with POPCNT (x86-64-v2), and AVX, AVX2 and AVX-512 paths are dispatched.
- aarch64: the baseline is NEON, and the newer extensions (FP16, dot product, BF16) are dispatched or part of the baseline depending on the platform.

Limitations to be aware of:

- 32-bit and other architectures (for example `linux-armv7a`, `linux-x86`) are not shipped; they can be built from source if a consumer targets them.
- Each platform must use its matching classifier: the architecture and OS of the native library have to agree with the running JVM.

## Use as a dependency

The artifact version is the version of the underlying OpenCV release (`weasis.opencv.version` in the root `pom.xml`); a fourth number marks a revision of the Java code on the same OpenCV release.
The native libraries carry the OpenCV version with a `-dcm` suffix.

```xml
<dependency>
  <groupId>org.weasis.core</groupId>
  <artifactId>weasis-core-img</artifactId>
  <version>${weasis.core.img.version}</version>
</dependency>
```

A matching native classifier must be added separately, for example for Linux x86-64:

```xml
<dependency>
  <groupId>org.weasis.thirdparty.org.opencv</groupId>
  <artifactId>libopencv_java</artifactId>
  <version>${weasis.opencv.native.version}</version> <!-- for example 5.1.0-dcm -->
  <type>so</type>
  <classifier>linux-x86-64</classifier>
</dependency>
```

Other classifiers: `linux-aarch64`, `macosx-x86-64` / `macosx-aarch64` (type `dylib`), `windows-x86-64` (artifactId `opencv_java`, type `dll`).

## Project layout

Two-module Maven build:

- Root `pom.xml` — BOM (`weasis-core-img-bom`) pinning `weasis.opencv.version` and declaring all supported native classifiers.
- `weasis-core-img/` — the library itself (packaging `bundle`; the OSGI bundle plugin embeds dependencies inline under `lib/`).

Main source roots under `weasis-core-img/src/main/java`:

- `org.weasis.core.util` — generic helpers (file, string, math, stream, zip, `Pair` / `Triple`, `SoftHashMap`); `org.weasis.core.util.annotations` holds the `Generated` marker.
- `org.weasis.opencv.natives` — native-library loader.
- `org.weasis.opencv.data` — image data types (`PlanarImage`, `ImageCV`, `FileRawImage`, `LookupTableCV`, `MetadataParser`, `ImageSize`).
- `org.weasis.opencv.op` — image operations (`ImageAnalyzer`, `ImageTransformer`, `ImageIOHandler`, `ImageConversion`, `ImageContentHash`).
  The former `ImageProcessor` facade was removed after its deprecation; see `docs/RISK_COVERAGE.md` for the migration matrix.
- `org.weasis.opencv.op.lut` — window/level and presentation LUTs; `org.weasis.opencv.op.lut.colormap` — color map model, compiler and sampler.
- `org.weasis.opencv.seg` — segmentation primitives.
- `org.weasis.opencv.waveform` — DICOM waveform readers (native and T.261) and the T.261 streaming writer.

The OpenCV Java sources themselves are not vendored: they are pulled at build time from the `opencv:sources` artifact, unpacked into `target/sources-import`, and added as an extra source root via `build-helper-maven-plugin`.

## Conventions

- Formatter: [google-java-format](https://github.com/google/google-java-format) via Spotless.
- License header (EPL-2.0 OR Apache-2.0) is enforced by Spotless on every Java file.
- Tests use [JUnit 6](https://junit.org/) (`org.junit.jupiter.*`) and [Mockito](https://site.mockito.org/).

## License

Dual-licensed under the [Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html) and the [Apache License v2.0](https://www.apache.org/licenses/LICENSE-2.0).
