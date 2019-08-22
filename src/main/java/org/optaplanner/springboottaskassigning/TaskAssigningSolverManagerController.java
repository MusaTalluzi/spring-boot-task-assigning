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
import java.util.Set;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.springboottaskassigning.domain.TaskAssigningSolution;
import org.optaplanner.springboottaskassigning.solver.SolverStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/tenants")
public class TaskAssigningSolverManagerController {

    @Autowired
    private TaskAssigningSolverManagerService solverManagerService;

    @GetMapping
    public Set<Long> getSubmittedTenantsIds() {
        return solverManagerService.getSubmittedTenantsIds();
    }

    @PostMapping("/{problemId}/solver")
    public void solve(@PathVariable Long problemId, @RequestBody TaskAssigningSolution planningProblem) {
        if (!solverManagerService.solve(problemId, planningProblem)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Problem (" + problemId + ") already exists.");
        }
    }

    @PostMapping("/{problemId}/solver/generate/{taskListSize}/{employeeListSize}")
    void solve(@PathVariable Long problemId, @PathVariable int taskListSize, @PathVariable int employeeListSize) {
        if (!solverManagerService.solve(problemId, taskListSize, employeeListSize)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Problem (" + problemId + ") already exists.");
        }
    }

    @GetMapping("/{problemId}/solver/bestSolution")
    public TaskAssigningSolution bestSolution(@PathVariable Long problemId) {
        TaskAssigningSolution bestSolution = solverManagerService.getBestSolution(problemId);
        if (bestSolution == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Problem (" + problemId + ") does not have a solver task submitted or solving has not started yet.");
        }
        return bestSolution;
    }

    @GetMapping("/{problemId}/solver/bestScore")
    public Score bestScore(@PathVariable Long problemId) {
        Score score = solverManagerService.getBestScore(problemId);
        if (score == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Problem (" + problemId + ") does not have a solver task submitted or solving has not started yet.");
        }
        return score;
    }

    @GetMapping("/{problemId}/solver/status")
    public SolverStatus solverStatus(@PathVariable Long problemId) {
        SolverStatus status = solverManagerService.getSolverStatus(problemId);
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Problem (" + problemId + ") does not have a solver task submitted or solving has not started yet.");
        }
        return status;
    }
}