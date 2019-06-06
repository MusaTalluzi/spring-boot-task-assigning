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

import java.util.function.Consumer;

import javax.annotation.PostConstruct;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.springboottaskassigning.domain.TaskAssigningSolution;
import org.optaplanner.springboottaskassigning.domain.TaskAssigningSolutionRepository;
import org.optaplanner.springboottaskassigning.solver.SolverManager;
import org.optaplanner.springboottaskassigning.solver.SolverStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaskAssigningSolverManagerService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final TaskAssigningSolutionRepository taskAssigningSolutionRepository;
    private final Consumer<TaskAssigningSolution> onBestSolutionChangedEvent;
    private final Consumer<TaskAssigningSolution> onSolvingEnded;

    @Autowired
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
                taskAssigningSolutionRepository.save(taskAssigningSolution);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        };
    }

    @PostConstruct
    public void loadProblemsAndStartSolving() {
        taskAssigningSolutionRepository.findAll()
                .forEach(taskAssigningSolution -> solve(taskAssigningSolution.getTenantId(), taskAssigningSolution));
    }

    public void solve(Long tenantId, TaskAssigningSolution planningProblem) {
        solverManager.solve(tenantId, planningProblem, onBestSolutionChangedEvent, onSolvingEnded);
    }

    public TaskAssigningSolution bestSolution(Long tenantId) {
        return solverManager.getBestSolution(tenantId);
    }

    public Score bestScore(Long tenantId) {
        return solverManager.getBestScore(tenantId);
    }

    public SolverStatus solverStatus(Long tenantId) {
        return solverManager.getSolverStatus(tenantId);
    }
}
