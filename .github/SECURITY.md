# Security Policy

`weasis-core-img` is a Java wrapper around OpenCV with additions oriented toward
medical imaging (window/level LUTs, segmentation primitives, raw image I/O). It
decodes and transforms image data — often untrusted DICOM or raw pixel input —
through native OpenCV code, and is consumed downstream by
[Weasis](https://github.com/nroduit/Weasis). Because malformed input can reach
native memory operations, we take security issues seriously and appreciate
responsible disclosure.

## Supported Versions

Security fixes are provided for the latest released version. The artifact version
tracks the underlying OpenCV native release (`<opencv.version>.x`), so we
recommend always running the most recent release.

| Version | Supported          |
| ------- | ------------------ |
| 5.0.x   | :white_check_mark: |
| < 5.0   | :x:                |

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues,
discussions, or pull requests.**

Instead, report them privately using one of the following channels:

- **Preferred:** Open a [private security advisory](https://github.com/nroduit/weasis-core-img/security/advisories/new)
  via GitHub's "Report a vulnerability" feature.
- Alternatively, email the maintainer at **nicolas.roduit@gmail.com**.

Please include as much of the following as you can to help us triage quickly:

- The type of issue (e.g. buffer overflow or native memory corruption, denial of
  service via a crafted image, path traversal on file I/O, unsafe deserialization,
  resource exhaustion, etc.).
- The affected component(s) and version (image decoding/`ImageIOHandler`, LUT
  handling, `FileRawImage` raw I/O, native library loading, etc.).
- Step-by-step instructions to reproduce the issue, ideally with a minimal sample.
- Proof-of-concept or exploit code, if available.
- The impact, including how an attacker might exploit it.

**Do not include real patient data** in your report. Use synthetic or fully
anonymized DICOM/image data only.

## Disclosure Process

- We will acknowledge receipt of your report within **5 business days**.
- We will investigate and provide an initial assessment within **10 business
  days**, and keep you informed of progress toward a fix.
- Once a fix is available, we will coordinate a release and a public advisory.
  We are happy to credit you in the advisory unless you prefer to remain
  anonymous.

We ask that you give us a reasonable amount of time to address the issue before
any public disclosure.

## Scope

This policy covers the `weasis-core-img` library and its source code in this
repository. Vulnerabilities in third-party dependencies — including the bundled
OpenCV native libraries — should be reported to the respective upstream projects;
if such an issue affects `weasis-core-img`, feel free to let us know so we can
update.

Thank you for helping keep `weasis-core-img` and its users safe.
