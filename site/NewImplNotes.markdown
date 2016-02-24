---
layout: default
---
[[TOC]]

# New API Implementation Notes

This page keeps track of the internals of the Deter API implementation.

## Configuration Files

Both the test utilities and the service itself have configuration files.

### Service Configurations

Service configuration file a are java properties files stored in /usr/local/etc/deter/.  The main configuration file is

/usr/local/etc/deter/service.properties which consists of (passwords and usernamed expunged):

    #DETER utilities properties
    #Thu Jul 18 16:27:29 PDT 2013
    emulabDbUrl=jdbc\:mysql\://localhost/tbdb?user\=auser&password\=apassword
    keystorefilename=/usr/local/apache-tomcat-7.0/conf/tomcat.keystore
    logconfig=/usr/local/etc/deter/log4j.properties
    deterDbUrl=jdbc\:mysql\://localhost/deter?user\=auser&password\=apassword
    keystorepw=apassword

Contents are:

 * the URL to read and write the emulab DB (emulabDbUrl)
 * the URL to read and write the deter DB (deterDbUrl)
 * the keystore containing the id used to sign new credentials (keystore)
 * the peystore passowrd (keystorepw)
 * the log4j properties file used to configure the service logging (logconfig)

The loggingconfiguration syntax is defined by [http://logging.apache.org/log4j/1.2/manual.html log4j].  Current contents are:

    # logging for DeterServices
    #
    
    # For debugging this configuration set this variable true and look at tomcat\'s
    # stderr.
    log4j.debug=true
    
    # Rolling log appender to put out into /var/log/deter
    log4j.appender.A1=org.apache.log4j.RollingFileAppender
    log4j.appender.A1.MaxFileSize=10MB
    log4j.appender.A1.MaxBackupIndex=3
    log4j.appender.A1.layout=org.apache.log4j.PatternLayout
    log4j.appender.A1.layout.ConversionPattern=%d{MM/dd HH:mm:ss} %-5p %c{1}: %m%n
    log4j.appender.A1.file=/var/log/deter/service.log
    
    # The net.deterlab logger will inherit the root level and use the
    # /var/log/deter appender above.  It will not log to the root.
    log4j.logger.net.deterlab=DEBUG, A1
    log4j.additivity.net.deterlab=false
    

That defines a service log in /var/log/deter/services.log logging at DEBUG and higher.  It is automatically rotated at 10 MB with 3 copies kept around.  We use the log4j rotator rather than newsyslog to avoid disruption.

### Utility Configurations

Utilities read from a configuration in the users `$HOME/.deterutils.properties`.  This is a java properties file. A simple setup looks like (passwords and users purged):

    #DETER utilities properties
    #Wed Aug 14 18:09:13 PDT 2013
    useridpw=apassword
    serviceurl=https\://localhost\:52323/axis2/services/
    trustpw=apassword
    useridfilename=/usr/home/faber/myID
    trustfilename=/usr/home/faber/keystore

This keeps track of

 * Trusted certificates - the DETER service id (trustfilename & trustpw)
 * User identity certificate - useridfilename, useridpw)
 * The service base URL - ( serviceurl)

Both files can be modified by the SetProperty utility.


## Database tables

### Users

Users are kept in a table of this format called users:


| Field | Type | Null | Key | Default | Extra |
|idx | int(11) | NO   | PRI | NULL  | auto_increment |
|uid | varchar(20) | YES  | - | NULL | - | 
|password | varchar(255) | YES  | - | NULL | - |
|hashtype | varchar(32)  | YES  | - | NULL | - |
|passwordexpires | datetime   | YES | - | NULL | - |

Most of that is straightforward.  The password is a hash with type defined by hashtype.  Currently all are crypt format hashes.

Passowrds are checked using a challenge system and password resets also populate the same table.  When created a challenge is put in the userchallenge table:

| Field | Type | Null | Key | Default | Extra |
| uidx | int(11) | YES | - | NULL | - |
| data | blob | YES | - | NULL | - |
| validity | datetime | YES | - | NULL | - |
| challengeid | bigint(20) | YES | UNI | NULL | - |
| type | varchar(20) | YES | - | NULL | - |

