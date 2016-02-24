---
layout: default
---
[[TOC]]

# DETER Testbed System Programming Interface (SPI)

The DETER ASPI is really an interface with two sides, an outward facing side (the testbed SPI) that allows people to manage resources on the testbed and an inward facing side (the containers SPI)  that coordinates the containers that make up an experiment environment.  This document defines these interfaces and documents how to use them.  It is a living document that will gain detail as the specification grows and implementations are put into service.

The testbed API is responsible for managing the following things:

*  Users
    * researchers
*  Projects 
    * groups of related users used to control what users have access to the testbed as a whole and to provide real-world accountability in the case of misbehavior.
*  Circles
    * groups of related users used to manage what users can see and do to other testbed resources
*  Experiments
    * research environments that may be stored, edited, and realized on testbed resources
*  Libraries
    * groups of experiments that provide logical organization of experiments into classes or collect experiments with similar scope of interest
*  Realizations
    * an experiment in progress and its binding to resources.
 

The testbed SPI enables users to ask a DETER testbed to do things for them.  It will generally be called from a more user-friendly front end, tuned to the user\'s experience and goals, such as the evoloving DETER Beginners Interface.

The containers API is used by the DETER control system to manage the resources that make up an experimental topology in progress. The goal of this API is to take raw resources inside the testbed that have been allocated using the testbed API, and configure them into a usable environment.  Each logical element of an experiment (a computer, a router, etc.) is represented by a container which must be managed by DETER.

Managing a container consists of:

 * Allocating resources to it
 * Installing and configuring any virtualization software or configuring hardware
  * This involves translating from a more generic topology/configuration description into the setup for a specific container type
 * Configuring and starting MAGI software necessary to connect to the experiment management system
 * Exposing and using container-specific features to provide DETER services

We discuss the testbed SPI and then the containers SPI.  Then we discuss some initial implementation details and present a roadmap for development.

## DETER Testbed SPI

The testbed SPI manipulates a few entities to provide the experimentation environment.  This section discusses the key abstractions of the testbed SPI and how they work together to create an experimental environment.

### Users

A *user* is a researcher who uses the DETER testbed.  They request testbed services and allocate testbed resources.  Users are the actors that make things happen through test testbed SPI.

Each user is identified by a unique string, their *userid*.  Userids are assigned when the user is created on the testbed and guaranteed to be unique.

In addition to the user identifier DETER keeps meta-data about users.  Currently that meta-data consists of:

 * Projects the user is in (see below)
 * Experiments the user owns (see below)
 * A password to authenticate the user
 * A valid e-mail address for communication and password resetting
 * General metadata, e.g, 
   * Real name
   * Affiliation
   * Phone number
   * Address

A user identifies themselves to the testbed SPI by proving that they hold a specific public/private keypair.  An initial such keypair is issued when the user is created, and a user can acquire another valid pair at any time by proving they know their password.  Generally those pairs are short-lived to guard against loss or theft, but the password is administered using local testbed policies.

### Grouping Users: Projects and Circles

DETER groups users for two main reasons: to distribute the time costs of administration and to facilitate collaborative use of DETER\'s resources.  The first is accomplished through the Project abstraction and the second through the Circle abstraction.

#### Projects

Projects are groups of users who can access testbed resources.  They correspond to such real-world groups as classes or research projects. Each project has an owner, vetted by DETER staff, who can then add users to the project at their discretion.  Such users have access to the DETER testbed.  This is close in spirit to an Emulab project, but shorn of its access control implications.


A project always has an owner, the user vetted by DETER staff.  Ownership can be changed, but only by the owner.

When a user is added to a project, their rights are also defined within that project.  The rights are:

 * Can add other users to the project
 * Can remove users from the project
 * Can realize create circles in the project\'s name space (see below)

Each project has a profile attached to it, as a user does, and that profile can be expanded to include other attributes.

When a researcher wants to start a project on DETERlab, the researcher creates a user and requests a project.  Though the testbed API sees this as two steps, the user interface presented by a web page would roll these two steps together. A user sees it as a single application page.

