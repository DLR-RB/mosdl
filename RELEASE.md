MOSDL release guide
-------------------

This guide is intended to be used by the repository maintainer in order to release a new version.

Make sure your local `~/.m2/settings.xml` file contains the following settings:

```xml
<settings>
    <servers>
        <server>
            <id>gpg.passphrase</id>
            <passphrase>clear or encrypted passphrase</passphrase>
        </server>
        <server>
            <id>ossrh</id>
            <username>your-jira-id</username>
            <password>your-jira-pwd</password>
        </server>
    </servers>
</settings>
```

If your `settings.xml` does not contain the `gpg.passphrase` you can leave it out (and get asked when you build) or you can supply it when building: `mvn deploy -Prelease -Dgpg.passphrase=thephrase`

All of the following steps happen on the `master` branch. Currently, there is no need for a more sophisticated branching model.

1. Update version number to release version:
	```
	mvn org.codehaus.mojo:versions-maven-plugin:set -DgenerateBackupPoms=false -DnewVersion=<release version>
	```
2. Make sure [`CHANGELOG.md`](CHANGELOG.md) contains all relevant changes for the new release up to <release version>.
3. Make sure [`THIRD-PARTY.txt`](THIRD-PARTY.txt) contains all relevant notices for dependencies (use `mvn dependency:tree` for reviewing a list of dependencies or use `mvn license:third-party-report "-Dlicense.includedScopes=compile,runtime"` for generating a license report HTML file, which you should still review).
4. Stage all changes in git.
5. Build, test and deploy to Maven Central (uploading may take one or two minutes):
	```
	mvn clean deploy -Prelease
	```
6. Release deployed artifacts to Maven Central:
	- Login to https://oss.sonatype.org/
	- Click `Build Promotion -> Staging Repositories -> <newly created repository>` and examine contents for correctness.
	- Click `Close` in the top toolbar, click `Confirm` and wait one or two minutes until the close activity has finished (click `Refresh` to see the current state).
	- Click `Release` in the top toolbar. Wait until status reads `released` (click `Refresh` to see the current state).
7. Copy contents of `target/mosdl-<release version>` to `.github` folder (except the JAR file; overwrite if necessary).
8. Stage and commit all changes in git. Commit message:
	```
	Release v<release version>
	```
9. Create annotated release tag in git. Tag name:
	```
	v<release version>
	```
10. Update version number to next snapshot version:
	```
	mvn org.codehaus.mojo:versions-maven-plugin:set -DgenerateBackupPoms=false -DnewVersion=<snapshot version>
	```
11. Add new section to top of [`CHANGELOG.md`](CHANGELOG.md):
	```
	### <snapshot version> (unreleased)
	- Not released yet.
	```
12. Stage and commit all changes in git. Commit message:
	```
	Version upped to <snapshot version>.
	```
13. Create a new release in GitHub:
	- Use tag `v<release version>`.
	- Release title: `Release v<release version>`
	- Copy section `### <release version>` from [`CHANGELOG.md`](CHANGELOG.md) to release description text box.
	- Attach `target/mosdl-<release version>.zip` to release.
