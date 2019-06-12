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

import java.util.NoSuchElementException;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.springboottaskassigning.domain.TaskAssigningSolution;
import org.optaplanner.springboottaskassigning.solver.DefaultSolverManager;
import org.optaplanner.springboottaskassigning.solver.SolverManager;
import org.optaplanner.springboottaskassigning.solver.SolverStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TaskAssigningSolverManagerService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final TaskAssigningSolutionRepository taskAssigningSolutionRepository;
    private final Consumer<TaskAssigningSolution> onBestSolutionChangedEvent;
    private final Consumer<TaskAssigningSolution> onSolvingEnded;

    private SolverManager<TaskAssigningSolution> solverManager;

    public TaskAssigningSolverManagerService(TaskAssigningSolutionRepository taskAssigningSolutionRepository) {
        this.taskAssigningSolutionRepository = taskAssigningSolutionRepository;
        onBestSolutionChangedEvent = taskAssigningSolution -> {
            logger.debug("Best solution changed.");
            try {
                taskAssigningSolutionRepository.save(taskAssigningSolution);
            } catch (Exception e) {
                e.printStackTrace();
                // FIXME the exception is eaten and not propagated properly, duplicate by using older version of optaplanner-persistence-jpa
                throw new RuntimeException(e);
            }
        };
        onSolvingEnded = taskAssigningSolution -> {
            logger.debug("Solving ended.");
            try {
                // FIXME org.hibernate.StaleObjectStateException: Row was updated or deleted by another transaction (or unsaved-value mapping was incorrect)
                // cause: when saving after bestSolutionChangedEvent, persisted solution version is different than in memory solution version
                // which is saved again here.
                taskAssigningSolutionRepository.save(taskAssigningSolution);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        };
    }

    @PostConstruct
    public void loadExistingProblemsAndStartSolving() {
        solverManager = new DefaultSolverManager<>();
        taskAssigningSolutionRepository.findAll()
                .forEach(taskAssigningSolution -> solve(taskAssigningSolution.getTenantId(), taskAssigningSolution));
    }

    @PreDestroy
    public void tearDown() throws InterruptedException {
        solverManager.shutdown();
    }

    public void solve(Long tenantId, TaskAssigningSolution planningProblem) {
        solverManager.solve(tenantId, planningProblem, onBestSolutionChangedEvent, onSolvingEnded);
    }

    public TaskAssigningSolution bestSolution(Long tenantId) throws NoSuchElementException {
        return solverManager.getBestSolution(tenantId).get();
    }

    public Score bestScore(Long tenantId) throws NoSuchElementException {
        return solverManager.getBestScore(tenantId).get();
    }

    public SolverStatus solverStatus(Long tenantId) throws NoSuchElementException {
        return solverManager.getSolverStatus(tenantId).get();
    }
}
