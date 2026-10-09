# Publishing to Maven Central

The nine loader / Minecraft targets have independent Maven coordinates. The root `gradle.properties` supplies the version. `scripts/publishing/prepare-central.sh` uses each target's own Gradle Wrapper and creates one Central Portal upload ZIP for the complete target matrix. It only prepares local files and does not upload them.

In Git Bash, set `JAVA_HOME_17`, `JAVA_HOME_21`, and `JAVA_HOME_25` to existing JDK installations. Each target selects its Gradle JVM through `ci.java` in `ci.properties`; its configured compilation toolchain remains unchanged.

To prepare artifacts and checksums before configuring a signing key:

```bash
bash scripts/publishing/prepare-central.sh unsigned
```

Each `targets/<target>/build/central/repository/` contains the runtime JAR, sources JAR, Javadoc JAR, POM, and checksums. Unsigned mode does not create an upload ZIP.

Signed preparation uses Gradle's built-in Signing plugin and local GnuPG. Select an existing key through personal Gradle configuration or the following non-secret parameters. Keep private keys and passphrases out of the project and command line. GPG Agent / Pinentry handles the passphrase.

```bash
bash scripts/publishing/prepare-central.sh signed \
  -Psigning.gnupg.executable=gpg \
  -Psigning.gnupg.homeDir=/path/to/your/gnupg-home \
  -Psigning.gnupg.keyName=YOUR_PUBLIC_KEY_FINGERPRINT
```

Publish the public key according to [Sonatype's GPG requirements](https://central.sonatype.org/publish/requirements/gpg/). Successful signed preparation creates `build/central/KltytonUI-<version>.zip`, containing only the current version of the nine components. It excludes `maven-metadata.xml`, other versions, and local agent rules. Forge signs the reobfuscated JAR; Fabric uses Loom's production artifacts.

With Windows Git's bundled GnuPG, select the key directory through `GNUPGHOME` and omit `signing.gnupg.homeDir`. The script converts the directory to a Git Bash path and preserves it for Gradle child processes, avoiding drive-letter colons in GPG Agent socket names. `signing.gnupg.executable` can point to the installed `gpg.exe` by its full path.

Submit the ZIP through the [Central Portal upload workflow](https://central.sonatype.org/publish/publish-portal-upload/). Check the organization's Usage Center for current limits. Local preparation does not mean a quota request has been approved or that the components are publicly available.

After publication and coordinate verification, downstream Gradle projects can use `mavenCentral()` and the matching target dependency, for example `io.github.kltyton.kltytonui:KltytonUI-neoforge-26.2:<published-version>`. The runtime still needs Rhino and the loader dependencies declared in that target's metadata.
