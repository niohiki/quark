Quark v1.0
==========

Quark is a 2D pure Scala game engine.


Create a project
================

Run `sbt "startup [project-name] [organization.name]"` (from http://www.scala-sbt.org) in the main Quark folder. The new project will be created under `quark-projects`.

Once this is done, run `sbt` (in the main folder also), and the list of projects will be available with the command `projects`. Use
`project [project-name]` to switch to it, and `dist` to generate a distribution version of the game (in `quark-projects/[project-name]/dist`) that already includes the engine lib. For development, use usual sbt commands such as `run` and `compile`.

Files are generated automatically so that the new project can be imported (and run) as an Eclipse project, that makes automatic reference to the Quark source. This can be useful to tweak the source to your needs, but beware, it will affect every project.
