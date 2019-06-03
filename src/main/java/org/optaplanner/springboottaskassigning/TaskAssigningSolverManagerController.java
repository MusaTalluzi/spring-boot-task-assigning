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

import org.optaplanner.core.api.score.Score;
import org.optaplanner.springboottaskassigning.domain.TaskAssigningSolution;
import org.optaplanner.springboottaskassigning.domain.TaskAssigningSolutionRepository;
import org.optaplanner.springboottaskassigning.solver.SolverManager;
import org.optaplanner.springboottaskassigning.solver.SolverStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenants/{tenantId}/solver")
public class TaskAssigningSolverManagerController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final TaskAssigningSolutionRepository taskAssigningSolutionRepository;
    private final Consumer<TaskAssigningSolution> onBestSolutionChangedEvent;
    private final Consumer<TaskAssigningSolution> onSolvingEnded;

    @Autowired
    private SolverManager<TaskAssigningSolution> solverManager;

    public TaskAssigningSolverManagerController(TaskAssigningSolutionRepository taskAssigningSolutionRepository) {
        this.taskAssigningSolutionRepository = taskAssigningSolutionRepository;
        onBestSolutionChangedEvent = taskAssigningSolutionRepository::save;
        onSolvingEnded = taskAssigningSolutionRepository::save;
    }

    @PostMapping
    public void solve(@PathVariable Comparable<?> tenantId, @RequestBody TaskAssigningSolution planningProblem) {
        taskAssigningSolutionRepository.save(planningProblem);
        solverManager.solve(tenantId, planningProblem, onBestSolutionChangedEvent, onSolvingEnded);
    }

    @GetMapping("/bestSolution")
    public TaskAssigningSolution bestSolution(@PathVariable Comparable<?> tenantId) {
        return solverManager.getBestSolution(tenantId);
    }

    @GetMapping("/bestScore")
    public Score bestScore(@PathVariable Comparable<?> tenantId) {
        return solverManager.getBestScore(tenantId);
    }

    @GetMapping("/status")
    public SolverStatus solverStatus(@PathVariable Comparable<?> tenantId) {
        return solverManager.getSolverStatus(tenantId);
    }
}