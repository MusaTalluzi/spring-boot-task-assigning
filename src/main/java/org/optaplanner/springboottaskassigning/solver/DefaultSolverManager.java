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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    // TODO SolverManager should be SOLVER_CONFIG agnostic, it should take the solver configuration as a constructor argument
    // TODO i.e. InputStream/File ...
    public DefaultSolverManager() {
        solverFactory = SolverFactory.createFromXmlResource(SOLVER_CONFIG, DefaultSolverManager.class.getClassLoader());
        problemIdToSolverTaskMap = new ConcurrentHashMap<>();
        int numAvailableProcessors = Runtime.getRuntime().availableProcessors();
        logger.info("Number of available processors: {}.", numAvailableProcessors);
        solverExecutorService = Executors.newFixedThreadPool(numAvailableProcessors - 1);
        eventHandlerExecutorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public void solve(Object problemId,
                      Solution_ planningProblem,
                      Consumer<Solution_> onBestSolutionChangedEvent,
                      Consumer<Solution_> onSolvingEnded) {
        solve(problemId, planningProblem, onBestSolutionChangedEvent, onSolvingEnded, null);
    }

    @Override
    public void solve(Object problemId,
                      Solution_ planningProblem,
                      Consumer<Solution_> onBestSolutionChangedEvent,
                      Consumer<Solution_> onSolvingEnded,
                      Consumer<Throwable> onException) {
        SolverTask<Solution_> newSolverTask;
        synchronized (this) {
            if (isProblemSubmitted(problemId)) {
                throw new IllegalArgumentException("Problem (" + problemId + ") already exists.");
            }
            newSolverTask = new SolverTask<>(problemId, solverFactory.buildSolver(), planningProblem);
            problemIdToSolverTaskMap.put(problemId, newSolverTask);
            logger.info("A new solver task was created with problemId ({}).", problemId);
        }

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
                logger.error("Exception while solving problem (" + problemId + ").", throwable.getCause());
                if (onException != null) {
                    eventHandlerExecutorService.submit(() -> onException.accept(throwable.getCause()));
                }
            }
            return eventHandlerExecutorService.submit(() -> onSolvingEnded.accept(solution_));
        });
    }

    @Override
    public void stopSolver(Object problemId) {
        logger.debug("Stopping solver of problemId ({}).", problemId);
        SolverTask<Solution_> solverTask;
        synchronized (this) {
            if (!isProblemSubmitted(problemId)) {
                throw new IllegalArgumentException("Problem (" + problemId + ") was not submitted.");
            }
            solverTask = problemIdToSolverTaskMap.get(problemId);
        }
        if (solverTask != null) {
            solverTask.stopSolver();
        }
    }

    @Override
    public boolean isProblemSubmitted(Object problemId) {
        return problemIdToSolverTaskMap.containsKey(problemId);
    }

    @Override
    public Solution_ getBestSolution(Object problemId) {
        logger.debug("Getting best solution of problemId ({}).", problemId);
        SolverTask<Solution_> solverTask;
        synchronized (this) {
            if (!isProblemSubmitted(problemId)) {
                logger.error("Problem (" + problemId + ") was not submitted.");
            }
            solverTask = problemIdToSolverTaskMap.get(problemId);
        }
        return solverTask == null ? null : solverTask.getBestSolution();
    }

    @Override
    public Score getBestScore(Object problemId) {
        logger.debug("Getting best score of problemId ({}).", problemId);
        SolverTask<Solution_> solverTask;
        synchronized (this) {
            if (!isProblemSubmitted(problemId)) {
                logger.error("Problem (" + problemId + ") was not submitted.");
            }
            solverTask = problemIdToSolverTaskMap.get(problemId);
        }
        return solverTask == null ? null : solverTask.getBestScore();
    }

    @Override
    public SolverStatus getSolverStatus(Object problemId) {
        logger.debug("Getting solver status of problemId ({}).", problemId);
        SolverTask<Solution_> solverTask;
        synchronized (this) {
            if (!isProblemSubmitted(problemId)) {
                logger.error("Problem (" + problemId + ") was not submitted.");
            }
            solverTask = problemIdToSolverTaskMap.get(problemId);
        }
        return solverTask == null ? null : solverTask.getSolverStatus();
    }

    @Override
    public boolean addEventListener(Object problemId, SolverEventListener<Solution_> eventListener) {
        logger.debug("Adding an event listener for problemId ({}).", problemId);
        SolverTask<Solution_> solverTask;
        synchronized (this) {
            solverTask = problemIdToSolverTaskMap.get(problemId);
            if (solverTask == null) {
                logger.error("Problem (" + problemId + ") was not submitted.");
                return false;
            }
        }
        solverTask.addEventListener(eventListener);
        return true;
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down {}.", DefaultSolverManager.class.getName());
        // Shutting down executor services before stopping solvers so that queued up solver tasks don't start solving.
        // TODO consider using org.optaplanner.core.impl.solver.thread.ThreadUtils
        solverExecutorService.shutdownNow();
        eventHandlerExecutorService.shutdownNow();
        stopSolvers(); // TODO is this necessary?
    }

    private void stopSolvers() {
        for (SolverTask solverTask : problemIdToSolverTaskMap.values()) {
            solverTask.stopSolver();
        }
    }
}