The project is created but unapproved and cannot do anything.  When the group responsible for vetting projects approves the project, its state is changed (to approved) and the owner can now add other users and use testbed resources. (Each project has a circle linked to it that controls project members access to testbed resources.)

When a new user applies to join the testbed, a user is created that can only set its password or apply to create or join a project.  When a user applies to join a project, all project members who can add that user are notified.  Any one of them can accept the user and set their permissions.  By accepting that user the project takes some responsibility for them.  If that user misbehaves, the project owner will be contacted as well as the user.

If a user is removed from all projects, they return to a powerless state.

#### Circles

Circles are groups of users.  They are used to confer rights to groups of users, identify groups of users, and influence how resources are configured when experiments are realized on the testbed.  Circles are intended to be lightweight and encourage collaboration inside a project or across projects. This is a more general use of grouping than an Emulab testbed.

A circle always has an owner, the user responsible for its creation.  Ownership can be changed, but only by the owner.

Circle names are scoped one of two ways, by prefixing them with either the uid of the user that created it, or a project that the creating user is a member of.  The right to create circles with the project\'s prefix is controlled by a user\'s permissions in the project.  Because uids and project names are unique across the testbed, circle names are unique when prefixed.   The names written as prefix:circle.

When a user is added to a circle, their rights are also defined within that circle.  The rights are:

 * Can add other users to the circle
 * Can remove users from the circle
 * Can realize experiments under this circle (granting members of the circle access to the resources in use)

Each circle has a profile attached to it, as a user does, and that circle can be expanded to include other attributes.

