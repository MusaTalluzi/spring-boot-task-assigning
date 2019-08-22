/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.optaplanner.springboottaskassigning;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.springboottaskassigning.domain.Employee;
import org.optaplanner.springboottaskassigning.domain.Task;
import org.optaplanner.springboottaskassigning.domain.TaskAssigningSolution;
import org.optaplanner.springboottaskassigning.domain.TaskOrEmployee;
import org.optaplanner.springboottaskassigning.repository.TaskAssigningSolutionRepository;
import org.optaplanner.springboottaskassigning.repository.TaskRepository;
import org.optaplanner.springboottaskassigning.solver.DefaultSolverManager;
import org.optaplanner.springboottaskassigning.solver.SolverManager;
import org.optaplanner.springboottaskassigning.solver.SolverStatus;
import org.optaplanner.springboottaskassigning.utils.TaskAssigningGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskAssigningSolverManagerService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final TaskAssigningSolutionRepository taskAssigningSolutionRepository;
    private final TaskRepository taskRepository;
    private final Consumer<TaskAssigningSolution> onBestSolutionChangedEvent;
    private final Consumer<TaskAssigningSolution> onSolvingEnded;

    private SolverManager<TaskAssigningSolution> solverManager;

    public TaskAssigningSolverManagerService(TaskAssigningSolutionRepository taskAssigningSolutionRepository,
                                             TaskRepository taskRepository) {
        this.taskAssigningSolutionRepository = taskAssigningSolutionRepository;
        this.taskRepository = taskRepository;
        solverManager = new DefaultSolverManager<>(DefaultSolverManager.SOLVER_CONFIG);

        onBestSolutionChangedEvent = taskAssigningSolution -> {
            logger.debug("Best solution changed.");
            try {
                // TODO: sync-up with Jiri about StaleObjectStateException
                updateSolution(taskAssigningSolution);
            } catch (Exception e) {
                logger.error("Error in onBestSolutionChangedEvent listener", e);
                // FIXME the exception is eaten and not propagated properly, duplicate by using older version of optaplanner-persistence-jpa
                // reason: this method is executed using eventHandlerExecutorService.submit()
                throw new RuntimeException(e);
            }
        };
        onSolvingEnded = taskAssigningSolution -> {
            logger.debug("Solving ended.");
            try {
                updateSolution(taskAssigningSolution);
            } catch (Exception e) {
                logger.error("Error in onSolvingEnded listener", e);
                throw new RuntimeException(e);
            }
        };
    }

    @Transactional
    private void updateSolution(TaskAssigningSolution taskAssigningSolution) {
        Long tenantId = taskAssigningSolution.getTenantId();
        Optional<TaskAssigningSolution> solutionEntityOptional = taskAssigningSolutionRepository.findById(taskAssigningSolution.getId());
        if (solutionEntityOptional.isPresent()) {
            TaskAssigningSolution solutionEntity = solutionEntityOptional.get();
            if (taskAssigningSolution.getScore() != null && !taskAssigningSolution.getScore().equals(solutionEntity.getScore())) {
                solutionEntity.setScore(taskAssigningSolution.getScore());
                taskAssigningSolutionRepository.save(solutionEntity);
            }
            // Update: Tasks (nextTask, previousTaskOrEmployee, employee, start&EndTime) and Employees (nextTask)
            Map<Long, Task> taskEntityMap = solutionEntity.getTaskList()
                    .stream().parallel().collect(Collectors.toConcurrentMap(Task::getId, task -> task));
            Map<Long, Employee> employeeEntityMap = solutionEntity.getEmployeeList()
                    .stream().parallel().collect(Collectors.toConcurrentMap(Employee::getId, Function.identity()));

            taskAssigningSolution.getTaskList().stream().parallel()
                    .filter(task -> task.getPreviousTaskOrEmployee() != null)
                    .filter(task -> !task.getPreviousTaskOrEmployee().equals(taskEntityMap.get(task.getId()).getPreviousTaskOrEmployee()))
                    .forEach(task -> updateTask(task, taskEntityMap.get(task.getId()), taskEntityMap, employeeEntityMap));
        } else {
            logger.error("Trying to update solution ({}) that does not exist.", tenantId);
        }
    }

    private void updateTask(Task newTask, Task taskEntity, Map<Long, Task> taskEntityMap, Map<Long, Employee> employeeEntityMap) {
        TaskOrEmployee newPreviousTaskOrEmployee = newTask.getPreviousTaskOrEmployee();
        if (newPreviousTaskOrEmployee instanceof Task) {
            taskEntity.setPreviousTaskOrEmployee(taskEntityMap.get(newPreviousTaskOrEmployee.getId()));
        } else if (newPreviousTaskOrEmployee instanceof Employee) {
            taskEntity.setPreviousTaskOrEmployee(employeeEntityMap.get(newPreviousTaskOrEmployee.getId()));
        } else {
            throw new IllegalArgumentException("previousTaskOrEmployee of task (" + newTask.getId() + ") is neither " +
                    "an Employee nor a Task.");
        }
        taskEntity.getPreviousTaskOrEmployee().setNextTask(taskEntity);

        taskEntity.setNextTask(Objects.isNull(newTask.getNextTask()) ? null
                : taskEntityMap.get(newTask.getNextTask().getId()));
        if (Objects.nonNull(taskEntity.getNextTask())) {
            taskEntity.getNextTask().setPreviousTaskOrEmployee(taskEntity);
        }

        taskEntity.setEmployee(Objects.isNull(newTask.getEmployee()) ? null
                : employeeEntityMap.get(newTask.getEmployee().getId()));
        taskEntity.setStartTime(newTask.getStartTime());
        taskEntity.setEndTime(newTask.getEndTime());
        taskRepository.save(taskEntity);
    }

    @PostConstruct
    public void loadExistingProblemsAndStartSolving() {
        List<TaskAssigningSolution> solutionList = taskAssigningSolutionRepository.findAll();
        solutionList
                .forEach(taskAssigningSolution -> solve(taskAssigningSolution.getTenantId(), taskAssigningSolution));
    }

    @PreDestroy
    public void tearDown() {
        solverManager.shutdown();
    }

    public Set<Long> getSubmittedTenantsIds() {
        return (Set<Long>)(Set<?>)solverManager.getProblemIds();
    }

    public boolean solve(Long problemId, int taskListSize, int employeeListSize) {
        TaskAssigningSolution generatedPlanningProblem =
                new TaskAssigningGenerator(problemId).createTaskAssigningSolution(taskListSize, employeeListSize);
        return solve(problemId, generatedPlanningProblem);
    }

    public boolean solve(Long problemId, TaskAssigningSolution planningProblem) {
        if (solverManager.isProblemSubmitted(problemId)) {
            return false;
        }
        taskAssigningSolutionRepository.save(planningProblem);
        solverManager.solve(problemId, planningProblem, onBestSolutionChangedEvent, onSolvingEnded);
        return true;
    }

    public TaskAssigningSolution getBestSolution(Long problemId) throws NoSuchElementException {
        return solverManager.getBestSolution(problemId);
    }

    public Score getBestScore(Long problemId) throws NoSuchElementException {
        return solverManager.getBestScore(problemId);
    }

    public SolverStatus getSolverStatus(Long problemId) throws NoSuchElementException {
        return solverManager.getSolverStatus(problemId);
    }
}
