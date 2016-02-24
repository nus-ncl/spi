---
layout: default
---
[[TOC]]

# Implementation Path

The New DETER APIs replace what is essentially a widened Emulab interface with powerful additional features implemented on top of it with an integrated system that incorporates the new features into a principled and powerful API.  In addition the administration of users and rights has been significantly expanded to enable more interesting and powerful sharing of resources and experiments between users.

As this system is implemented, DETERlab must preserve the existing web interfaces for its current users.  Thousands of users, including students, depend on the existing feature set and resources to conduct research and teach classes.  This page discusses the challenges and plan in moving the new features forward without overly disrupting existing service.

## The System Architecture

The new system architecture (below) includes the existing Emulab software as a resource allocation system.

[[Image(System_Diagram.png)]]

All the systems outside the red rectangle will require to be created or modified to support the new API.  In many cases we will derive the new, general structures from existing ones.

The new system will primarily be run by the DETER System box - a server running on the current boss machine.  It differs from the current interfaces in that it:

 * Separates web interfaces from operational interfaces
 * Manipulates more complex experiment object that include
  * Descriptions of operations to perform
  * Experimental dataflow
  * Topology expressed in containers
 * Supports a more expressive and flexibly permissions system
  * Access to the same experiment allowed by multiple projects
 
The current Emulab implementation carries out some of those functions now, albiet less powerfully.  The initial DETER system implementation will use the legacy implementations of those features (in all cases the new features are a superset) and implement and expose enhanced functionality as needed.  In the long run, the only features to be provided by Emulab will be allocation of computers resources and providing physical connectivity.

## Policy and Permissions

The policies of the DETER interface are more fine-grained and more complex than existing Emulab policies.  We will be using the [http://abac.deterlab.net ABAC] authorization control system to separate the authorization semantics from the implementation and to allow independent auditing and reconfiguration of that system.

The major thrust of this part of the implementation is providing administrator tools to read and edit the policies, expressed in ABAC, that underly the system.  In addition, we will be producing a policy database that will manage the system\'s interaction with policy.

We are planning to design our own policy management system, based on the ABAC grouper system we demonstrated at [http://groups.geni.net/geni/wiki/GEC14Agenda GEC 14].

The policy system can be designed implemented and enhanced without being in the critical path of creating the new DETER interface.

## The DETER Beginner Web Interface (DBI)

To demonstrate the power of the testbed API to provide multiple views of DETERlab, a group is constructing an alternative teaching/introductory interface using the DETER testbed API.  The development of the new DETER system must support this new interface as it forms.

## Roadmap

The roadmap for development is:

 1. Implement stubs and passthrough code that allows access to existing functionality through the DETER testbed API.  This is a 2-3 week prospect and is done in order to
  * support development of the DBI
  * allow simultaneously creating a new minimal access web interface that will become the default interface
 2. Migrate user and permissions function into the new interface
  * At this point parallel policy development tool development can begin
 3. Build the in-testbed processes that use the containers API for configuration
  * A transition plan for existing images is also required here
 4. Complete integration of testbed API and containers API
  * At this point a transitional web DB will be in place
 5. Allow dual testbed use through either interface - new users on new interface
 6. After about 1 month, shut down old interfaces.


## Short Term Schedule and Milestones

 * Access Control integrated with API calls
   * *Milestone 5 Sept (~~23 Aug~~)* demonstrate access controlled calls to API
     * Subset of regression tests demonstrate access control in place
 * Front end access to Experiments
   * *Milestone 18 Sep (~~28 Aug~~)*  *Done 17 Sep* [NewImplNotes#BindingEmulabConstructstoDETERConstructs Wiki documnetation of Emulab integration plan] for meshing new interface to Emulab (matching DETER & Emulab users, reconciling Emulab Projects and DETER circles)
   * *Milestone 7 Oct (~~20 Sep~~) (~~30 Aug~~)* Wiki documentation of Specification of internal representation of experiment topology this is a spec of what we store and how for experiment topologies and how they become emulab/containers topologies.  For actions, data gathering, and constraints placeholder files will be used.  These will be expanded.
   * *Milestone 11 Oct (~~1 Oct~~) (~~6 Sept~~)* Working Experiment API calls that access saved but realistic experiment topologies and placeholder files on other axes.  Initial subset of regression tests.

 * Experiment Back-end
   * *Milestone 18 Oct (~~4 Oct ~~)(~~11 Sep~~)* demonstrate connection of DETER users to Emulab users.  API creation of user on E-in-E results in correct creation of Emulab users and projects as outlined in the 20 Sep ~~28 Aug~~ milestone.
   * *Milestone 25 Oct (~~11 Oct~~) (~~18 Sep~~)* can create physical node-only experiment from interface on e-in-e.
   * *Milestone 1 Nov (~~21 Oct~~) (~~27 Sep~~)* can create containerized experiment on e-in-e with virtual machines.