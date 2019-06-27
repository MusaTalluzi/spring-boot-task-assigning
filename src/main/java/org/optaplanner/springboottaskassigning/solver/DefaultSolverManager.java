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
    private Map<Object, SolverTask<Solution_>> problemIdToSolverTaskMap;

    // TODO SolverManager should be SOLVER_CONFIG agnostic, it should take the solver configuration as a constructor argument
    // TODO i.e. InputStream/File ...
    public DefaultSolverManager() {
        solverFactory = SolverFactory.createFromXmlResource(SOLVER_CONFIG, DefaultSolverManager.class.getClassLoader());
        problemIdToSolverTaskMap = new HashMap<>();
        int numAvailableProcessors = Runtime.getRuntime().availableProcessors();
        logger.info("Number of available processors: {}.", numAvailableProcessors);
        executorService = Executors.newFixedThreadPool(numAvailableProcessors - 2);
    }

    @Override
    public void solve(Object problemId, Solution_ planningProblem,
                      Consumer<Solution_> onBestSolutionChangedEvent, Consumer<Solution_> onSolvingEnded) {
        synchronized (this) {
            if (problemIdToSolverTaskMap.containsKey(problemId)) {
                throw new IllegalArgumentException("Problem (" + problemId + ") already exists.");
            }
            SolverTask<Solution_> newSolverTask = new SolverTask<>(problemId, solverFactory.buildSolver(), planningProblem,
                    onSolvingEnded);
            // TODO implement throttling
            if (onBestSolutionChangedEvent != null) {
                newSolverTask.addEventListener(bestSolutionChangedEvent -> onBestSolutionChangedEvent.accept(bestSolutionChangedEvent.getNewBestSolution()));
            }
            executorService.submit(newSolverTask);
            problemIdToSolverTaskMap.put(problemId, newSolverTask);
            logger.info("A new solver task was created with problemId ({}).", problemId);
        }
    }

    @Override
    public Optional<Solution_> getBestSolution(Object problemId) {
        logger.debug("Getting best solution of problemId ({}).", problemId);
        SolverTask<Solution_> solverTask = problemIdToSolverTaskMap.get(problemId);
        return solverTask == null ? Optional.empty() : Optional.ofNullable(solverTask.getBestSolution());
    }

    @Override
    public Optional<Score> getBestScore(Object problemId) {
        logger.debug("Getting best score of problemId ({}).", problemId);
        SolverTask<Solution_> solverTask = problemIdToSolverTaskMap.get(problemId);
        return solverTask == null ? Optional.empty() : Optional.ofNullable(solverTask.getBestScore());
    }

    @Override
    public Optional<SolverStatus> getSolverStatus(Object problemId) {
        logger.debug("Getting solver status of problemId ({}).", problemId);
        SolverTask<Solution_> solverTask = problemIdToSolverTaskMap.get(problemId);
        return solverTask == null ? Optional.empty() : Optional.ofNullable(solverTask.getSolverStatus());
    }

    @Override
    public void addEventListener(Object problemId, SolverEventListener<Solution_> eventListener) {
        logger.debug("Adding an event listener for problemId ({}).", problemId);
        SolverTask<Solution_> solverTask = problemIdToSolverTaskMap.get(problemId);
        if (solverTask != null) {
            solverTask.addEventListener(eventListener);
        } else {
            logger.error("Problem ({}) does not have a SolverTask submitted.", problemId);
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
