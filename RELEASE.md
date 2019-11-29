MOSDL release guide
-------------------

__WARNING__: The process described here is preliminary!

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
	mvn version:set -DnewVersion=<release version>
	```
2. Make sure [`CHANGELOG.md`](CHANGELOG.md) contains all relevant changes for the new release up to <release version>.
3. Stage all changes in git.
4. Build, test and deploy to Maven Central:
	```
	mvn deploy -Prelease
	```
5. Copy contents of `target/mosdl-<release version>` to `.github` folder (except the JAR file; overwrite if necessary).
6. Stage and commit all changes in git. Commit message:
	```
	Release v<release version>
	```
7. Create release tag in git. Tag name:
	```
	v<release version>
	```
8. Update version number to next snapshot version:
	```
	mvn version:set -DnewVersion=<snapshot version>
	```
9. Add new section to top of [`CHANGELOG.md`](CHANGELOG.md):
	```
	### <snapshot version> (unreleased)
	```
10. Stage and commit all changes in git. Commit message:
	```
	Version upped to <snapshot version>.
	```
11. Create a new release in GitHub:
	- Use tag `v<release version>`.
	- Release title: `Release v<release version>`
	- Copy section `### <release version>` from [`CHANGELOG.md`](CHANGELOG.md) to release description text box.
	- Attach `target/mosdl-<release version>.zip` to release.
