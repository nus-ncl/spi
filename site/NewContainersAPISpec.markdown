---
layout: default
---
[[TOC]]

# DETER Containers API

The containers API is a set of simple and extensible operations to control testbed elements.  It is primarily a way for the testbed control to communicate with experiment elements. As such it is not usually visible to users.  For power users we do supply a passtrough interface so that they can access specializations of containers.

## Testbed to Containers Operations

Each of these is sent to a container controller with a distinct address.  A controller is often responsible for more than one container.  In general we attempt to scale the number of controllers with the number of physical resources so that the testbed can communicate with them directly.  In very large testbeds, there may be interconnected controllers acting as relays.

The operations in this section are all exported from the container controller.  The testbed discovers container controllers when they report in using the operation in the following section.

All the control commands can be applied to multiple containers simultaneously.

There are only a few things all containers can do:

Begin operating as an experiment element. For a computer, boot; for a flux capacitor, begin travelling in time:

 * *Service:* Containers
 * *Operation:* start
 * *Input Parameters:*
   * NameRE - an optional list of regular expressions to match against container names
   * Types - a list of container types to restrict the operations to
 * *Return Values:*
  * None

Become quiescent. Only containers API requests will work on a stopped container

 * *Service:* Containers
 * *Operation:* stop
 * *Input Parameters:*
   * NameRE - an optional list of regular expressions to match against container names
   * Types - a list of container types to restrict the operations to
 * *Return Values:*
  * None

Set up the container\'s internal state. (For a computer, establish accounts and mount filesystems; for a flux capacitor, set travel rate and destination date).  Configuration can be a multi-step process to facilitate configuring many containers at once.  A base configuration can be distributed broadly and sub-configurations passes to specialized elements.  The semantics of combining configurations can vary per-container-type, but are generally a sequential application of configurations.

 * *Service:* Containers
 * *Operation:* configure
 * *Input Parameters:*
   * NameRE - an optional list of regular expressions to match against container names
   * Types - a list of container types to restrict the operations to
   * ConfigData - an opaque field used to configure the container
   * Commit - a flag indicating that this is the configuration is complete and the container can begin applying it
 * *Return Values:*
  * None

Containers can describe themselves.  Again, controllers can collapse multiple like containers into one operation.

 * *Service:* Containers
 * *Operation:* describe
 * *Input Parameters:*
   * NameRE - an optional list of regular expressions to match against container names
   * Types - a list of container types to restrict the operations to
 * *Return Values:*
  * An array of structures with the following fields
   * Names - an array of strings naming containers
   * Type - a string giving the container type
   * ConfigFormat - the configuration format that the container supports
   * Interfaces - a list of pre-defined extension interfaces supported by the container.  If "custom" is given ExtendedOps is required
   * ExtendedOps - an optional list of URIs from which the WSDL for custom extended operations can be gathered

Get the state of containers

 * *Service:* Containers
 * *Operation:* state
 * *Input Parameters:*
   * NameRE - an optional list of regular expressions to match against container names
   * Types - a list of container types to restrict the operations to
 * *Return Values:*
  * a list of fields of the form
    * Names - an array of container names
    * State - a string giving the state
      * Down - The container is in communication with the testbed, but not yet configured to act as an experiment element 
      * Configured - The container is set up to act as an experiment element but is not yet doing so 
      * Pinned - The container is not acting as an experiment element, but is carrying out an operation that renders it otherwise unusable. For example, a container that is capturing its state may be in this state. 
      * Up - The container is acting as an experiment element 
      * None - Nothing is known about the container. is is (as yet) unresponsive.

## Containers to Testbed Operations

A container controller can report a state change to the testbed using the call

 * *Service:* Containers
 * *Operation:* newState
 * *Input Parameters:*
   * Controller - the URI of the container controller reporting
   * Names - an array of container names
   * State - a string giving the state, as in state above

As containers are initialized, controllers will check in with the testbed using this call.
