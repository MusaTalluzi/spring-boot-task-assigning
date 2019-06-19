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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.api.solver.event.SolverEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultSolverManager<Solution_> implements SolverManager<Solution_> {

    private static final Logger logger = LoggerFactory.getLogger(DefaultSolverManager.class);

    public static final String SOLVER_CONFIG = "org/optaplanner/springboottaskassigning/solver/taskAssigningSolverConfig.xml";

    private ExecutorService executorService;
    private SolverFactory<Solution_> solverFactory;
    private Map<Comparable<?>, SolverTask<Solution_>> tenantIdToSolverTaskMap;

    // TODO SolverManager should be SOLVER_CONFIG agnostic, it should take the solver configuration as a constructor argument
    // TODO i.e. InputStream/File ...
    public DefaultSolverManager() {
        solverFactory = SolverFactory.createFromXmlResource(SOLVER_CONFIG, DefaultSolverManager.class.getClassLoader());
        tenantIdToSolverTaskMap = new HashMap<>();
        int numAvailableProcessors = Runtime.getRuntime().availableProcessors();
        logger.info("Number of available processors: {}.", numAvailableProcessors);
        executorService = Executors.newFixedThreadPool(numAvailableProcessors - 2);
    }

    @Override
    public void solve(Comparable<?> tenantId, Solution_ planningProblem,
                      Consumer<Solution_> onBestSolutionChangedEvent, Consumer<Solution_> onSolvingEnded) {
        synchronized (this) {
            if (tenantIdToSolverTaskMap.containsKey(tenantId)) {
                throw new IllegalArgumentException("Tenant id (" + tenantId + ") already exists.");
            }
            SolverTask<Solution_> newSolverTask = new SolverTask<>(tenantId, solverFactory.buildSolver(), planningProblem,
                    onSolvingEnded);
            // TODO implement throttling
            if (onBestSolutionChangedEvent != null) {
                newSolverTask.addEventListener(bestSolutionChangedEvent -> onBestSolutionChangedEvent.accept(bestSolutionChangedEvent.getNewBestSolution()));
            }
            executorService.submit(newSolverTask);
            tenantIdToSolverTaskMap.put(tenantId, newSolverTask);
            logger.info("A new solver task was created with tenantId ({}).", tenantId);
        }
    }

    @Override
    public Optional<Solution_> getBestSolution(Comparable<?> tenantId) {
        logger.debug("Getting best solution of tenantId ({}).", tenantId);
        SolverTask<Solution_> solverTask = tenantIdToSolverTaskMap.get(tenantId);
        return solverTask == null ? Optional.empty() : Optional.ofNullable(solverTask.getBestSolution());
    }

    @Override
    public Optional<Score> getBestScore(Comparable<?> tenantId) {
        logger.debug("Getting best score of tenantId ({}).", tenantId);
        SolverTask<Solution_> solverTask = tenantIdToSolverTaskMap.get(tenantId);
        return solverTask == null ? Optional.empty() : Optional.ofNullable(solverTask.getBestScore());
    }

    @Override
    public Optional<SolverStatus> getSolverStatus(Comparable<?> tenantId) {
        logger.debug("Getting solver status of tenantId ({}).", tenantId);
        SolverTask<Solution_> solverTask = tenantIdToSolverTaskMap.get(tenantId);
        return solverTask == null ? Optional.empty() : Optional.ofNullable(solverTask.getSolverStatus());
    }

    @Override
    public void addEventListener(Comparable<?> tenantId, SolverEventListener<Solution_> eventListener) {
        logger.debug("Adding an event listener for tenantId ({}).", tenantId);
        SolverTask<Solution_> solverTask = tenantIdToSolverTaskMap.get(tenantId);
        if (solverTask != null) {
            solverTask.addEventListener(eventListener);
        } else {
            logger.error("Tenant ({}) does not have a SolverTask submitted.", tenantId);
        }
    }

    @Override
    public void shutdown() throws InterruptedException {
        logger.info("Shutting down {}.", DefaultSolverManager.class.getName());
        executorService.shutdown();
        Long awaitingDuration = 1L;
        if (!executorService.awaitTermination(awaitingDuration, TimeUnit.SECONDS)) {
            logger.info("Still waiting shutdown after {} second, calling shutdownNow().", awaitingDuration);
            executorService.shutdownNow();
        }
    }
}
