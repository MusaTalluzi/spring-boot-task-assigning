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

package org.optaplanner.springboottaskassigning.solver;

import java.util.Optional;
import java.util.function.Consumer;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.solver.event.SolverEventListener;

public interface SolverManager<Solution_> {

    // FIXME CRITICAL BUG: iterate over planning solution entities and set tenantId, or induce it from solution.getTenantId()
    // TODO Replace tenantId with solverId
    void solve(Comparable<?> tenantId, Solution_ planningSolution,
               Consumer<Solution_> onBestSolutionChangedEvent, Consumer<Solution_> onSolvingEnded);

    Optional<Solution_> getBestSolution(Comparable<?> tenantId);

    Optional<Score> getBestScore(Comparable<?> tenantId);

    Optional<SolverStatus> getSolverStatus(Comparable<?> tenantId);

    void addEventListener(Comparable<?> tenantId, SolverEventListener<Solution_> eventListener);

    void shutdown() throws InterruptedException;
}
