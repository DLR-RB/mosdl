MOSDL - Mission Operations Service Description Language
=======================================================

The Mission Operations Service Description Language MOSDL can be used to describe [CCSDS MO services](https://public.ccsds.org/Pubs/521x0b2e1.pdf) in terms of operations and data types. It is supposed to be a concise notation that serves as a basis for documentation, code generation, quick iteration and discussion without the need for graphical tools or manual or assisted XML editing. Where possible sensible defaults are chosen so that the service author can omit information that he deems unnecessary (e.g. service or data type numbers). Yet it remains possible to specify this information if full control is needed.

MOSDL files can be read in by the MOSDL compiler. It can emit a CCSDS MO XML compliant specification file and CCSDS MO XML specifications can be transformed back into MOSDL.

Syntax highlighting is provided for [Notepad++](https://notepad-plus-plus.org/) users: Just load [`npp_mosdl.xml`](npp_mosdl.xml) in Notepad++ using its user-defined language functionality.


Table of contents
-----------------

- [MOSDL syntax](#mosdl-syntax)
    - [General structure](#general-structure)
        - [Comments](#comments)
        - [Documentation](#documentation)
        - [Identifiers](#identifiers)
    - [Areas](#areas)
    - [Type references and imports](#type-references-and-imports)
    - [Services](#services)
    - [Operations](#operations)
        - [SEND operation](#send-operation)
        - [SUBMIT operation](#submit-operation)
        - [REQUEST operation](#request-operation)
        - [INVOKE operation](#invoke-operation)
        - [PROGRESS operation](#progress-operation)
        - [PUBSUB operation](#pubsub-operation)
        - [Capability sets](#capability-sets)
    - [Data types](#data-types)
        - [Nullable types](#nullable-types)
        - [Lists](#lists)
        - [Composites](#composites)
        - [Enumerations](#enumerations)
    - [Errors](#errors)
- [Example](#example)


MOSDL syntax
------------

### General structure

The typical structure of a MOSDL file looks like the following. This file just serves as illustration and will not compile because some things have been replaced by ellipses. All features will be explained in this document.

```
area areaName [4711]

import ...
import ...

service ServiceName {
    request myOperation(...) -> (...)

    progress longRunning(...)
        -> (...)
        -> (...)*
        -> (...) throws SOME_ERROR
    
    pubsub monitor <- (...)
    
    composite ServiceLevelType {
        ...
    }
}

composite AreaLevelType {
    ...
}

enum SomeEnum {
    ...
}

error SOME_ERROR
```

MOSDL keywords, names and so on are case-sensitive, white-space does not have any significance and can be inserted freely. Comments are possible anywhere and different from documentation, for which only certain positions are possible. The former is ignored, the latter can be used to generate documentation.

MOSDL assumes that your service descriptions are contained in files. They usually end in `.mosdl` and contain any number of services, data types and errors. Each MOSDL file is namespaced in a concrete `area` which you have to specify at the top of the file and which corresponds to the MO notion of area. Several MOSDL files may have the same area such that their contents are all put in the same area. It is recommended to put a single service specification in a single file.


#### Comments

Comments may appear anywhere in the MOSDL file and are ignored. Comment style is Java comment style:

* `//` for line comments
* `/* bla bla bla */` for multi-line block comments

#### Documentation

Most elements in MOSDL can be documented. The documentation usually has to come right *before* the element that shall be documented. Additionally, operations allow specially formatted documentation similar to Javadoc or Doxygen to refer to messages, parameters and errors instead of putting the documentation right before each of these element. The following sections show where exactly you can put documentation. Python-style and triple-slash style documentation is supported:

* `///` for a single line documentation
* `""" bla bla bla """` for multi-line block documentation

#### Identifiers

Several elements in MO have two identifiers: a name and a number, e.g. services, operations, composite data types, enumerations, errors and so on. In general, MOSDL requires that you specify the name, but does not require you to specify the number. If you don't specify the number, MOSDL automatically assigns one starting with `1` (except for areas, which will start with `256`). If you explicitly specify a number all subsequent auto-generated numbers for that element type that you do not explicitly specify will increment starting from your explicitly set number. You must not use numbers smaller than `1`. For example:

```
send firstOperation()            // auto-assigned operation number: 1
send secondOperation [42] ()     // user-assigned operation number: 42
send thirdOperation()            // auto-assigned operation number: 43
```

As you have seen numbers are specified by putting them in square brackets right after the name. You can specify numbers either in decimal or (case-insensitive) hexadecimal format: `error MY_DEC_ERROR [123]` or `error MY_HEX_ERROR [0xAFFE]`.

Names can be put directly in the specification or they can be put between quotation marks. Quotation marks are mandatory if you use a keyword as name in order to distinguish the two. For example:

```
send fourthOperation("request": String)
```


### Areas

Think of an area similar to a Java package: It is a namespace for services, data types and errors. The area is mandatory, therefore you have to start each MOSDL file with an area statement. Optional documentation comes right before the `area` keyword, the optional area number and are version are put in square brackets after the area name:

```
"""
This is the documentation of a test area.
Its name is 'areaName', its number is 4711 and its version is 2.
"""
area areaName [4711.2]
```

You can leave out the number. In this case the auto-assigned area numbers start with `256`. This is an arbitrary choice which allows some headroom for future CCSDS-defined areas which currently are:

* `1`: MAL
* `2`: COM
* `3`: Common
* `4`: MC

You can also leave out the dot and the version number. In this case the default version number `1` is assigned. The following combinations are possible:

```
area plain              // name: 'plain',       number: auto-assigned starting with 256, version: 1
area onlyNumber [4711]  // name: 'onlyNumber',  number: 4711,                            version: 1
area onlyVersion [.2]   // name: 'onlyVersion', number: auto-assigned starting with 256, version: 2
area full [4711.2]      // name: 'full',        number: 4711,                            version: 2
```


### Type references and imports

Types and errors can be referred to (e.g. in the message signature of an operation) using three different notations:

* Fully qualified: `areaName::typeName` (for area-level types and errors) and `areaName::serviceName.typeName` (for service-level types and errors)
* Partly qualified: `serviceName.typeName`
* Unqualified: `typeName`

Only two scopes exist: service-level scope and area-level scope. Using a fully qualified type reference is straight-forward because the referenced type is unique. You can use a partly qualified type reference if you reference types from the same area, but a different service. Use unqualified type references to refer to service-level types from the same service or area-level types from the same area. If you are in service scope this scope will be searched first and only if the type is not found there the enclosing area scope is searched for the type. If you are in area scope only the area scope will be searched for the type. This also means that you need to use at least a partly qualified name if you refer to a service-level type from area scope.

You can also import types (and errors) from other areas and services using an `import` statement:

```
import MC::Parameter.ValidityState
```

This allows you to refer to the imported types by unqualified reference (simply `ValidityState` in this example). Import statements come directly after the `area` statement and always need to refer to foreign types using fully qualified references. If one of your imported types collides with one of your defined types, the imported type will shadow your defined type and you need to use a partly or fully qualified reference to refer to your defined type.

All fundamental MAL data types are imported implicitly from the `MAL` area and are listed here:

* Blob
* Boolean
* Double
* Duration
* FineTime
* Float
* Identifier
* Integer
* Long
* Octet
* Short
* String
* Time
* UInteger
* ULong
* UOctet
* URI
* UShort
* Attribute
* Element
* Composite


### Services

You can specify one or more services in a file, though it is recommended to use one file per service. Documentation comes right before the `service` keyword, the optional service number is put in square brackets after the area name. Service contents need to be enclosed in curly braces:

```
/// Documentation for service 'ServiceName'. This service will get number 1 auto-assigned.
service ServiceName {
    // all service contents (data types, operations, errors) come here
}
```

You can put capability sets, operations, data types and errors inside a service. If an operation is put directly into a service without enclosing capability set, the operation is implicitly put in its own capability set.


### Operations

Operations are specified using a keyword denoting the interaction pattern, the message signatures (i.e. data types contained in the exchanged messages) and the possible errors. Message signatures look like typed parameter lists enclosed in parentheses: `(firstMessagePart: FirstType, secondMessagePart: SecondType)`. Commas between message parts are optional so that you can put them on separate lines easily. Stylized arrows denote the direction of a message: No arrow for messages originating from a consumer, `->` for messages from provider to consumer, `<-` for messages from provider to broker or broker to consumer.

As usual, operation numbers are optional. If they are specified, they are put in square brackets after the operation name. Errors that can be raised by the operation need to be put in a `throws` clause. Multiple errors are comma-separated. Errors can either be referenced from service-level or area-level errors or they can be defined in-line using the `error` keyword (see the 'Errors' section for details). It is possible to specify a data type to use as extra information object for an error after a colon (the same considerations apply here as outlined in the 'Errors' section). As an example a REQUEST operation with name `myOperation` and number `42` might look like this:

```
request myOperation [42] (firstParam: String, secondParam: MyType)
    -> (someResponseNumber: UInteger)
    throws MAL::INTERNAL, MY_OWN_ERROR: Integer, ANOTHER_ERROR, error MY_INLINE_ERROR [666] : String
```

It is possible to mark side-effect free operations by an asterisk right before the operation name. This corresponds to the *support in replay* feature of MO and generally should be used for operations that can be invoked without changing any state. Example:

```
request *getFileNames(directory: String) -> (fileNames: List<String>)
```

Documentation is allowed in several places:

* before the interaction pattern keyword for operation documentation,
* before the message arrow (or if no arrow is present before the opening parenthesis) for message documentation,
* before the name for each parameter in the message parts list for message parts documentation,
* before the `error` keyword or error reference for error documentation,
* before the data type of the extra information for extra information documentation.

Using all of these becomes difficult to read but shall be demonstrated here as an example, using the same operation as above:

```
"""
This is the operation documentation.
"""
request myOperation [42]
    /// This is the documentation for the REQUEST message.
    ("""Doc for the first param.""" firstParam: String, """Doc for the second param.""" secondParam: MyType)
    /// This is the documentation for the RESPONSE message.
    -> (""" Doc for the only response param.""" someResponseNumber: UInteger)
    throws
        /// Documentation for the INTERAL error.
        MAL::INTERNAL,
        /// Documentation for MY_OWN_ERROR.
        MY_OWN_ERROR:
            /// Some number helpful for understanding this error.
            Integer,
        """
        Multi-line documentation
        for the in-line defined MY_INLINE_ERROR.
        """
        error MY_INLINE_ERROR [666] : """Human readable error cause.""" String
```

Documentation before the interaction pattern keyword is treated specially: In order to keep the specification readable you can use (similar to Javadoc or Doxygen) special tags to document messages belonging to the operation, parameters of each message, and each error. In this case you do not need any further documentation directly at each of these elements (known as "in-line" documentation). If you have both "bulk" and in-line documentation they are merged with a line-break separator.

Documentation tags look like `@tagname optionalParam: The actual documentation.`, where `@tagname` needs to be on the start of a new line (disregarding any whitespace) and the colon must be followed by whitespace. Whether a tag parameter has to be given depends on the tag. The following tags can be used:

Tags for documenting messages (no parameter expected). Which ones you can use depends on the interaction pattern:

* `@send` for SEND messages
* `@submit` for SUBMIT messages
* `@request` for REQUEST messages
* `@response` for REQUEST_RESPONSE and PROGRESS_RESPONSE messages
* `@invoke` for INVOKE messages
* `@ack` for INVOKE_ACK and PROGRESS_ACK messages
* `@progress` for PROGRESS messages
* `@update` for PROGRESS_UPDATE messages
* `@publish` for PUBSUB_PUBLISH and PUBSUB_NOTIFY messages

Tags for documenting message parameters. The parameter name has to be given as parameter. Again, it depends on the interaction pattern which ones you can use:

* `@sendparam`
* `@submitparam`
* `@requestparam`
* `@responseparam`
* `@invokeparam`
* `@ackparam`
* `@progressparam`
* `@updateparam`
* `@publishparam`

Tags for documenting errors. The error name (in case of in-line error definitions) or the error type (in case of error references) has to be given as parameter. If you give an error type, it has to be literally the same as the one used in the code, i.e. if you use an unqualified type reference you have to use an unqualified reference here as well, and if you use a fully qualified reference you have to use a fully qualified reference here as well:

* `@error` for errors and error references
* `@errorinfo` for the extra information field of the error or error reference

Any other tags are reserved but will be ignored currently. Rewriting the previous example in terms of bulk documentation looks like the following:

```
"""
This is the operation documentation.

@request: This is the documentation for the REQUEST message.
@requestparam firstParam : Doc for the first param.
@requestparam secondParam: Doc for the second param.

@response: This is the documentation for the RESPONSE message.
@responseparam someResponseNumber: Doc for the only response param.

@error     MAL::INTERNAL  : Documentation for the INTERAL error.
@error     MY_OWN_ERROR   : Documentation for MY_OWN_ERROR.
@errorinfo MY_OWN_ERROR   : Some number helpful for understanding this error.
@error     MY_INLINE_ERROR: Multi-line documentation
for the in-line defined MY_INLINE_ERROR.
@errorinfo MY_INLINE_ERROR: Human readable error cause.
"""
request myOperation [42] (firstParam: String, secondParam: MyType)
    -> (someResponseNumber: UInteger)
    throws MAL::INTERNAL, MY_OWN_ERROR: Integer, ANOTHER_ERROR, error MY_INLINE_ERROR [666] : String
```

All possible MO interaction pattern can be expressed:

#### SEND operation

```
send operationName(firstParam: String, secondParam: MyType)
```

The only message that can be specified is the SEND message. Please note that this operation must not throw any errors.

#### SUBMIT operation

```
submit operationName(firstParam: String, secondParam: MyType)
```

The SUBMIT message has to be specified and the (always empty) ACK message does not need to be spelled out.

#### REQUEST operation

```
request operationName(firstParam: String, secondParam: MyType) // REQUEST message
    -> (someResponseNumber: UInteger)                          // RESPONSE message
```

Both REQUEST and RESPONSE messages need to be specified.

#### INVOKE operation

```
invoke operationName(firstParam: String, secondParam: MyType?) // INVOKE message
    -> (someAckNumbers: List<UInteger?>)                       // ACK message
    -> (text: String?, value: Attribute)                       // RESPONSE message
```

All three messages (INVOKE, ACK and RESPONSE) have to be specified.

#### PROGRESS operation

```
progress operationName(firstParam: String, secondParam: MyType) // PROGRESS message
    -> (someAckNumber: UInteger)                                // ACK message
    -> (messageNumber: Integer, update: Blob)*                  // UPDATE messages
    -> (text: String?, value: Attribute)                        // RESPONSE message
```

All four messages (PROGRESS, ACK, UPDATE and RESPONSE) have to be specified. The UPDATE message has a trailing asterisk `*` in order to denote that this message may occur more often.

#### PUBSUB operation

```
pubsub monitor <- (eventText: String)
```

The only message you can specify here is the PUBLISH/NOTIFY message. All other messages (SUBSCRIBE, UNSUBSCRIBE, ...) are generated according to the PUBSUB pattern and their contents are fixed by MO. PUBLISH is the message from provider to broker and NOTIFY the message from broker to consumer. Message contents are identical for both messages, therefore you only can specify one message here. Please note the left-pointing arrow, as opposed to the right-pointing arrow for the other interaction patterns.

#### Capability sets

MO allows to group operations into capability sets as a means to tailor a service specification. Each capability set is referred to by number, but as usual MOSDL does not require you to specify the number explicitly. Capability sets do not have names. If you do not care about capability sets you do not need to use them. In this case MOSDL assigns an own capability set for each operation. Documentation comes right before the `capability` keyword. Capability sets do not open a new scope, i.e. the scope is still that of the enclosing service. Example:

```
/// Basic operations for just getting definitions.
capability [7] {
    request listDefinitions() -> (definitionIds: List<Identifier>)
    request getDefinition(definitionId: Identifier) -> (definition: Element)
}

/// Advanced operation to store new definitions.
capability {
    submit addDefinition(definitionId: Identifier, newDefinition: Element)
}
```


### Data types

Data types can be specified on area or service level by using the respective keywords for the possible specifications, `composite` and `enumeration`. On area-level a third keyword (`fundamental`) is possible, but only MAL itself is allowed to use it. These fundamental MAL data types are automatically imported and can be used with unqualified references. The `Attribute` and `Element` data types are abstract and need to be filled in by concrete types when they are used during program execution. `Attribute` stands for any of the 18 concrete basic MAL types, `Element` for any concrete type (also NULL and list types).

#### Nullable types

For every type a corresponding nullable type exists that additionally allows a NULL value denoting the absence of any value. The nullable type that corresponds to a type `MyType` is simply `MyType?`, i.e. with a following question mark.

#### Lists

For every type a corresponding list type exists. The list type that correspond to a type `MyType` is simply `List<MyType>`. You can freely combine list and nullable types:

* non-nullable list type of non-nullable elements: `List<MyType>`
* nullable list type of non-nullable elements: `List?<MyType>`
* non-nullable list type of nullable elements: `List<MyType?>`
* nullable list type of nullable elements: `List?<MyType?>`

Because MAL (currently) does not know lists of non-nullable elements the first two examples are equivalent to the last two. Nonetheless, you still should make clear whether NULL values can be expected as list elements, even if it "only" serves as documentation.

Please note that according to MAL the only place where you can use lists of abstract types is as last part of a message.

#### Composites

Composites are user-defined structures with a name, defined using the `composite` keyword and curly braces enclosing the list of composite fields. Only concrete composites have a number, called the *short form part* in MAL language. As usual, the number is optional in MOSDL and is auto-assigned if left out. Abstract composites are denoted using the `abstract` keyword and may be the ancestor for other composites that inherit from or *extend* the abstract composite. MAL does not allow extending concrete composites nor does it allow multiple extension. Composite fields have a name and a type. The only abstract type allowed as field type is `Attribute`.

Documentation for the composite comes before the `composite` keyword (or for abstract composites before the `abstract` keyword). Each field may be documented right before the name of the field. Example:

```
/// Composite representing a color.
composite Color {
    /// The red value between 0 and 1.
    r: Double
    /// The green value between 0 and 1.
    g: Double
    /// The blue value between 0 and 1.
    b: Double
}

abstract composite Vehicle { // no number is allowed for abstract composites
    maxSpeed: UInteger
    color: Color?
}

composite Car extends Vehicle {
    fuel: FuelType
    licensePlate: Identifier
}

composite Airplane [42] extends Vehicle {
    numberOfEngines: UInteger
    wingSpan: UInteger
}
```

#### Enumerations

Enumerations are user-defined types with a finite fixed set of countable values. They are defined using the `enum` keyword, a name, an optional short form part and a set of value items enclosed in curly braces. Each item may be assigned a number value. If left out one is assigned automatically. Commas between value items are optional.

Documentation comes before the `enum` keyword for the enumeration and before the value item for the value.

```
/// This enumeration denotes the possible fuel types for a car.
enum FuelType {
    /// Gasoline, petrol, Benzin.
    GASOLINE
    DIESEL
    ELECTRIC [4]
    HYDROGEN
}
```

### Errors

Errors can be defined on area-level, on service-level or in-line when specifying operations using the `error` keyword followed by the error name, an optional error number (auto-generated if left out) and an optional (colon-separated) type for any extra information associated with the error. You cannot put a nullable type here because the extra information type is always treated to be nullable, i.e. you must always be prepared to receive NULL instead of a value. You can only specify a single data type for extra information. If you need to provide more complex data, define a composite type to use here.

Documentation comes before the `error` keyword for the error and before the extra information type for the extra information documentation. For in-line error definitions on operations you may alternatively provide error documentation in the bulk documentation using `@error` and `@errorinfo` tags. Please note that this kind of documentation is currently not allowed for area-level and service-level errors. Examples:

```
/// Simple error that does not contain extra information.
error SOME_ERROR
error DESCRIPTIVE_ERROR [0xD00F] : """Human readable error cause.""" String
```


Example
-------

This is an example that demonstrates many of the concepts outlined above.

File `VerySimpleService.mosdl`:

```
area hcc [4711.2]

"""
This a very simple service with
just one operation and one data type.
"""
service VerySimpleService {
    request myOnlyOperation (text: String?, numbers: List<UInteger?>)
        -> (answer: String, myInstance: MyOwnType)
        throws MY_ONLY_ERROR

    /// The only data type of this service.
    composite MyOwnType {
        numberList: List<UInteger?>
        isFullMoon: Boolean?
    }
}

error MY_ONLY_ERROR [12345]
```

File `ComplexService.mosdl`:

```
area hcc [4711.2]

import hcc::VerySimpleService.MyOwnType

service TestService [3] {
    request *getValue(id: Identifier) -> (value: Attribute?) throws NOT_FOUND

    capability {
        submit setValue [5](id: Identifier, newValue: Attribute?) throws NOT_FOUND, error INVALID: InvalidType
        request lockValue(id: Identifier) -> (lockedValue: Attribute?) throws NOT_FOUND
    }

    /// @progressparam valueHistory: Determines whether to include value history.
    progress *listIds(includeValueHistory: Boolean)
        -> (expectedNumberOfIds: UInteger)
        -> (id: Identifier, """Only set if includeValueHistory is true. """ valueHistory: List?<Attribute?>)*
        -> (processingTime: Duration)
        throws MAL::TOO_MANY

    pubsub monitorValueChanges <- (oldValue: Attribute?, newValue: Attribute?)

    /*
    composite CommentedOut {
        ... - this composite is completely ignored; it is not even a valid composite
    }
    */

    enum InvalidType { // TODO: Maybe add some more values?
        TOO_LARGE
        TOO_SMALL
    }
}

service OrbitService {
    invoke calculate(timestamp: Time)
        -> (expectedTimeUntilAnswer: Duration)
        -> (answer: OrbitStuff)
}

abstract composite OrbitStuff {
    inclination: Double
}

composite MoonOrbitStuff extends OrbitStuff {
    fullMoonInfo: MyOwnType
    isMoonFromCheese: Boolean
}

error NOT_FOUND [0xFFFE]
```