A password challenge - checking a login - has type "clear." The data is the challenge issued with enough info to check the response.  Currently, only a clear challenge is used, meaning that the data contains the crypt hash of the password to check.  The validity field is checked and old challenges discarded whenever a challeng is accessed.  uidx is the index (idx) of the user for whom the challenge was issued.  As with all DB fields the links are through integer indices.

When used to reset a password the challenge is issued with type "PasswordReset".  The presence of such a challenge is all that is checked.  A user who calls changePasswordChallenge with a the ID of a valid "PasswordReset" challenge can change the password of the user pointed to by the uidx field.

The body of a notification is stored in the notification table:

| Field | Type | Null | Key | Default | Extra |
| idx | int(11) | NO | PRI | NULL | auto_increment |
| created | timestamp | NO | - | CURRENT_TIMESTAMP | on update CURRENT_TIMESTAMP |
| body | text | YES | - | NULL | - |

When delivered to a user a row is added to the usernotification table for each user:

| Field | Type | Null | Key | Default | Extra |
| nidx | int(11) | YES | MUL | NULL | - |
| uidx | int(11) | YES | MUL | NULL | - |
| flags | int(11) | YES | - | NULL | - |


nidx and uidx are both constrained to be foreign key indices unto the notification and users tables respectively.  This means that all notifications to a user are removed before the user is removed.  Each entry in the usernotification table is a status for that message to a user (urgent/read).

### Profiles

Most objects in the Deter API have a profile attached to them and all of these tables are the same with minor tweaks.  Here is the schema for user profile entries (the userattribute table):