Each user in an approved project is a member of at least two circles: a circle attached to the project (created and owned by the owner of the project in the project\'s name space) and a single-user circle owned (and created by) the uid.  A user bob in project newclass will be a member of the circle bob:bob and newclass:newclass.

Bob will have the right to realize experiments in the bob:bob circle - that is bob can create experiments that only bob can access.  Bob will also have whatever rights the owner of the newclass project gave bob when he joined. If bob created the newstuff project, bob will have all rights in the newstuff:newstuff project.  No user has the right to add or remove users from their single-user circle.

Circles are used to control sharing as well.  If bob and alice are made a team in newclass, the owner of that project, e.g., their instructor or teaching assistant, can make them a circle under the project\'s namespace.  bob and alice may be made members of newclass:team1.  Now experiments realized in that circle will be accessible to bob and alice.

A user can create circles at will, but can only add users to a circle with their consent.  Because circles enable users to share resources (e.g., local files) the desire to share must be mutual.

The circle linked to a project is manipulated through the project, not as a circle.  Users added or removed to a project are added to or removed from its linked circle.  Permissions are also manipulated through the project interface.  A simple project may never need to create additional circles.


### Experiments

All of the testbed API is ultimately geared toward the creation of experiments.  An experiment contains many aspects that define its environment and operation.  An aspect is a user-defined piece of data that is used to influence the testbed\'s operation.  Some aspects are understood by the base testbed, and some must be interpreted by code supplied by the user.  Initially the prototype understands only a few aspects.  These include:

*  A description of the experimental environment
   topology of computers and other resources in which the experiment will take place, including infrastructure necessary to carryout and gather data from the experiment
*  A procedure to carry out
   the repeatable sequence of events and reaction to those events that tests a hypothesis.  This may also contain event descriptions and actions to take based n them.
 

Not all experiments in the sense of the API data structure will have all these elements.  Testbeds are often used to create an environment in which to try ideas out and explore ideas without intending to reproduce the experience.  The other end of the spectrum is rigorous, repeatable hypothesis testing.  The API supports both by allowing some of these aspects to be omitted for some experiments (in the API sense).

Experiments have unique names, scoped by the name of the user who created them or a project that the experiment is tied to.  Any user can create experiments scoped by their name.  The right to create experiments scoped by a project name is based on an explicit permission.  There is no functional difference between the two experiments, but projects that choose to exert editorial control of experiment content can do it by vetting experiments that members of that project create in the project space.

The experiment API is primarily concerned with:

 * Storing the experiment specification for repeated use
 * Sharing the experiment specification between users subject to [policy](NewPolicy.html)

We call the process of constructing the experiment\'s environment \'\'realizing\'\' the experiment.


### Libraries

A library is a group of related experiments analogous to a circle or project being a group of related users.  Libraries make relationships between experiments explicit so that users can operate on related experiments easily.  As with users, projects, and experiments, libraries have profiles attached that store human-readable metadata about their contents.

Libraries allow researchers and testbed administrators to group similar experiments and make them easy to find.  For example, a library of experiments for new testbed users can be maintained by testbed administrators.  A library of experiments for new members of a class or a research group can be maintained by the instructor or principal investigator.

Access to libraries is controlled using circles as above, so once created, administration of a library can be flexible.  Different libraries will have different policies.

As with experiments, the names of libraries are unique and scoped by a user or project name.  Only users delegated the explicit permission to create a library in the project name space may do so.  Project owners and managers may use this permission to vet libraries.

### Realizations

A realization of an experiment is a binding of experiment description to [resources](NewAPI#Resources.html) - it is an experiment in progress.  Realizations are made in the context of a circle that determines which researchers can manipulate the realization\'s resources.  Members of that circle can log into computers, manipulate the orchestration tools, etc.

Realizations are explicit objects because an experiment may be in progress more than once and used by different groups of researchers.

The realization API is primarily concerned with constructing an experiment\'s environment by calling out through the Testbed SPI

We call the process of constructing the experiment\'s environment \'\'realizing\'\' the experiment.

When realizing the experiment, the testbed uses the testbed API to configure and control the hardware and software that create the environment.

## The Containers API

The containers API is responsible for allocating resources to an experiment and configuring that hardware in ways appropriate for the user\'s goals - realizing it.

The key abstraction for realizing an experiment is the *container*.  A container holds some of a physical resource\'s computational and networking power and uses it to create part of the experiment at a level of realism appropriate to the researcher\'s goals.  A researcher who is interested in end-system behavior will not put very much computational power into realizing the routers that forward packets, but a lot into realizing end systems.

The containers API coordinates:

 * Allocating the resources from the testbed\'s pool of resources
 * Configuring the physcial resources to realize the experiment
 * Partitioning the computational and networking power of each resource into appropriate realizations
 * Assigning resources to carry out the experiment
 * Configuring resources using containers so that the experiment can be carried out successfully on limited resources
 * Initializing and supporting an experiment control system like MAGI to carry out the experiment\'s procedure, police invariants, and gather data.

This API supports communication between the testbed control system and the containers that make up an experiment being realized (and after it is realized).

### General Containers

A clear kind of container is a computer running a virtual machine monitor that partitions its CPU resources between virtual machines.  This is a very useful kind of container, but not the only one.  The containers interface is intended to support new forms of computer virtualization, efficient network simulation, or new kinds of physical hardware that can be part of testbeds.

To support these goals operations that the testbed can perform on a container are \'\'simple\'\' and \'\'can be specialized\'\'.  In particular, the operations guaranteed to work on any container are:

*  Start
    * Begin operating as an experiment element. (For a computer, boot; for a flux capacitor, begin travelling in time)
*  Stop
    * Become quiescent.  Only containers API requests will work on a stopped container
*  Describe
    * Tell the testbed what extended operations the container supports, the configuration format to use, and resources allocated to it
*  State
    * Tell the container state (see below)
*  Configure
    * Set up the container\'s internal state.  (For a computer, establish accounts and mount filesystems; for a flux capacitor, set travel rate and destination date)
*  Clean
    * Undo the effects of any Configure commands

A container can be in the following states:

*  Down
    * The container is in communication with the testbed, but not yet configured to act as an experiment element
*  Configured
    * The container is set up to act as an experiment element but is not yet doing so
*  Pinned
    * The container is not acting as an experiment element, but is carrying out an operation that renders it otherwise unusable.  For example, a container that is capturing its state may be in this state.
*  Up
    * The container is acting as an experiment element
*  None
    * Nothing is known about the container. is is (as yet) unresponsive.

Containers report state in response to a `describe` operation and can also spontaneously report changes in state to the testbed.

Generally a container\'s life cycle looks like:

 * Container starts in None state.
 * Hardware boots, containers code begins running on the resource, when that code comes up the container is Down and reports this state to the testbed.
 * The testbed asks the container to describe itself and sends an appropriate contfiguration request
 * When the configuration is successful the container moves to Configured and reports it
 * When the testbed tells the contatiner to start, it reports when it comes up and changes its state to Up. At this point, if an experiment control system like MAGI was part of the configuration, that system will take over running any experiment procedure.
 * When the experiment (or a phase of the experiment is done) the testbed can issue a Stop to the container, which will move to Configured.

From that point the testbed can issue Clean, Configure, and Start commands to adjust the container state and operation.

### Resources

Resources are the physical and conceptual objects managed by the testbed that are used to build experimental environments.  They are the computers, network ports, externally routable addresses, virtual machine images, et al. from which experiments are constructed.

Resources are a class of objects that are user to build experiments.  Now there are are a few well known resources that are visible to the SPI, but generic resource objects are also supported by the SPI and provide a way to integrate new building blocks.  Some of the specialized resources are:

 * computers
 * links
 * disk images
 * ssh keys

Access to and configuration of resources is affected by the circle(s) a user is acting as a member of.  When a user requests resources, thay specify the circle under which they are requesting them.  A user requesting resources as a member of a circle representing a university class may have access to different resources than one acting as a member of the testbed administration.  How membership affects the resources a group can claim is set by testbed policy.

In addition, the membership in circles controls how resources will be configured.  A resource in use by a particular circle will generally be configured to be accessible to all members of that circle.  A student who allocates resources as a member of a small design group while implementing a class project may later allocate resources as a member of the whole class when presenting the work to the class\'s TA and professor.


### Configuration and Specialization

The most common form of container specialization is the configuration format that the container expects. We have defined formats for containers that look like computers that define accounts, filesystems, software installations, etc. Other configuration formats will evolve and move into the mainstream.

In addition we define low-level commands in an experiment description that bind a configuration message content to a node in an experiment.  This allows new container configurations to be communicates even when the testbed does not understand them.

In addition to custom configuration systems, containers can export custom operations.  For example, some virtual machines support snapshotting their state.  That can be useful to experimenters.  Again containers can advertise operations that the testbed is unaware of and the testbed API presents a way for users to pass a request through to a container directly.


## Putting It All Together

To understand how the pieces above fit together, here is an example of a researcher using DETER.

A person has an idea for research they would like to to conduct on DETER.  That researcher accesses a DETER user interface - for example the DETER web interface - and fills out a form that gathers information about them and their proposed research.  The web interface translates this into a request to create a user and then to create a vetting project for that user.  The information about the research is routed to the DETER administration.

After the DETER administration decides that the research is reasonable and meets their criteria, they authorize creation of the vetting project.  The administration sends the user e-mail telling them they can use DETER.  Additionally the testbed may add the new user to a project for new users or other projects for researchers in similar fields.

When the user visits the DETER web interface, it notices that the user is a member of the new users project and presents an opportunity to take a tutorial.  In addition, the user has access to sample experiments accessible to users in the new user\'s project.  These experiments are organized into samples based on their membership in libraries accessible by the new user.  The library structure helps the tutorial writer guide the user through the sample experiments. Working from the tutorial and documentation and starting from a sample experiment, the user may create a new experiment with an environment, constraints, procedure and data collection specced out.

The web interface uses the testbed API to review an experiment in the new users project, copy it to the user\'s vetting project and edit the new experiment to meet the user\'s goals.

The user then realizes the experiment using the testbed API, uses the experiment control API - usually MAGI - to carry out the experiment and collect the data.  When the experiment is complete, the testbed API is used to release resources.

Now the user assesses the data, and may go off to publish results or create a new experiment from this one that tests a different hypothesis or something else.


## More Information

From here, readers may be interested in the

 * [Detailed Specification of the testbed API](NewTestbedAPISpec.html)
 * [Detailed Specification of the containers API](NewContainersAPISpec.html)
 * [Implementation plan/roadmap](NewImpl.html)
   * [Topology Description](TopologyDescription.html)
 * [Implementation Notes](NewImplNotes.html)
 * [UI Implementation Notes](UIImplNotes.html)
