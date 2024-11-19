MOSDL change log
================

Current version: __${project.version}__


Version compatibility table
---------------------------

| MAL issue          | recommended MOSDL release | Java versions          |
| ------------------ | ------------------------- | ---------------------- |
| 3 (March 2024)     | ${project.version}        | 11, 17, 21             |
| 2 (March 2013)     | 0.2.0                     | 11, 17, 21             |
| 2 (March 2013)     | 0.1.1                     | 8                      |
| 1 (September 2010) | unsupported               | -                      |


Version migration guides
------------------------

This section is intended to give guidance for migrations due to major breaking changes, especially those that change syntax or semantics of MOSDL files. Please refer to the version history for minor changes, e.g. in the usage of MOSDL. 


### Migration from version <= 0.2.0 to ${project.version} (MAL issue 2 to 3)

Issue 3 of the MAL marks a breaking change in the definition of CCSDS Mission Operations Services. This has direct consequences for MOSDL files and depending on the features you use for specifying services, migration might not be straight-forward. The following list guides you through the migration process, with the steps likely requiring most work at the top.

* The COM object model has been replaced with a simpler MO object model. Objects are now specified by composites inheriting from `MAL::Object` and can be referred to using the `ObjectRef<...>` Attribute type instead of a non-typesafe `Long` identifier. The previously used `source` and `related` links are gone and can now be replaced with any number and name of fields appropriate to your data model. All COM services have been removed, so you need to think if and how to replace them. If you made use of the Activity Tracking Service Pattern it is advised to model the appropriate interactions directly in your service model.
* PUBLISH-SUBSCRIBE interaction patterns have been simplified. Instead of the rigid four subscription keys you can now define your own appropriate to your service model. In MOSDL this means that `pubsub` operations now have to provide a (potentially empty) subscription key message. In contrast to other "real" messages, only Attribute types are allowed for the fields. Also, the PUBLISH/NOTIFY update structure will not automatically be made a List. If you need a List you need to specify a List for the PUBLISH/NOTIFY message yourself.
* Lists are not allowed to contain `null` values anymore. If you need this functionality you can specify a wrapper Composite around your values instead.
* Lifting of polymorphic restrictions should not affect migration, but you should make yourself familiar with the new possibilities by reading MAL Issue 3.
* Service-level data types and errors have been removed. If you made use of such, you have to move them to Area-level. If you previously referred to Service-level data types (using a fully qualified or partly qualified reference) you also need to adapt your references and remove the Service part.
* Errors cannot be defined directly in operations anymore. Define them at Area-level and refer to them from the operation.
* The "support in replay" feature has been removed. Simply delete any asterisk in front of your operation names.
* The MOSDL convenience feature of automatically assigning capability sets has been changed: Previously, each operation without explicitly specified capability set has been put into its own capability set. Now, all consecutive operations without specified capability sets are put into a single capability set. It is recommended that you explicitly specify capability sets if you care about them.


MOSDL version history
---------------------

### 0.3-SNAPSHOT (unreleased)
- Not released yet.
- Breaking change: Add support for MAL issue 3, drop support for MAL issue 2. Changes are too numerous to list them here. See [migration section](#version-migration-guides) for more information.
- Put consecutive operations without specified capability into one capability set instead of each operation into its own.
- Load MOSDL files from a directory explicitly in lexicographic order of the file names (this affects auto-generated numbering).
- When generating MOSDL don't emit numbers (area, service, operation numbers, short form parts) if they are consecutive starting with 1, such that they would be auto-generated like this.

### 0.2.0 (2024-05-16)
- Drop support for Java 8.
- Add support for Java 11+ (at least up to Java 21).
- New experimental XSD generator for generating XML Schema files for data structures defined in an MO specification.
- Rename of the Maven plugin configuration switch `mosdl-doc-type` to `doc-type`.
- Update dependencies.

### Version 0.1.1 (2019-12-05)
- #1: Maven plugin now properly creates target directory.

### Version 0.1.0  (2019-12-02)
- First release.