| Field | Type | Null | Key | Default | Extra |
| idx | int(11) | NO | PRI | NULL | auto_increment |
| name | varchar(20) | YES | - | NULL | - |                                
| datatype | enum(\'STRING\',\'INT\',\'FLOAT\',\'OPAQUE\') | YES | - | NULL | - |
| optional | tinyint(4) | YES | - | NULL | - |                             
| access | enum(\'READ_WRITE\',\'READ_ONLY\',\'WRITE_ONLY\',\'NO_ACCESS\') | YES | - | NULL | - |
| description | text | YES | - | NULL | - |
| format | varchar(256) | YES | - | NULL | - |                             
| formatdescription | text | YES | - | NULL | - |
| sequence | int(11) | NO | - | 0 | - |                                    
| length | int(11) | NO | - | 0 | - |

This is a description of the attribute.  Note that attribute descriptions have indices.

To assign a value in a given user\'s profile, an entry is added to the userattributevalue table:

| Field | Type | Null | Key | Default | Extra |
| uidx | int(11) | YES | MUL | NULL | - |
| aidx | int(11) | YES | MUL | NULL | - |
| value | text | YES | - | NULL | - |

uidx is an index into the users table, aidx is an index into the userattribute table and value is the contents of the variable.  A profile is the collection of these entries for a user.

The DB enforces the index constraints.

Profiles for circles and projects use the tables called circleattribute and projectattribute, with the same fields (to give separate schema spaces).  Circle attrubutes are assigned via the circleattributevalue table:

| Field | Type | Null | Key | Default | Extra |
| cidx | int(11) | YES | MUL | NULL | - |
| aidx | int(11) | YES | MUL | NULL | - |
| value | text | YES | - | NULL | - |

and projects using projectattributevalue

| Field | Type | Null | Key | Default | Extra |
| pidx | int(11) | YES | MUL | NULL | - |
| aidx | int(11) | YES | MUL | NULL | - |
| value | text | YES | - | NULL | - |

cidx and pidx are indices into the circles and projects tables respectively, with analogous DB constraints.

### Circles and Projects

Circles and porjects are similar in implementation, differing slightly in the base tables that define them.  Circles are in the circles table:

| Field | Type | Null | Key | Default | Extra |
| idx | int(11) | NO | PRI | NULL | auto_increment |
| circleid | varchar(256) | YES | - | NULL | - |
| owneridx | int(11) | YES | MUL | NULL | - |
| created | timestamp | NO | - | CURRENT_TIMESTAMP | on update CURRENT_TIMESTAMP |

This defines a circleid, owner and keeps a creation time.

Projects are in the projects table:

| Field | Type | Null | Key | Default | Extra |
| idx | int(11) | NO | PRI | NULL | auto_increment |
| projectid | varchar(256) | YES | - | NULL | - |
| owneridx | int(11) | YES | MUL | NULL | - |
| created | timestamp | NO | - | CURRENT_TIMESTAMP | on update CURRENT_TIMESTAMP |
| linkedidx | int(11) | YES | MUL | NULL | - |
| flags | int(11) | NO | - | 0 | - |


The differences are the linkedidx field, which is an index into the circles table for the linked circle, and teh flags field that is a set of boolean attributes about a project.  Right now this is only the flag for approval, which is used commonly enough and intrinsic enough to the project to be stored here rather than the profile.

Membership in a circle is defined by the circleusers table:

| Field | Type | Null | Key | Default | Extra |
| cidx | int(11) | NO | MUL | NULL | - |
| uidx | int(11) | NO | MUL | NULL | - |
| perms | int(11) | YES | - | NULL | - |

This is almost exactly analogous to the notifications table.  Perms is a mask of teh permissions granted to this user with respect to this circle.  The indices have appropriate DB constraints as keys into circles (cidx) and users (uidx).

Project membership is exactly analogous:

| Field | Type | Null | Key | Default | Extra |
| pidx | int(11) | NO | MUL | NULL | - |
| uidx | int(11) | NO | MUL | NULL | - |
| perms | int(11) | YES | - | NULL | - |

pidx is constrained as a key into projects.

Finally circles and projects both allow joining and adding by consent of the new member and a member with ADD_USER rights.  This is accomplished by putting a challenge into the database that can be redeemed by presenting the API with its ID (from a 64-bit space).

The table holding these challenges (circlechallenge) looks like:


| Field | Type | Null | Key | Default | Extra |
| idx | bigint(20) | YES | UNI | NULL | - |
| uidx | int(11) | YES | MUL | NULL | - |
| cidx | int(11) | YES | MUL | NULL | - |
| expires | datetime | YES | - | NULL | - |
| perms | int(11) | YES | - | NULL | - |

uidx and cidx are links into users and circles as before.


| Field | Type | Null | Key | Default | Extra |
| idx | bigint(20) | YES | UNI | NULL | - |
| uidx | int(11) | YES | MUL | NULL | - |
| pidx | int(11) | YES | MUL | NULL | - |
| expires | datetime | YES | - | NULL | - |
| perms | int(11) | YES | - | NULL | - |

Projects are analogous.

## Binding Emulab Constructs to DETER Constructs

Descartes will use Emulab as a [NewImpl resource allocation system].  This means formerly first class entities, such as Emulab users and projects will be the building blocks on which DETER entities will be built.  The Emulab constructs will become more ephemeral.  For example, a DETER experiment\'s allocation of computers and VLANs will be embodied as an Emulab experiment, but the DETER experiment will also contain the container configurations, procedures, data monitoring and constraint implementations.

### Users, Projects, and Circles

Initially I was concerned that while DETER users and DETER projects would map fairly cleanly to Emulab users and projects, that circles would be a problem.  The primary concern was that Emulab Projects map to UNIX groups on the testbed and that those UNIX permissions implement isolation on shared filesystems inside an experiment.  Older UNIXes limited the number of groups to 16, and we had experienced problems crossing that limit.

Investigations and experiments show that FreeBSD 9 (on which the testbed runs) allows a user to be a member of an unlimited number of groups, and that group ids are a full 32-bits.  This means that both projects and circles can be directly mapped to Emulab projects, and that we can take advantage of existing isolation mechanisms.

### Extensions to Emulab

Three main extensions must be made to Emulab to support this mapping:

 * An interface to create an Emulab user without a confirmation going to the user (Descartes will do the confirmation).
   * A variation on this exists for creating student users that we will flesh out into a full "create user" interface
 * An interface for creating EMulab Projects without a confirmation loop.  Again Descartes will handle the validation of the DETER project.
  * There is old code for this in fedd, which once was to user dynamic projects.  That code\'s time has apparently come.
 * A system for mapping circle names into project/UNIX group names.  The Emulab project that underlies a DETER circle will be largely invisible to a user, except in that a UNIX group tied to it will be used to manipulate the file system.  We will need to provide a simple mapping from the {user|project}:circle name to a project/group name.


These should be easy to implement.