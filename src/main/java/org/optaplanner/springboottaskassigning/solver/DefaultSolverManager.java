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

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
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

    private ExecutorService solverExecutorService;
    private ExecutorService eventHandlerExecutorService;
    private SolverFactory<Solution_> solverFactory;
    private Map<Object, SolverTask<Solution_>> problemIdToSolverTaskMap;
    private Map<Object, CompletableFuture<Solution_>> problemIdToCompletableFutureMap;

    // TODO SolverManager should be SOLVER_CONFIG agnostic, it should take the solver configuration as a constructor argument
    // TODO i.e. InputStream/File ...
    public DefaultSolverManager() {
        solverFactory = SolverFactory.createFromXmlResource(SOLVER_CONFIG, DefaultSolverManager.class.getClassLoader());
        problemIdToSolverTaskMap = new ConcurrentHashMap<>();
        problemIdToCompletableFutureMap = new ConcurrentHashMap<>();
        int numAvailableProcessors = Runtime.getRuntime().availableProcessors();
        logger.info("Number of available processors: {}.", numAvailableProcessors);
        solverExecutorService = Executors.newFixedThreadPool(numAvailableProcessors - 1);
        eventHandlerExecutorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public synchronized void solve(Object problemId, Solution_ planningProblem,
                                   Consumer<Solution_> onBestSolutionChangedEvent, Consumer<Solution_> onSolvingEnded) {
        if (problemIdToSolverTaskMap.containsKey(problemId)) {
            throw new IllegalArgumentException("Problem (" + problemId + ") already exists.");
        }
        SolverTask<Solution_> newSolverTask = new SolverTask<>(problemId, solverFactory.buildSolver(), planningProblem);
        // TODO implement throttling
        if (onBestSolutionChangedEvent != null) {
            newSolverTask.addEventListener(
                    bestSolutionChangedEvent ->
                            eventHandlerExecutorService.submit(
                                    () -> onBestSolutionChangedEvent.accept(bestSolutionChangedEvent.getNewBestSolution())));
        }

        CompletableFuture<Solution_> solverFuture = CompletableFuture.supplyAsync(newSolverTask::startSolving, solverExecutorService);
        solverFuture.handle((solution_, throwable) -> {
            if (throwable != null) {
                throw new RuntimeException("Error in handling CompletableFuture of problem (" + problemId + ").", throwable.getCause());
            }
            return eventHandlerExecutorService.submit(() -> onSolvingEnded.accept(solution_));
        });
        problemIdToSolverTaskMap.put(problemId, newSolverTask);
        problemIdToCompletableFutureMap.put(problemId, solverFuture);
        logger.info("A new solver task was created with problemId ({}).", problemId);
    }

    @Override
    public void stopSolver(Object problemId) {
        if (!problemIdToSolverTaskMap.containsKey(problemId)) {
            throw new IllegalArgumentException("Solver (" + problemId + ") does not exist.");
        }
        stopSolverTask(problemIdToSolverTaskMap.get(problemId));
    }

    @Override
    public Solution_ getBestSolution(Object problemId) {
        logger.debug("Getting best solution of problemId ({}).", problemId);
        SolverTask<Solution_> solverTask = problemIdToSolverTaskMap.get(problemId);
        return solverTask == null ? null : solverTask.getBestSolution();
    }

    @Override
    public Score getBestScore(Object problemId) {
        logger.debug("Getting best score of problemId ({}).", problemId);
        SolverTask<Solution_> solverTask = problemIdToSolverTaskMap.get(problemId);
        return solverTask == null ? null : solverTask.getBestScore();
    }

    @Override
    public SolverStatus getSolverStatus(Object problemId) {
        logger.debug("Getting solver status of problemId ({}).", problemId);
        SolverTask<Solution_> solverTask = problemIdToSolverTaskMap.get(problemId);
        return solverTask == null ? null : solverTask.getSolverStatus();
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
    public void shutdown() {
        logger.info("Shutting down {}.", DefaultSolverManager.class.getName());
        stopSolvers();
        solverExecutorService.shutdown();
        eventHandlerExecutorService.shutdown();
        Long awaitingDuration = 1L;
        try {
            if (!solverExecutorService.awaitTermination(awaitingDuration, TimeUnit.SECONDS)
                    || !eventHandlerExecutorService.awaitTermination(awaitingDuration, TimeUnit.SECONDS)) {
                logger.info("Still waiting shutdown after {} second, calling shutdownNow().", awaitingDuration);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("SolverManager thread interrupted while awaiting termination.", e);
        } finally {
            solverExecutorService.shutdownNow();
            eventHandlerExecutorService.shutdownNow();
        }
    }

    private void stopSolvers() {
        for (SolverTask solverTask : problemIdToSolverTaskMap.values()) {
            stopSolverTask(solverTask);
        }
    }

    private void stopSolverTask(SolverTask solverTask) {
        solverTask.stopSolver();
        // No need to call solverFuture.get() to propagate exceptions since they are handled in solve() -> solverFuture.handle()
    }
}
