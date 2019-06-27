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
@RequestMapping("/tenants/{tenantId}/solver")
public class TaskAssigningSolverManagerController {

    @Autowired
    private TaskAssigningSolverManagerService solverManagerService;

    @PostMapping
    public void solve(@PathVariable Long tenantId, @RequestBody TaskAssigningSolution planningProblem) {
        solverManagerService.solve(tenantId, planningProblem);
    }

    @GetMapping("/bestSolution")
    public TaskAssigningSolution bestSolution(@PathVariable Long tenantId) {
        try {
            return solverManagerService.getBestSolution(tenantId);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Tenant (" + tenantId + ") does not have a solver task submitted or solving has not started yet.", e);
        }
    }

    @GetMapping("/bestScore")
    public Score bestScore(@PathVariable Long tenantId) {
        try {
            return solverManagerService.getBestScore(tenantId);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Tenant (" + tenantId + ") does not have a solver task submitted or solving has not started yet.", e);
        }
    }

    @GetMapping("/status")
    public SolverStatus solverStatus(@PathVariable Long tenantId) {
        try {
            return solverManagerService.getSolverStatus(tenantId);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Tenant (" + tenantId + ") does not have a solver task submitted or solving has not started yet.", e);
        }
    }
}