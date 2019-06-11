# spring-boot-task-assigning
An example of Task Assigning using OptaPlanner and SpringBoot

This example uses SolverManager, a high-level wrapper for the low-level
Solver to handle thread management and multitenancy.

SolverManager is going to be the public API, and an example of how it is going
to be used is in TaskAssigningSolverManagerService.

    NOTE: To get the project working you need 7.24.0-SNAPSHOT version of optaplanner-core
                with this bug fix: https://github.com/kiegroup/optaplanner/pull/511 in your local repository
