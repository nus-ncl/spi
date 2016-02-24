---
layout: default
---
[[TOC]]

# DETER Testbed API

The DETER testbed API is broken up into services, each covering one of the major abstractions that the testbed exports.  The services are:

 * Admin
 * ApiInfo
 * Users
 * Projects
 * Experiments
 * Libraries
 * Resources
 * Realizations

We discuss each in detail and provide links to the detailed javadoc of the implementation, where applicable and available.

The document and API are both works in progress and will be fleshed out and changed as the API is modified.

## General Info

The API is implemented as a SOAP web service over TLS secured connections.  The user\'s identity is generally provided by a client certificate, but the client certificates are generally transient.  A valid temporary client certificate can be requested from the API by correctly responding to a challenge keyed on the user\'s password.

The API is implemented using the axis web service framework, which means that javadocs are provided for the various calls.

Each service and call are accessed by appending servicename/operation to the base URL of the API server.  On DETERlab the base URL will be https://api.isi.deterlabnet:52323/axis2/service.  For example, one can access the getVersion operation below at https://api.isi.deterlabnet:52323/axis2/service/ApiInfo/getVersion 

Each operation returns useful parameters on success and throws a fault, called a DeterFault, on an error.  Faults are standard SOAP faults with a detail section that includes the following fields:

 * ErrorCode - a 32-bit integer encoding the type of error.  Constants are available in the [http://www.isi.edu/~faber/tmp/DeterAPI/doc/constant-values.html the javadocs].  Values are:
   * access - access denied
   * request - bad request
   * internal - internal server error
   * password - user has an expired password that must be changed
   * login - user is not logged in for a call that requires a valid login.
 * ErrorString - a string describing the broad error
 * DetailString - a string describing the details that caused the error

The `ErrorString` and `ErrorCode` are equivalent, but the information in the `DetailString` is generally more informative about the specifics.  Request or access errors are generally correctable on the client side while internal errors are not.

[http://www.isi.edu/~faber/tmp/DeterAPI/doc/net/deterlab/testbed/api/DeterFault.html javadoc for DeterFault]


## ApiInfo

The ApiInfo service provides metadata about the running DETER API.  It also provides a simple check that a user is presenting authentication credentials correctly.

[http://www.isi.edu/~faber/tmp/DeterAPI/doc/net/deterlab/testbed/api/ApiInfo.html javadoc for ApiInfo]

The primary operation in the ApiInfo service is `getVersion`.  The call is unauthenticated and can be made driectly from a web browser to confirm that the API is functioning and that the user can see DETERlab.

 * *Service:* ApiInfo
 * *Operation:* getVersion
 * *Input Parameters:*
   * None
 * *Return Values:*
   * Version - A string containing the API version number
   * PatchLevel - A string containing the patch level
   * KeyID - A string.  If the user presented a valid public key and passed the SSL challenge, this is the sha1 hash of that key.  If no key was presented or an invalid one, this field is not returned.

There is an addtional simpler service, `echo`, that takes one parameter and returns it:

 * *Service:* ApiInfo
 * *Operation:* echo
 * *Input Parameters:*
   * param - a string to echo
 * *Return Values:*
   * the same string

## Users

The Users API is concerned with managing users and their profiles as well as authenticating to the testbed and receiving a client certificate for later calls.

[http://www.isi.edu/~faber/tmp/DeterAPI/doc/net/deterlab/testbed/api/Users.html javadoc for Users]

### Authentication

A user\'s identity is tied to an X.509 client certifcate issued by DETER.  A certificate is bound to one user at a time, but a person can logout and log in as a different DETER user using the same certificate by presenting a different password, or logging out, which severs the connection of user to certificate.

A user can get a DETER-signed certificate by logging in without presenting an existing DETER-signed certificate.  For example, the first time a user logs in, they will get a DETER-signed certificate.

A UI is welcome to keep one certificate for a session, or to reuse the certificates until they expire.  The certificates are issued with several years of valid time.

A user can authenticate to the testbed and receive an X.509 certificate by requesting a challenge using the `requestChallenge` operation and responding to the challenge via the `challengeResponse` operation.

Currently only one challenge is available, type "clear".  The challenge has no data and the user replies with their password in clear text.  Note that the exchange is protected by the SSL encrypted exchange, but implementors should not respond to clear challenges with parameters in the query string.  The "masked" challenge will address this issue shortly.

 * *Service:* Users
 * *Operation:* requestChallenge
 * *Input Parameters:*
   * userid - the identity to authenticate
   * types - a list of challenges that are acceptable to the user.  May be empty
 * *Return Values:*
   * Type - the kind of challenge
   * Data - the data needed for the challenge
   * Validity - number of seconds the challenge may be responded to
   * ChallengeID - a 64-bit identifier that allows the server to validate the reply

Each challenge is valid for 2 minutes and are rate-limited.  Web interfaces should collect the password from the user before requesting a challenge from the API to avoid spurious timeouts.

 * *Service:* Users
 * *Operation:* challengeResponse
 * *Input Parameters:*
  * ResponseData - a binary string, the response to the challenge
  * ChallengeID - The 64-bit identifier of the challenge being responded to
 * *Return Values*:
  * Certificate - an optional  binary string containing a PEM-encoded X.509 certificate and private key. (Passed down the encrypted connection)  If the user presented a certificate on this connection, no credential is returned.

Any certificate returned by the challenge is signed by the testbed.

The binding between user and certificate lasts 24 hours.  It can be renewed by making another login exchange.  A UI may want to prompt a user to login again before the binding times out.

 * *Service:* Users
 * *Operation:* logout
 * *Input Parameters:*
  * None
 * *Return Values:*
  * A boolean, true if the logout succeeded

Removes the user-to-certificate binding for this certificate.  The certificate can be reused or discarded, but cannot be used to manipulate the testbed without another successful requestChallenge/challengeResponse exchange.


### Password Changes

The password is a unique user feature that is not in the profile because of its role in authentication.  When a user needs to change their password there are two API calls that can be used.

If the user knows their current (or expired) password, they can authenticate using the standard challenge response protocol and then call

* *Service:* Users
 * *Operation:* changePassword
 * *Input Parameters:*
  * uid - the userid to change (note that an admin may change others passwords)
  * newPass - the new passowrd
 * *Return Values*:
  * a boolean, true if successful, but errors will throw a fault

We expect that the web interface will handle issues like confirming user input to a password change page.  The changePassword call just makes the change directly.

If a user has forgotten their password, the user can request a password challenge, sent to them at their profile e-mail address.  The challenge is a 64-bit number that can be used to call changePasswordChallenge without logging in.  To request a challenge, the web interface calls:

* *Service:* Users
 * *Operation:* requestPasswordReset
 * *Input Parameters:*
  * uid - the userid to change
  * urlPrefix - a string prefixed to the challenge ID in the mail sent to the user
 * *Return Values*:
  * a boolean, true if successful, but most errors will throw a fault

Again, we expect this call to generally be made from a web interface that will then want to present an input form to the in order to reset their password.  The urlPrefix field provides that hook.  A web interface running on !https://niftydeter.com might call requestPasswordReset with parameters \'forgetfuluser\' and \'!https://niftydeter.com/reset.html?challenge=\'.  After that call forgetfuluser will get e-mail asking him or her to access the web page at !https://niftydeter.com/reset.html?challenge=1283548127541824, allowing `niftydeter.com` to present their password change interface, and do error checking, etc. on the new password.

Each challenge is valid for 2 hours, and they are rate-limited so only a few can be outstanding.

With a valid challenge in hand, the web interface can call

* *Service:* Users
 * *Operation:* changePasswordChallenge
 * *Input Parameters:*
  * challengeID - the 64-bit number from the e-mail
  * newPass - the new passowrd
 * *Return Values*:
  * a boolean, true if successful, but most errors will throw a fault


### Profile Manipulation

DETER keeps metadata about each user called a profile.  The API provides an authenticated user with several interfaces to query and modify their profile information.

In the API each element of profile data is represented as a structure with the following data in it:

 * name of the element
 * type of the element
  * string
  * integer
  * double
  * binary/opaque
 * value(s) of the element
 * a flag set if the element is optional
 * A flag set if the field can be removed from the profile
 * a modification type: elements may be read/write, read-only (e.g., username) or write-only (e.g., some shared secret) (valid strings are at [http://www.isi.edu/~faber/tmp/DeterAPI/doc/constant-values.html the constants documentation.]
 * a brief description of the field, intended to be presented by a web interface or other third party program
 * an ordering hint for web interfaces presenting the data to users, an integer
 * an optional length hint to give a web interface a hint as to how big an input field to present.  0 indicates no hint.

To get a profile schema, for example to create an empty web page,

 * *Service:* Users
 * *Operation:* getProfileDescription
 * *Input Parameters:*
 * *Return Values:*
   * Uid - always empty
   * A list of profile elements each containing
     * Name - a string, the element\'s name
     * DataType - a string giving the element\'s
       * string
       * integer
       * double
       * binary/opaque
     * Value - a string containing the element\'s value
     * Access - a string describing the access values (values at [http://www.isi.edu/~faber/tmp/DeterAPI/doc/constant-values.html javadoc as described above])
     * Optional - a flag true if the field is optional (must be present but may be empty)
     * Removable - a flag true if the field can be removed
     * Description - a string explaining the field
     * Format - a regular expression that can be used to validate the field entry (may be null, and generally is for optional fields)
     * FormatDescription - A brief, natural language description of the field input constraints, e.g. "A valid e-mail address" or "only numbers and spaces".
     * OrderingHint - an integer suggesting where to present the attribute. Lower numbers come first
     * LengthHint - an integer suggesting how long an input firld should be presented

To read a user\'s profile (generally only users can read their own profile).

 * *Service:* Users
 * *Operation:* getUserProfile
 * *Input Parameters:*
  * userid - a string naming the user to be retrieved
 * *Return Values:*
   * Userid - the user whose profile is returned
   * A list of profile elements each containing
     * Name - a string, the element\'s name
     * DataType - a string giving the element\'s
       * string
       * integer
       * double
       * binary/opaque
     * Value - a string containing the element\'s value
     * Access - a string describing the access values (values at [http://www.isi.edu/~faber/tmp/DeterAPI/doc/constant-values.html javadoc as described above])
     * Optional - a flag true if the field is optional (must be present but may be empty)
     * Removable - a flag true if the field can be removed
     * Description - a string explaining the field
     * Format - a regular expression that can be used to validate the field entry (may be null, and generally is for optional fields)
     * FormatDescription - A brief, natural language description of the field input constraints, e.g. "A valid e-mail address" or "only numbers and spaces".
     * OrderingHint - an integer suggesting where to present the attribute. Lower numbers come first
     * LengthHint - an integer suggesting how long an input field should be presented

Finally a user can modify a profile:
 
 * *Service:* Users
 * *Operation:* changeUserProfile
 * *Input Parameters:*
   * Userid - the user\'s profile to modify
   * A list of change requests. Each request contains
     * Name - the name of the field to change
     * Value - the new value of the field
     * Delete - a flag, if true delete the field rather than modify it
 * *Return Values:*
   * A list of responses each containing
    * Name - astring with the name of the field
    * Success - a flag indicating if the request succeeded
    * Reason - a string indicating the reason if Success is false

### Notifications

A notification is a short message from another user or the testbed asking a user to take an action (add someone to a project, update a password before expiration) or communicating other information to the user (testbed downtime, etc).  A web interface will want to present these to a user.

Each notification has a set of flags that define its state.  Currently there are 2

 * *READ* true if the user has read the notification
 * *URGENT*true if the testbed considers this urgent information

The user is free to change this state using the markNotifications call.  Note that notifications are per-user.  A user who clears the READ or URGENT flag does not clear the flag for other recipients of the notification.

 
 * *Service:* Users
 * *Operation:* getNotifications
 * *Input Parameters:*
   * Userid - the user\'s identity to gather notifications
   * FirstDate - an optional date.  If present only show notifications after that date
   * LastDate - an optional date.  If present only show notifications before that date
   * Flags - A potentially empty list of flags to check.  If a flag\'s name appears in this list, only notifications with their flag in the given state are returned.
     * Tag - a string, the name of the flag
     * isSet - a boolean, true if the flag should be set
 * *Return Values:*
   * A list of responses each containing
     * ID - a 64-bit integer identifying the notification
     * Flags - the notification state in the format
       * Tag - a string, the name of the flag
       * isSet - a boolean, true if notifications with the flag set should be returned, false if notifications with the flag unset should be returned
     * Sent - the date the notification was issued
     * Text - the text of the notification

The filtering variables are all applied if given.

The mask and flags variable define the state a user is searching for.  If a bit is set in both mask and flags, only messages with that bit set are returned.  Similarly if a bit is clear in flags and set in mask, only messages with that bit clear will be returned.

To be concrete: a user who wants to retrieve all unread messages (urgent or not) should send a flags field with the READ bit clear and a mask with the READ bit set.  To retrieve only unread urgent messages, the flags should have READ clear and URGENT set, and both READ and URGENT set in the mask.


 * *Service:* Users
 * *Operation:* markNotifications
 * *Input Parameters:*
   * Userid - the user\'s identity to gather notifications
   * Ids - an array of 64-bit integers, the identifiers of the notifications to mark
   * Flags - A list of flags to modify.  If a flag\'s name appears in this list, notifications will have their flag set to the given value.
     * Tag - a string, the name of the flag
     * isSet - a boolean, true if the flag should be set, false if it should be unset
 * *Return Values:*
   * None

Administrators can send notifications, for example to announce testbed events.  The call to do so is:

 * *Service:* Users
 * *Operation:* sendNotification
 * *Input Parameters:*
   * Users - a list of uids to receive the notification
   * Projects - a list of projects to receive the notification.  All users in the project will receive it
   * Flags - A list of initial flag values.  If a flag\'s name appears in this list, notifications will have their flag set to the given value.  Flags that do not appear will be unset.
     * Tag - a string, the name of the flag
     * isSet - a boolean, true if the flag should be set, false if it should be unset
   * Text - the content of the notification
 * *Return Values:*
   * None

### Creation and Deletion

Finally a user can request access to the testbed by creating a profile.  The user has no privileges and consumes minimal resources until they join a vetted project.  This is an unauthenticated call, but the user profile is not created until an automated e-mail exchange is made between the testbed and the proto-user.

 * *Service:* Users
 * *Operation:* createUser
 * *Input Parameters:*
   * Userid - the requested userid (optional)
   * A list of profile elements each containing
     * Name - a string, the element\'s name
     * StringValue - a string containing the element\'s value
 * *Return Values:*
  * The userid actually created

Note that all non-optional fields must be provided, so this is best preceeded by a call to getProfileDescription to learn the fields.

A base userid is constructed, either from the userid presented or from a required field in the profile.  If that base userid is available, that a user with that userid is created.  Otherwise the userid is disambiguated until there are no collisions.  The userid actually created is returned.

The user is created without membership in any projects and without a password.  A password reset challenge (equivalent to one issued by requestPasswordReset) is issued and the challenge e-mailed to the user at the address in the profile.

Administrators can create users without the confirmation step:

 * *Service:* Users
 * *Operation:* createUserNoConfirm
 * *Input Parameters:*
   * Userid - the requested userid (optional)
   * A list of profile elements each containing
     * Name - a string, the element\'s name
     * StringValue - a string containing the element\'s value, unless it is binary/opaque
   * clearpassword - an optional string given the cleartext password
   * hash - an optional string containing a hashed password
   * hashtype - an optional string giving the type of hash
 * *Return Values:*
  * The userid actually created

Note that either the cleartext password or the hash and type must be passed.  This interface is under for initializing the testbed or creating users in batch for something like a class.


Removing a user is accomplished using
 * *Service:* Users
 * *Operation:* removeUser
 * *Input Parameters:*
   * Userid - the requested userid
 * *Return Values:*
  * a boolean, true if the user was removed

Most users are unlikely to use this service, and policy may allow only administrators to remove users.

### Manipulating Profile Attributes

Administrators may add attributes to user profiles or remove them.  That is to say, add a schema (including format constraints and descriptions), not set a value for a user.

Creating an attribute:

* *Service:* Users
 * *Operation:* createUserAttribute
 * *Input Parameters:*
   * Name - the attribute name
   * Type - the type (STRING, INT, FLOAT, OPAQUE)
   * Optional - a boolean, true if this attribute is optional
   * Access - a string: the user\'s ability to modify (READ_WRITE, READ_ONLY, WRITE_ONLY, NO_ACCESS)
   * Description - natural language description of the field (optional)
   * Format - a regular expression describing the format (optional)
   * Formatdescription - a natural language explanation of the format (optional)
   * OrderingHint - an integer suggesting where to present the attribute. Lower numbers come first
   * LengthHint - an integer suggesting how long an input 
   * Def - default value of the attribute (will be set for all users)
 * *Return Values:*
  * a boolean, true if the attribute was created 

And removing an attribute:

* *Service:* Users
 * *Operation:* removeUserAttribute
 * *Input Parameters:*
   * Name - the attribute name
 * *Return Values:*
  * a boolean, true if the attribute was removed

Finally administrators can modify profile attributes:

* *Service:* Users
 * *Operation:* modifyUserAttribute
 * *Input Parameters:*
   * Name - the attribute name
   * Type - the type (STRING, INT, FLOAT, OPAQUE)
   * Optional - a boolean, true if this attribute is optional
   * Access - a string: the user\'s ability to modify (READ_WRITE, READ_ONLY, WRITE_ONLY, NO_ACCESS)
   * Description - natural language description of the field (optional)
   * Format - a regular expression describing the format (optional)
   * Formatdescription - a natural language explanation of the format (optional)
   * OrderingHint - an integer suggesting where to present the attribute. Lower numbers come first
   * LengthHint - an integer suggesting how long an input 
 * *Return Values:*
  * a boolean, true if the attribute was created 




## Projects

The profile API lets users manage their project memberships as well as manipulate projects.

[http://www.isi.edu/~faber/tmp/DeterAPI/doc/net/deterlab/testbed/api/Projects.html Projects javadocs]

### Listing Projects

Authenticated users can look at the various projects they are members of, and may scope such searcher by project names.

 * *Service:* Projects
 * *Operation:* viewProjects
 * *Input Parameters:*
   * Userid - the requester\'s userid
   * Owner - a userid - only find projects owned by this user
   * NameRE - a string containing a regular expression to match against project names
 * *Return Values:*
   * A list of project descriptors containing
     * Name - a string contaiing the project name
     * Owner - the uid of the project owner
     * Members - an array of structures containing
       * Userid - a member\'s userid
       * Permissions - a list of strings encoding the user\'s rights (each a flag, if present the user has the right)
         * ADD_USER - right to add a user to this project
         * REMOVE_USER - right to remove a user from this project
         * CREATE_CIRCLE - the right to create a circle under this project\'s name space
     * Approved - a flag indicating this project has been approved

Filters are cumulative: specifying a regular expression and an owner will match on both.

### Changing Project Membership

Each of these changes propagates to the linked circle that controls resource permissions for the project. In particular, the permissions fields should include the union of the project and circle rights to set

With appropriate rights a user can add another user to a project they are in. 
The user being added must confirm this addition (below).  As a result of this call, users being added will be sent a notification including a challenge id to respond to, similar to a password change URL.  The prefix for that URL can be passed in here.

 * *Service:* Projects
 * *Operation:* addUsers
 * *Input Parameters:*
   * ProjectID - the project 
   * Uids - the userids to add
   * Perms - a list of strings giving the permissions to grant the user in this project.  The values are the same as in viewProjects.
   * urlPrefix - a prefix to direct the user to a specific web interface to respond to the challenge.
 * *Return Values:*
   * A list of results of the form
     * Name - the uid being added
     * Success - a flag indicating success or failure
     * Reason - the reason for a failure

For each user successfully added, a notification is sent.  To respond to that notification the UI must call:

* *Service:* Projects
 * *Operation:* addUserConfirm
 * *Input Parameters:*
   * ChallengeID - the challenge being responded to
 * *Return Values:*
   * true if the user was added.  A fault is thrown on errors.

Once the confirmation is received, the user is actually a member of the project.  If challenges are not responded to in 48 hours, they are no longer valid.

Administrators can add users without going through the confirmation process:

* *Service:* Projects
 * *Operation:* addUsersNoConfirm
 * *Input Parameters:*
   * ProjectID - the project 
   * Uids - the userids to add
   * Perms - a list of strings that define the user\'s rights.  They are the same strings as in viewProjects.
 * *Return Values:*
   * A list of results of the form
     * Name - the uid being added
     * Success - a flag indicating success or failure
     * Reason - the reason for a failure

Users can also request to join a project.  This requires some member of the project with ADD_USER rights to agree, so a notification is sent to all such users.

 * *Service:* Projects
 * *Operation:* joinProject
 * *Input Parameters:*
   * Uid - the uid to add
   * ProjectID - the project to join
   * urlPrefix - a prefix to direct the user to a specific web interface to respond to the challenge.
 * *Return Values:*
   * A boolean, true if the join request was sent

The join succeeds when a user with appropriate permissions calls joinProjectConfirm.
Note that the user calling joinProjectConfirm sets the new user\'s project permissions.

* *Service:* Projects
 * *Operation:* joinProjectConfirm
 * *Input Parameters:*
   * ChallengeID - the challenge being responded to
   * Perms - a list of strings defining the permissions to grant the user in this project.  The values are as in viewProjects.
 * *Return Values:*
   * true if the user was successfully added.  A fault is thrown on errors.


Users can be removed from projects:

 * *Service:* Projects
 * *Operation:* removeUsers
 * *Input Parameters:*
   * ProjectName - the project to change
   * Uids - uids to remove
 * *Return Values:*
   * A list of results of the form
     * Name - the uid being added
     * Success - a flag indicating success or failure
     * Reason - the reason for a failure

If a user has the right to both add and remove a user, that user can change a user\'s permissions:

 * *Service:* Projects
 * *Operation:* changePermissions
 * *Input Parameters:*
   * ProjectName - the project to change
   * Uids - the uids to manipulate
   * Permissions - a list of strings encoding the user\'s rights (each a flag - the user is assigned the right/permission if the string is present).  The values are as in viewProjects.
 * *Return Values:*
   * A list of results of the form
     * Name - the uid being added
     * Success - a flag indicating success or failure
     * Reason - the reason for a failure

### Manipulating Projects

The owner of a group (and only the owner of that group) can give that privilege away:

 * *Service:* Projects
 * *Operation:* setOwner
 * *Input Parameters:*
   * Userid - the requester\'s userid
   * ProjectName - the project to change
   * NewOwner - the uid of the new owner
 * *Return Values:*
   * None

### Project Profiles

The calls for manipulating a project profile are very similar to user profiles:

To get a profile schema, for example to create an empty web page,

 * *Service:* Projects
 * *Operation:* getProfileDescription
 * *Input Parameters:*
 * *Return Values:*
   * Projectid - always empty
   * A list of profile elements each containing
     * Name - a string, the element\'s name
     * DataType - a string giving the element\'s
       * string
       * integer
       * double
       * binary/opaque
     * Value - a string containing the element\'s value
     * Access - a string describing the access values (values [http://www.isi.edu/~faber/tmp/DeterAPI/doc/constant-values.html here])
     * Optional - a flag true if the field is optional (must be present but may be empty)
     * Removable - a flag true if the field can be removed
     * Description - a string explaining the field
     * Format - a regular expression that can be used to validate the field entry (may be null, and generally is for optional fields)
     * FormatDescription - A brief, natural language description of the field input constraints, e.g. "A valid e-mail address" or "only numbers and spaces".
     * OrderingHint - an integer suggesting where to present the attribute. Lower numbers come first
     * LengthHint - an integer suggesting how long an input firld should be presented

To read a project\'s profile.

 * *Service:* Projects
 * *Operation:* getProjectProfile
 * *Input Parameters:*
  * Projectid - a string naming the project to be retrieved
 * *Return Values:*
   * Projectid - the project whose profile is returned
   * A list of profile elements each containing
     * Name - a string, the element\'s name
     * DataType - a string giving the element\'s
       * string
       * integer
       * double
       * binary/opaque
     * Value - a string containing the element\'s value
     * Access - a string describing the access values (Defined [http://www.isi.edu/~faber/tmp/DeterAPI/doc/constant-values.html here])
     * Optional - a flag true if the field is optional (must be present but may be empty)
     * Removable - a flag true if the field can be removed
     * Description - a string explaining the field
     * Format - a regular expression that can be used to validate the field entry (may be null, and generally is for optional fields)
     * FormatDescription - A brief, natural language description of the field input constraints, e.g. "A valid e-mail address" or "only numbers and spaces".
     * OrderingHint - an integer suggesting where to present the attribute. Lower numbers come first
     * LengthHint - an integer suggesting how long an input field should be presented

Finally a user can modify a project profile:
 
 * *Service:* Projects
 * *Operation:* changeProjectProfile
 * *Input Parameters:*
   * ProjectId - the project\'s profile to modify
   * A list of change requests. Each request contains
     * Name - the name of the field to change
     * Value - the new value of the field
     * Delete - a flag, if true delete the field rather than modify it
 * *Return Values:*
   * A list of responses each containing
    * Name - astring with the name of the field
    * Success - a flag indicating if the request succeeded
    * Reason - a string indicating the reason if Success is false


### Manipulating Profile Attributes

Administrators may add attributes to project profiles or remove them.  That is to say, add a schema (including format constraints and descriptions), not set a value for a project.

Creating an attribute:

* *Service:* Projects
 * *Operation:* createProjectAttribute
 * *Input Parameters:*
   * Name - the attribute name
   * Type - the type (STRING, INT, FLOAT, OPAQUE)
   * Optional - a boolean, true if this attribute is optional
   * Access - a string: the user\'s ability to modify (READ_WRITE, READ_ONLY, WRITE_ONLY, NO_ACCESS)
   * Description - natural language description of the field (optional)
   * Format - a regular expression describing the format (optional)
   * Formatdescription - a natural language explanation of the format (optional)
   * OrderingHint - an integer suggesting where to present the attribute. Lower numbers come first
   * LengthHint - an integer suggesting how long an input 
   * Def - default value of the attribute (will be set for all users)
 * *Return Values:*
  * a boolean, true if the attribute was created 

And removing an attribute:

* *Service:* Projects
 * *Operation:* removeProjectAttribute
 * *Input Parameters:*
   * Name - the attribute name
 * *Return Values:*
  * a boolean, true if the attribute was removed 

Finally administrators can modify profile attributes:

* *Service:* Projects
 * *Operation:* modifyProjectAttribute
 * *Input Parameters:*
   * Name - the attribute name
   * Type - the type (STRING, INT, FLOAT, OPAQUE)
   * Optional - a boolean, true if this attribute is optional
   * Access - a string: the user\'s ability to modify (READ_WRITE, READ_ONLY, WRITE_ONLY, NO_ACCESS)
   * Description - natural language description of the field (optional)
   * Format - a regular expression describing the format (optional)
   * Formatdescription - a natural language explanation of the format (optional)
   * OrderingHint - an integer suggesting where to present the attribute. Lower numbers come first
   * LengthHint - an integer suggesting how long an input 
 * *Return Values:*
  * a boolean, true if the attribute was created 


### Creating and Deleting Projects

Creating and removing projects are also similar to creating and removing users:

 * *Service:* Projects
 * *Operation:* createProject
 * *Input Parameters:*
   * ProjectId - the new project\'s name
   * Owner - the new owner\'s uid (usually the caller)
   * profile - a list of attributes that will make up the project\'s profile
   * A list of profile elements each containing
     * Name - a string, the element\'s name
     * StringValue - a string containing the element\'s value
 * *Return Values:*
   * None

As with users, all required (non-optional) fields in the profile must be provided.  The project will be created, but have no rights until approved.

On success the new project is created with the calling user as owner and sole member.  The user has all rights.


Approval is a result of the testbed\'s vetting process.  The user will be sent a notification when their project is approved.  When approved this call is made by an admin.

 * *Service:* Projects
 * *Operation:* approveProject
 * *Input Parameters:*
   * ProjectID - the project\'s name
   * approved - a boolean, the new approval state
 * *Return Values:*
   * None

Note that this call can be used to un-approve a previously approved project.

Removing a project is straightforward

 * *Service:* Projects
 * *Operation:* removeProject
 * *Input Parameters:*
   * ProjectId - the requester\'s userid
 * *Return Values:*
   * A boolean, true if the approval succeeded

Generally an removing a project requires a testbed administrator.

## Circles

The circle API lets users manage their circle memberships as well as manipulate circles.  The circles API is similar to the Projects API.  The primary exceptions are that there is no approval process for circles (though there is for joining or adding a user) and that the permissions are different.

These calls work on all circles *except* those linked to a project.  Those circles must have their membership, ownership, and permissions manipulated through the project to ensure that the contents remain synchronized.

[http://www.isi.edu/~faber/tmp/DeterAPI/doc/net/deterlab/testbed/api/Circles.html circles javadoc]

### Listing Circles

Authenticated users can look at the various circles they are members of, and may scope such searcher by circle names.

 * *Service:* Circles
 * *Operation:* viewCircles
 * *Input Parameters:*
   * Userid - the requester\'s userid
   * Owner - a userid - only find circles owned by this user
   * NameRE - a string containing a regular expression to match against circle names
 * *Return Values:*
   * A list of circle descriptors containing
     * Name - a string contaiing the circle name
     * Owner - the uid of the circle owner
     * Members - an array of structures containing
       * Userid - a member\'s userid
       * permissions - a list of strings encoding the user\'s rights (each a flag - the user has the permission if the string is present)
         * ADD_USER - right to add a user to this circle
         * REMOVE_USER - right to remove a user from this circle
         * REALIZE_EXPERIMENT - the right to realize an experiment under this circle

Filters are cumulative: specifying a regular expression and an owner will match on both.

### Changing Circle Membership

With appropriate rights a user can add another user to a circle they are in.
The user being added must confirm this addition (below).  As a result of this call, users being added will be sent a notification including a challenge id to respond to, similar to a password change URL.  The prefix for that URL can be passed in here.

 * *Service:* Circles
 * *Operation:* addUsers
 * *Input Parameters:*
   * CircleID - the circle
   * Uids - the userids to add
   * Perms - a list of strings that give the permissions to grant the user in this circle.  The values are the same as described in viewCircles.
   * urlPrefix - a prefix to direct the user to a specific web interface to respond to the challenge.
 * *Return Values:*
   * A list of results of the form
     * Name - the uid being added
     * Success - a flag indicating success or failure
     * Reason - the reason for a failure

For each user successfully added, a notification is sent.  To respond to that notification the UI must call:

* *Service:* Circles
 * *Operation:* addUserConfirm
 * *Input Parameters:*
   * ChallengeID - the challenge being responded to
 * *Return Values:*
   * true if the user was added.  A fault is thrown on errors.

Administrators can add users without going through the confirmation process:

* *Service:* Circles
 * *Operation:* addUsersNoConfirm
 * *Input Parameters:*
   * CircleID - the circle
   * Uids - the userids to add
   * Perms - a list of strings giving the permissions to grant the user in this circle.  The values are given the same as described in viewCircles.
 * *Return Values:*
   * A list of results of the form
     * Name - the uid being added
     * Success - a flag indicating success or failure
     * Reason - the reason for a failure

Users can also request to join a circle.  This requires some member of the circle with ADD_USER rights to agree, so a notification is sent to all such users.

 * *Service:* Circles
 * *Operation:* joinCircle
 * *Input Parameters:*
   * Uid - the uid to add
   * CircleID - the circle to join
   * urlPrefix - a prefix to direct the user to a specific web interface to respond to the challenge.
 * *Return Values:*
   * A boolean, true if the join notification was sent

The join succeeds when a user with appropriate permissions calls joinCircleConfirm.
Note that the user calling joinCircleConfirm sets the new user\'s circle permissions.

* *Service:* Circles
 * *Operation:* joinCircleConfirm
 * *Input Parameters:*
   * ChallengeID - the challenge being responded to
   * Perms - a list of strings giving the permissions to grant the user in this circle.  The values are as described in viewCircles.
 * *Return Values:*
   * true if the user was subsequently added.  A fault is thrown on errors.


Users can be removed from circles:

 * *Service:* Circles
 * *Operation:* removeUsers
 * *Input Parameters:*
   * CircleName - the circle to change
   * Uids - uids to remove
 * *Return Values:*
   * A list of results of the form
     * Name - the uid being added
     * Success - a flag indicating success or failure
     * Reason - the reason for a failure

If a user has the right to both add and remove a user, that user can change a user\'s permissions:

 * *Service:* Circles
 * *Operation:* changePermissions
 * *Input Parameters:*
   * CircleName - the circle to change
   * Uids - the uids to manipulate
   * Rights - a list of strings encoding the user\'s rights (each a flag).  Values are as given in viewCircles.  If the string is present, the user is granted the permission.
 * *Return Values:*
   * A list of results of the form
     * Name - the uid being added
     * Success - a flag indicating success or failure
     * Reason - the reason for a failure

### Manipulating Circles

The owner of a group (and only the owner of that group) can give that privilege away to another member:

 * *Service:* Circles
 * *Operation:* setOwner
 * *Input Parameters:*
   * Userid - the requester\'s userid
   * CircleName - the circle to change
   * NewOwner - the uid of the new owner
 * *Return Values:*
   * None

### Circle Profiles

The calls for manipulating a circle profile are very similar to user profiles:

To get a profile schema, for example to create an empty web page,

 * *Service:* Circles
 * *Operation:* getProfileDescription
 * *Input Parameters:*
 * *Return Values:*
   * Circleid - always empty
   * A list of profile elements each containing
     * Name - a string, the element\'s name
     * DataType - a string giving the element\'s
       * string
       * integer
       * double
       * binary/opaque
     * Value - a string containing the element\'s value
     * Access - a string describing the access values (values [http://www.isi.edu/~faber/tmp/DeterAPI/doc/constant-values.html here])
     * Optional - a flag true if the field is optional (must be present but may be empty)
     * Removable - a flag true if the field can be removed
     * Description - a string explaining the field
     * Format - a regular expression that can be used to validate the field entry (may be null, and generally is for optional fields)
     * FormatDescription - A brief, natural language description of the field input constraints, e.g. "A valid e-mail address" or "only numbers and spaces".
     * OrderingHint - an integer suggesting where to present the attribute. Lower numbers come first
     * LengthHint - an integer suggesting how long an input firld should be presented

To read a circle\'s profile.

 * *Service:* Circles
 * *Operation:* getCircleProfile
 * *Input Parameters:*
  * userid - a string naming the user to be retrieved
 * *Return Values:*
   * Circleid - the user whose profile is returned
   * A list of profile elements each containing
     * Name - a string, the element\'s name
     * DataType - a string giving the element\'s
       * string
       * integer
       * double
       * binary/opaque
     * Value - a string containing the element\'s value
     * Access - a string  describing the access values (values [http://www.isi.edu/~faber/tmp/DeterAPI/doc/constant-values.html here])
       * READ_WRITE
       * READ_ONLY
       * WRITE_ONLY (e.g., password)
     * Optional - a flag true if the field is optional (must be present but may be empty)
     * Removable - a flag true if the field can be removed
     * Description - a string explaining the field
     * Format - a regular expression that can be used to validate the field entry (may be null, and generally is for optional fields)
     * FormatDescription - A brief, natural language description of the field input constraints, e.g. "A valid e-mail address" or "only numbers and spaces".
     * OrderingHint - an integer suggesting where to present the attribute. Lower numbers come first
     * LengthHint - an integer suggesting how long an input field should be presented

Finally a user can modify a circle profile:

 * *Service:* Circles
 * *Operation:* changeCircleProfile
 * *Input Parameters:*
   * CircleId - the circle\'s profile to modify
   * A list of change requests. Each request contains
     * Name - the name of the field to change
     * Value - the new value of the field
     * Delete - a flag, if true delete the field rather than modify it
 * *Return Values:*
   * A list of responses each containing
    * Name - astring with the name of the field
    * Success - a flag indicating if the request succeeded
    * Reason - a string indicating the reason if Success is false


### Manipulating Profile Attributes

Administrators may add attributes to circle profiles or remove them.  That is to say, add a schema (including format constraints and descriptions), not set a value for a circle.
Creating an attribute:

* *Service:* Circles
 * *Operation:* createCircleAttribute
 * *Input Parameters:*
   * Name - the attribute name
   * Type - the type (STRING, INT, FLOAT, OPAQUE)
   * Optional - a boolean, true if this attribute is optional
   * Access - a string: the user\'s ability to modify (READ_WRITE, READ_ONLY, WRITE_ONLY, NO_ACCESS)
   * Description - natural language description of the field (optional)
   * Format - a regular expression describing the format (optional)
   * Formatdescription - a natural language explanation of the format (optional)
   * OrderingHint - an integer suggesting where to present the attribute. Lower numbers come first
   * LengthHint - an integer suggesting how long an input 
   * Def - default value of the attribute (will be set for all users)
 * *Return Values:*
  * a boolean, true if the attribute was created 

And removing an attribute:

* *Service:* Circles
 * *Operation:* removeCircleAttribute
 * *Input Parameters:*
   * Name - the attribute name
 * *Return Values:*
  * a boolean, true if the attribute was removed 

Finally administrators can modify profile attributes:

* *Service:* Circles
 * *Operation:* modifyCircleAttribute
 * *Input Parameters:*
   * Name - the attribute name
   * Type - the type (STRING, INT, FLOAT, OPAQUE)
   * Optional - a boolean, true if this attribute is optional
   * Access - a string: the user\'s ability to modify (READ_WRITE, READ_ONLY, WRITE_ONLY, NO_ACCESS)
   * Description - natural language description of the field (optional)
   * Format - a regular expression describing the format (optional)
   * Formatdescription - a natural language explanation of the format (optional)
   * OrderingHint - an integer suggesting where to present the attribute. Lower numbers come first
   * LengthHint - an integer suggesting how long an input
 * *Return Values:*
  * a boolean, true if the attribute was created


### Creating and Deleting Circles

Creating and removing circles are also similar to creating and removing users:

 * *Service:* Circles
 * *Operation:* createCircle
 * *Input Parameters:*
   * CircleId - the new circle\'s name
   * Uid - the new owner\'s uid (usually the caller)
   * profile - a list of attributes that will make up the circle\'s profile
   * A list of profile elements each containing
     * Name - a string, the element\'s name
     * StringValue - a string containing the element\'s value
 * *Return Values:*
   * None

As with users, all required (non-optional) fields in the profile must be provided.  The circle will be created, and can be used immediately.

Removing a circle is straightforward.

 * *Service:* Circles
 * *Operation:* removeCircle
 * *Input Parameters:*
   * Userid - the requester\'s userid
   * Name - the new circle\'s name
 * *Return Values:*
   * None

## Experiments

The experiment interface controls managing experiment definitions and realizing experiments (allocating and initializing resources). The experiment definition is in some flux, which is reflected in this section.

An experiment is a collection of aspects, each of which controls part of the experiment\'s environment and operation.  Some aspects are understood directly by the testbed, while others are interpreted and accessed by tolls inside the experiment.  Such tools may be written by DETER administrators or by experimenters themselves.

An [http://www.isi.edu/~faber/tmp/DeterAPI/doc/net/deterlab/testbed/api/ExperimentAspect.html aspect] is a named and typed object associated with the the experiment.  Specifically, an aspect has the following attributes:

*  name
   A unique string scoped by aspect type used to identify the aspect instance
*  type
   A string that tells the testbed or tool how to interpret the aspect.  The current interface interprets *topology* aspects as an experiment layout, and treats the rest as opaque.
*  subtype
   A string characterizing the aspect further. Subtypes may be created by the testbed or tools.
*  data
   The contents of the aspect.  This will be empty if datareference has a value
*  datareference
   A URI that points to the contents of this aspect
*  readOnly
   A boolean, true if the aspect can be modified.

Each experiment also includes an access control list (ACL) that lists the permissions that members of a given circle are granted to this experiment.

[http://www.isi.edu/~faber/tmp/DeterAPI/doc/net/deterlab/testbed/api/Experiments.html experiments javadoc]

### Viewing Experiments

One or more experiments can be viewed by an authorized user assuming that user has the proper permissions.  The search can be scoped by library and a regular expression matched against the experiment name.  In addition, the aspects returned and their contents can also be controlled.  To view experiments, call:

 * *Service:* Experiments
 * *Operation:* viewExperiments
 * *Input Parameters:*
   * Userid - a string, if given return all experiments this user can see
   * Lib - a string, the name of a library.  Only experiments in that library will be returned
   * Regex - a string containing a regular expression matched against experiment names
   * QueryAspects - an array of aspects used to scope the aspect search  Each aspect has the fields described above.  The readOnly, data, and dataReference fields are all ignored.   If the name is present, it much match. If the type is given it must also match. If a type is given the subtype field further scopes the search. A missing type field selects aspects without any subtype. A specific subtype selects only aspects that match both type and subtype, and the distinguished value "*" matches any subtype. A subtype with a null type is invalid.
   * listOnly - a boolean, true if the aspects returned should not contain data.  This avoids unnecessary large data transfers
 * *Return Values:*
   * One or more structures with the following fields
     * Name - a string containing the experiment name
     * Owner - a string containing the owner\'s userid
     * Aspects - a list of aspect structures as defined above
     * ACL - a list of structures of the form
       * Name - a string the circle name
       * Permissions - a list of strings containing this circle\'s permissions.  Permissions are from [http://www.isi.edu/~faber/tmp/DeterAPI/doc/constant-values.html#net.deterlab.testbed.api.Permissions this table]


### Creating and Deleting Experiments

An experiment is created using:

 * *Service:* Experiments
 * *Operation:* createExperiment
 * *Input Parameters:*
   * Name - a string containing a new experiment\'s name
   * Userid - the user making the request (the owner on success)
   * Aspects - a list of aspects as defined above
   * AccessLists - a list of access list entries - a list of structures of the form
       * Name - a string the circle name
       * Permissions - a list of strings containing this circle\'s permissions.Permissions are from [http://www.isi.edu/~faber/tmp/DeterAPI/doc/constant-values.html#net.deterlab.testbed.api.Permissions this table] 
   * Profile - a list of profile attribute value pairs
 * *Return Values:*
   * a boolean, true if the creation succeeded

Creating an experiment with one aspect may result in additional sub-aspects appearing, as well as modifications to other aspects.

An experiment is deleted using:
 
 * *Service:* Experiments
 * *Operation:* removeExperiment
 * *Input Parameters:*
   * Name - a string containing the experiment to delete
 * *Return Values:*
   * a boolean, true on success

After `removeExperiment` succeeds, experiment state is removed from the testbed completely.

### Modifying Experiments

Because aspects are somewhat free-form and can be interrelated, the interface for manipulating them is simple.  Aspects can be added and removed.

* *Service:* Experiments
 * *Operation:* addExperimentAspects
 * *Input Parameters:*
   * Name - a string containing the experiment\'s name
   * Aspects - a list of aspects as defined above
 * *Return Values:*
   * a boolean, true if the addition succeeded

As with creation, adding an aspect may result in additional sub-aspects appearing, as well as modifications to other aspects.

* *Service:* Experiments
 * *Operation:* removeExperimentAspects
 * *Input Parameters:*
   * Name - a string containing the experiment\'s name
   * Aspects - a list of aspects as defined above
 * *Return Values:*
   * a boolean, true if the removal succeeded

As with creation, adding an aspect may result in additional sub-aspects appearing, as well as modifications to other aspects.

* *Service:* Experiments
 * *Operation:* addExperimentACL
 * *Input Parameters:*
   * Name - a string containing the experiment\'s name
   * AccessLists - a list of access list entries - a list of structures of the form
       * Name - a string the circle name
       * Permissions - a list of strings containing this circle\'s permissions.Permissions are from [http://www.isi.edu/~faber/tmp/DeterAPI/doc/constant-values.html#net.deterlab.testbed.api.Permissions this table]
 * *Return Values:*
   * a boolean, true if the removal succeeded

* *Service:* Experiments
 * *Operation:* removeExperimentACL
 * *Input Parameters:*
   * Name - a string containing the experiment\'s name
   * AccessLists - a list circles to remove from the ACL
 * *Return Values:*
   * a boolean, true if the removal succeeded

The owner can give ownership to another user.

* *Service:* Experiments
 * *Operation:* setOwner
 * *Input Parameters:*
   * Name - a string containing the experiment\'s name
   * newOwner - a string, the new owner\'s UID
 * *Return Values:*
   * a boolean, true if the removal succeeded


We expect to add filter-based interfaces in the future as well.

### Realizing and Releasing Experiments

To realize an experiment use:


 * *Service:* Experiments
 * *Operation:* realizeExperiment
 * *Input Parameters:*
   * Userid - the user making the request (the owner on success)
   * Eid - the experiment to realize
   * Cid - the circle under which the experiment is being created
   * AccessLists - a list of access list entries - a list of structures of the form
       * Name - a string the circle name
       * Permissions - a list of strings containing this circle\'s permissions.Permissions are from [http://www.isi.edu/~faber/tmp/DeterAPI/doc/constant-values.html#net.deterlab.testbed.api.Permissions this table]
   * SendNotifications - an optional boolean.  If present and true, the system will use the notification system to inform the user of status changes. 
 * *Return Values:*
   * One structures with the following fields
     * Name - a string containing the realization name (scoped by owner name)
     * Experiment - a string containing the realized experiment
     * Circle - the circle that the experiment is realized in
     * Status - one of:
       * Empty
       * Allocated
       * Initializing
       * Active
       * Terminating
     * Containment - a list of structures of the format
       * Outer resource name - the name of the containing resource
       * Inner resource name - the name of the contained resource
     * Mapping - a list of structures of the format
       * Element name - the name of the element from the experiment\'s layout aspect
       * Resource name - the name of the resource to which the element is mapped

 
### Experiment Profiles

Experiments have profiles attached to them, as users, circles, and projects do.  Information in a profile is metadata about the experiment, intended for human consumption.  This is in opposition to an aspect which is intended for the testbed or experiment infrastructure to interpret.

The calls for manipulating an experiment profile are very similar to other profiles:

To get a profile schema, for example to create an empty web page,

 * *Service:* Experiments
 * *Operation:* getProfileDescription
 * *Input Parameters:*
 * *Return Values:*
   * ExperimentId - always empty
   * A list of profile elements each containing
     * Name - a string, the element\'s name
     * DataType - a string giving the element\'s
       * string
       * integer
       * double
       * binary/opaque
     * Value - a string containing the element\'s value
     * Access - a string describing the access values (values [http://www.isi.edu/~faber/tmp/DeterAPI/doc/constant-values.html here])
     * Optional - a flag true if the field is optional (must be present but may be empty)
     * Removable - a flag true if the field can be removed
     * Description - a string explaining the field
     * Format - a regular expression that can be used to validate the field entry (may be null, and generally is for optional fields)
     * FormatDescription - A brief, natural language description of the field input constraints, e.g. "A valid e-mail address" or "only numbers and spaces".
     * OrderingHint - an integer suggesting where to present the attribute. Lower numbers come first
     * LengthHint - an integer suggesting how long an input firld should be presented

To read an experiment\'s profile.

 * *Service:* Experiments
 * *Operation:* getExperimentProfile
 * *Input Parameters:*
  * eid - a string naming the experiment to be retrieved
 * *Return Values:*
   * ExperimentId - the experiment whose profile is returned
   * A list of profile elements each containing
     * Name - a string, the element\'s name
     * DataType - a string giving the element\'s
       * string
       * integer
       * double
       * binary/opaque
     * Value - a string containing the element\'s value
     * Access - a string describing the access values (Defined [http://www.isi.edu/~faber/tmp/DeterAPI/doc/constant-values.html here])
     * Optional - a flag true if the field is optional (must be present but may be empty)
     * Removable - a flag true if the field can be removed
     * Description - a string explaining the field
     * Format - a regular expression that can be used to validate the field entry (may be null, and generally is for optional fields)
     * FormatDescription - A brief, natural language description of the field input constraints, e.g. "A valid e-mail address" or "only numbers and spaces".
     * OrderingHint - an integer suggesting where to present the attribute. Lower numbers come first
     * LengthHint - an integer suggesting how long an input field should be presented

Finally a user can modify an experiment profile:
 
 * *Service:* Experiments
 * *Operation:* changeExperimentProfile
 * *Input Parameters:*
   * Eid - the experiment\'s profile to modify
   * A list of change requests. Each request contains
     * Name - the name of the field to change
     * Value - the new value of the field
     * Delete - a flag, if true delete the field rather than modify it
 * *Return Values:*
   * A list of responses each containing
    * Name - astring with the name of the field
    * Success - a flag indicating if the request succeeded
    * Reason - a string indicating the reason if Success is false


### Manipulating Profile Attributes

Administrators may add attributes to experiment profiles or remove them.  That is to say, add a schema (including format constraints and descriptions), not set a value for an experiment.

Creating an attribute:

* *Service:* Experiments
 * *Operation:* createExperimentAttribute
 * *Input Parameters:*
   * Name - the attribute name
   * Type - the type (STRING, INT, FLOAT, OPAQUE)
   * Optional - a boolean, true if this attribute is optional
   * Access - a string: the user\'s ability to modify (READ_WRITE, READ_ONLY, WRITE_ONLY, NO_ACCESS)
   * Description - natural language description of the field (optional)
   * Format - a regular expression describing the format (optional)
   * Formatdescription - a natural language explanation of the format (optional)
   * OrderingHint - an integer suggesting where to present the attribute. Lower numbers come first
   * LengthHint - an integer suggesting how long an input 
   * Def - default value of the attribute (will be set for all users)
 * *Return Values:*
  * a boolean, true if the attribute was created 

And removing an attribute:

* *Service:* Experiments
 * *Operation:* removeExperimentAttribute
 * *Input Parameters:*
   * Name - the attribute name
 * *Return Values:*
  * a boolean, true if the attribute was removed 

Finally administrators can modify profile attributes:

* *Service:* Experiments
 * *Operation:* modifyExperimentAttribute
 * *Input Parameters:*
   * Name - the attribute name
   * Type - the type (STRING, INT, FLOAT, OPAQUE)
   * Optional - a boolean, true if this attribute is optional
   * Access - a string: the user\'s ability to modify (READ_WRITE, READ_ONLY, WRITE_ONLY, NO_ACCESS)
   * Description - natural language description of the field (optional)
   * Format - a regular expression describing the format (optional)
   * Formatdescription - a natural language explanation of the format (optional)
   * OrderingHint - an integer suggesting where to present the attribute. Lower numbers come first
   * LengthHint - an integer suggesting how long an input 
 * *Return Values:*
  * a boolean, true if the attribute was created 


## Libraries

The library interface controls managing library contents. The experiment definition is in some flux, which is reflected in this section.

An library is a collection of experiments, each of which can be manipulated by users with the proper permissions.  By grouping experiments it is easier to collaborate on the experiments as well as providing pools of experiments to use as starting points for new users.

Each library includes an access control list (ACL) that lists the permissions that mebmers of a given circle are granted to the library.


### Viewing Libraries

One or more libraries can be viewed by an authorized user assuming that user has the proper permissions.  The search can be a regular expression matched against the library name.  To view libraries, call:

 * *Service:* Libraries
 * *Operation:* viewLibraries
 * *Input Parameters:*
   * Userid - a string, if given return all experiments this user can see
   * Regex - a string containing a regular expression matched against experiment names
 * *Return Values:*
   * One or more structures with the following fields
     * Name - a string containing the experiment name
     * Owner - a string containing the owner\'s userid
     * Experiments - a list of experiment names 
     * ACL - a list of structures of the form
       * Name - a string the circle name
       * Permissions - a list of strings containing this circle\'s permissions.  Permissions are from [http://www.isi.edu/~faber/tmp/DeterAPI/doc/constant-values.html#net.deterlab.testbed.api.Permissions this table]


### Creating and Deleting Libraries

A library is created using:

 * *Service:* Libraries
 * *Operation:* createLibrary
 * *Input Parameters:*
   * Name - a string containing a new experiment\'s name
   * Userid - the user making the request (the owner on success)
   * Experiments - a list of experiments in the library
   * AccessLists - a list of access list entries - a list of structures of the form
       * Name - a string the circle name
       * Permissions - a list of strings containing this circle\'s permissions.Permissions are from [http://www.isi.edu/~faber/tmp/DeterAPI/doc/constant-values.html#net.deterlab.testbed.api.Permissions this table] 
   * Profile - a list of profile attribute value pairs
 * *Return Values:*
   * a boolean, true if the creation succeeded

A library is deleted using:
 
 * *Service:* Library
 * *Operation:* removeLibrary
 * *Input Parameters:*
   * Name - a string containing the library to delete
 * *Return Values:*
   * a boolean, true on success

After `removeLibrary` succeeds, library is removed from the testbed completely.

### Modifying Libraries

Experiments can be added and removed from libraries

* *Service:* Libraries
 * *Operation:* addLibraryExperiment
 * *Input Parameters:*
   * Name - a string containing the library\'s name
   * Experiments - a list of experiment names
 * *Return Values:*
   * a boolean, true if the addition succeeded

As with creation, adding an aspect may result in additional sub-aspects appearing, as well as modifications to other aspects.

* *Service:* Libraries
 * *Operation:* removeLibraryExperiments
 * *Input Parameters:*
   * Name - a string containing the experiment\'s name
   * Experiments - a list of experiments to remove
 * *Return Values:*
   * a boolean, true if the removal succeeded


* *Service:* Libraires
 * *Operation:* addLibraryACL
 * *Input Parameters:*
   * Name - a string containing the library\'s name
   * AccessLists - a list of access list entries - a list of structures of the form
       * Name - a string the circle name
       * Permissions - a list of strings containing this circle\'s permissions.Permissions are from [http://www.isi.edu/~faber/tmp/DeterAPI/doc/constant-values.html#net.deterlab.testbed.api.Permissions this table]
 * *Return Values:*
   * a boolean, true if the removal succeeded

* *Service:* Libraries
 * *Operation:* removeLibraryACL
 * *Input Parameters:*
   * Name - a string containing the library\'s name
   * AccessLists - a list circles to remove from the ACL
 * *Return Values:*
   * a boolean, true if the removal succeeded

The owner can give ownership to another user.

* *Service:* Library
 * *Operation:* setOwner
 * *Input Parameters:*
   * Name - a string containing the experiment\'s name
   * newOwner - a string, the new owner\'s UID
 * *Return Values:*
   * a boolean, true if the removal succeeded

### Library Profiles

Libraries have profiles attached to them, as users, circles, and projects do.  Information in a profile is metadata about the library, intended for human consumption. 

The calls for manipulating a library profile are very similar to other profiles:

To get a profile schema, for example to create an empty web page,

 * *Service:* Libraries
 * *Operation:* getProfileDescription
 * *Input Parameters:*
 * *Return Values:*
   * LibraryId - always empty
   * A list of profile elements each containing
     * Name - a string, the element\'s name
     * DataType - a string giving the element\'s
       * string
       * integer
       * double
       * binary/opaque
     * Value - a string containing the element\'s value
     * Access - a string describing the access values (values [http://www.isi.edu/~faber/tmp/DeterAPI/doc/constant-values.html here])
     * Optional - a flag true if the field is optional (must be present but may be empty)
     * Removable - a flag true if the field can be removed
     * Description - a string explaining the field
     * Format - a regular expression that can be used to validate the field entry (may be null, and generally is for optional fields)
     * FormatDescription - A brief, natural language description of the field input constraints, e.g. "A valid e-mail address" or "only numbers and spaces".
     * OrderingHint - an integer suggesting where to present the attribute. Lower numbers come first
     * LengthHint - an integer suggesting how long an input firld should be presented

To read a library\'s profile.

 * *Service:* Libraries
 * *Operation:* getLibraryProfile
 * *Input Parameters:*
  * libid - a string naming the library to be retrieved
 * *Return Values:*
   * LibraryId - the experiment whose profile is returned
   * A list of profile elements each containing
     * Name - a string, the element\'s name
     * DataType - a string giving the element\'s
       * string
       * integer
       * double
       * binary/opaque
     * Value - a string containing the element\'s value
     * Access - a string describing the access values (Defined [http://www.isi.edu/~faber/tmp/DeterAPI/doc/constant-values.html here])
     * Optional - a flag true if the field is optional (must be present but may be empty)
     * Removable - a flag true if the field can be removed
     * Description - a string explaining the field
     * Format - a regular expression that can be used to validate the field entry (may be null, and generally is for optional fields)
     * FormatDescription - A brief, natural language description of the field input constraints, e.g. "A valid e-mail address" or "only numbers and spaces".
     * OrderingHint - an integer suggesting where to present the attribute. Lower numbers come first
     * LengthHint - an integer suggesting how long an input field should be presented

Finally a user can modify a library profile:
 
 * *Service:* Libraries
 * *Operation:* changeLibraryProfile
 * *Input Parameters:*
   * Libid - the library\'s profile to modify
   * A list of change requests. Each request contains
     * Name - the name of the field to change
     * Value - the new value of the field
     * Delete - a flag, if true delete the field rather than modify it
 * *Return Values:*
   * A list of responses each containing
    * Name - astring with the name of the field
    * Success - a flag indicating if the request succeeded
    * Reason - a string indicating the reason if Success is false


### Manipulating Profile Attributes

Administrators may add attributes to library profiles or remove them.  That is to say, add a schema (including format constraints and descriptions), not set a value for a library.

Creating an attribute:

* *Service:* Libraries
 * *Operation:* createLibraryAttribute
 * *Input Parameters:*
   * Name - the attribute name
   * Type - the type (STRING, INT, FLOAT, OPAQUE)
   * Optional - a boolean, true if this attribute is optional
   * Access - a string: the user\'s ability to modify (READ_WRITE, READ_ONLY, WRITE_ONLY, NO_ACCESS)
   * Description - natural language description of the field (optional)
   * Format - a regular expression describing the format (optional)
   * Formatdescription - a natural language explanation of the format (optional)
   * OrderingHint - an integer suggesting where to present the attribute. Lower numbers come first
   * LengthHint - an integer suggesting how long an input 
   * Def - default value of the attribute (will be set for all users)
 * *Return Values:*
  * a boolean, true if the attribute was created 

And removing an attribute:

* *Service:* Libraries
 * *Operation:* removeLibraryAttribute
 * *Input Parameters:*
   * Name - the attribute name
 * *Return Values:*
  * a boolean, true if the attribute was removed 

Finally administrators can modify profile attributes:

* *Service:* Libraries
 * *Operation:* modifyLibraryAttribute
 * *Input Parameters:*
   * Name - the attribute name
   * Type - the type (STRING, INT, FLOAT, OPAQUE)
   * Optional - a boolean, true if this attribute is optional
   * Access - a string: the user\'s ability to modify (READ_WRITE, READ_ONLY, WRITE_ONLY, NO_ACCESS)
   * Description - natural language description of the field (optional)
   * Format - a regular expression describing the format (optional)
   * Formatdescription - a natural language explanation of the format (optional)
   * OrderingHint - an integer suggesting where to present the attribute. Lower numbers come first
   * LengthHint - an integer suggesting how long an input 
 * *Return Values:*
  * a boolean, true if the attribute was modified 


## Admin

The Admin service allows testbed administrators to manipulate the implementation internals and to initialize the testbed.  Most users will use this interface.  This section is provided out of order for completeness.

[http://www.isi.edu/~faber/tmp/DeterAPI/doc/net/deterlab/testbed/api/Admin.html javadocs for Admin]

The various permissions described in this document are also installed in the implementation\'s database.  It is possible to add new permissions while the system is running using the addPermission call.  The name of a permission is a string (scoped by the object).  Objects include circle, library, experiment, and project.

 * *Service:* Admin
 * *Operation:* addPermission
 * *Input Parameters:*
   * name - the name of the permission to add (string)
   * object - the object type for which it is valid (string)
 * *Return Values:*
   * a boolean, true if successful


The bootstrap call initializes an empty system and adds a user with full administrative power.  This only works if no admin project exists and no *deterboss* user exists.  It also creates a regression project for testing.  It returns the userid and password of the new admin user (always deterboss).

 * *Service:* Admin
 * *Operation:* bootstrap
 * *Input Parameters:*
   * none
 * *Return Values:*
   * bootstrap user (string)
   * bootstrap password (string)

The clearCredentialCache() call removes any cached credentials from the system.

 * *Service:* Admin
 * *Operation:* clearCredentialCache
 * *Input Parameters:*
   * none
 * *Return Values:*
   * true on success

There are simple timers inserted in the code at times.  One can access their values and reset them using:

 * *Service:* Admin
 * *Operation:* getTimerValues
 * *Input Parameters:*
   * a list of timer names (may be empty)
 * *Return Values:*
   * A list of pairs of
    * timer name (string)
    * timer total (64-bit integer) elapsed time since started

 * *Service:* Admin
 * *Operation:* removeTimers
 * *Input Parameters:*
   * a list of timer names (may be empty)
 * *Return Values:*
   * true on success

If the policy databases have changed, administrators can reset the underlying policy and credential database using

 * *Service:* Admin
 * *Operation:* resetAccessControl
 * *Input Parameters:*
   * none
 * *Return Values:*
   * true on success

This can be a time-consuming operation.


## Resources

Resources are things that make up experiments. They are fairly generic at this point, though we expect to specialize them.  Currently system resources - like computers and VMs - are primarily manipulated by the testbed.  Like circles, resources have names scoped by user or project.  A user can create local resources in their namespace and the testbed creates resources in the admin or system namespaces.

Users primarily see system resources when an experiment is realized.

A resource consists a type, an optional description, zero or more facets, zero or more tags and an ACL that allows circle members to manipulate it.  Types are identifiers that control how the testbed and user can manipulate them.  Common resource types include "VM" and "computer".

A resource encapsulates the ability to

 * Compute
 * Store data
 * Communicate
 * Sense the world
 * Manipulate the world

Each of those capabilities is described by a facet data structure attached to the resource.  A facet describes the available powers of a resource.  When a resource is realized inside another, the inner resource provides its facets by consuming the outer resource\'s facets.

Tags are name value pairs associated with a resource or facet thereof.  They are a simple extension mechanism, akin to attributes in [topdl](LayoutAspect#ViewExchangeFormattopdlv2.html).

### Viewing Resources

One or more resources can be viewed by an authorized user assuming that user has the proper permissions.  The search can be a regular expression matched against the library name as well as scoping the search by type, realization membership, tags and user visibility.  To view resources, call:

 * *Service:* Resources
 * *Operation:* viewResources
 * *Input Parameters:*
   * Userid - a string, if given return all experiments this user can see
   * Type - a string, if given return only resources of this type
   * Regex - a string containing a regular expression matched against experiment names
   * Realization - an optional string.  If given return only resources in the given realization
   * Persist - an optional boolean.  If given return only peristent resources
   * tags - an array of name value pairs.  If given only return resources that are so tagged.
 * *Return Values:*
   * One or more structures with the following fields
     * Name - a string containing the resource name
     * Type - a string containing the type
     * Description - an optional natural language description
     * Tags - a list of name value pairs (strings)
     * Facets - a list of data structures containing
       * Name - the facet name
       * Type - the type
       * Value - the quantity
       * Units - the units that define value
       * Tags - a list of name value pairs (strings)
     * ACL - a list of structures of the form
       * Name - a string the circle name
       * Permissions - a list of strings containing this circle\'s permissions.  Permissions are from [http://www.isi.edu/~faber/tmp/DeterAPI/doc/constant-values.html#net.deterlab.testbed.api.Permissions this table]

Users - primarily administrators - can create or delete resources

 * *Service:* Resources
 * *Operation:* createResource
 * *Input Parameters:*
   * Name - a string, the scoped, unique identifier of the resource
   * Type - a string, the resource type
   * Persist - a boolean, true if the resource is independent of an experiment realization.  Non persistent resources resource disappear once bound to a realization and then released.
   * Description - a string, an optional human readable description of the resource
   * A list of zero or more facets that describe the capabilities and requirements of the resource.  Each facet includes
    * A name, scoped by the resource
    * A type (CPU, Storage, Communication, Sensing, Manipulation)
    * A value, the real-valued amount of the type
    * A units value that scales and defined the value
    * Zero or more tags.
  * An access control list
 * *Return Values:*
  * A boolean indicating success

A resource is deleted using:
 
 * *Service:* Resources
 * *Operation:* removeResource
 * *Input Parameters:*
   * Name - a string containing the resource to delete
 * *Return Values:*
   * a boolean, true on success


## Realizations

A realization is a binding of an Experiment and a Circle to Resources.  Intuitively, it is an experiment in progress.  A researcher manipulates a realization using the Realizations service.

There are two bindings that are part of a realization, the containment hierarchy that shows how resources are bound to other resources and a topology mapping that maps topology elements to resources.

In addition a realization has a status, indicating the state of the experiment instance as a whole.

### Viewing Realizations

One or more realizations - experiments in progress - can be viewed by an authorized user assuming that user has the proper permissions.  The search can be scoped by circle, and a regular expression matched against the experiment name.  To view realizations, call:

 * *Service:* Realizations
 * *Operation:* viewRealizations
 * *Input Parameters:*
   * Userid - a string, if given return all experiments this user can see
   * Regex - a string containing a regular expression matched against experiment names
 * *Return Values:*
   * One or more structures with the following fields
     * Name - a string containing the realization name (scoped by owner name)
     * Experiment - a string containing the realized experiment
     * Circle - the circle that the experiment is realized in
     * Status - one of:
       * Empty
       * Allocated
       * Initializing
       * Active
       * Terminating
     * Containment - a list of structures of the format
       * Outer resource name - the name of the containing resource
       * Inner resource name - the name of the contained resource
     * Mapping - a list of structures of the format
       * Element name - the name of the element from the experiment\'s layout aspect
       * Resource name - the name of the resource to which the element is mapped


### Creating and Deleting a Realization

A researcher creates a realization - starts an experiment - using the realizeExperiment operation on an experiment.

 * *Service:* Realizations
 * *Operation:* realizeExperiment
 * *Input Parameters:*
   * Userid - the user making the request (the owner on success)
   * Owner - an optional string the computer owner
   * Circle - the circle under which the experiment is being created
   * Experiment - the experiment being realized
   * Name - an optional string, the name of this realization (system will assign if omitted) - scoped by owner name
 * *Return Values:*
   * Success or failure

### Removing A Realization

A realization is deleted using:
 
 * *Service:* Realizations
 * *Operation:* removeRealization
 * *Input Parameters:*
   * Name - a string containing the realization to delete
 * *Return Values:*
   * a boolean, true on success

After `removeRealization` succeeds, the realization is removed from the testbed and the resources are released.

A user may deallocate the resources from a realization using:

A realization is deleted using:
 
 * *Service:* Realizations
 * *Operation:* releaseRealization
 * *Input Parameters:*
   * Name - a string containing the realization to delete
 * *Return Values:*
   * a boolean, true on success

After `removeRealization` succeeds, the realization remains in the testbed in the empty state.  When future interface changes allow changes to realizations, this will be more useful.