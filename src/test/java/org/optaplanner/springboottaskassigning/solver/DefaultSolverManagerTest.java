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

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.optaplanner.springboottaskassigning.domain.TaskAssigningSolution;
import org.optaplanner.springboottaskassigning.utils.TaskAssigningGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DefaultSolverManagerTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private SolverManager<TaskAssigningSolution> solverManager;
    private Long tenantId;
    private CountDownLatch solutionChangedLatch;
    private CountDownLatch solvingEndedLatch;

    @Before
    public void setup() {
        solverManager = new DefaultSolverManager<>();
        tenantId = 0L;
        solutionChangedLatch = new CountDownLatch(1);
        solvingEndedLatch = new CountDownLatch(1);
    }

    @After
    public void tearDown() {
        solverManager.shutdown();
    }

    @Test
    public void basicUsageOfSolverManagerWithOneProblem() throws InterruptedException {
        TaskAssigningSolution problem =
                new TaskAssigningGenerator(tenantId).createTaskAssigningSolution(24, 4);
        solverManager.solve(tenantId, problem,
                taskAssigningSolution -> solutionChangedLatch.countDown(),
                taskAssigningSolution -> solvingEndedLatch.countDown());
        solutionChangedLatch.await(60, TimeUnit.SECONDS);

        assertEquals(solverManager.getSolverStatus(tenantId), SolverStatus.SOLVING);
        assertEquals(solverManager.getBestSolution(tenantId).getTenantId(), tenantId);
        assertTrue(solverManager.getBestScore(tenantId).isSolutionInitialized());

        solverManager.stopSolver(tenantId);
        assertEquals(SolverStatus.TERMINATED_EARLY, solverManager.getSolverStatus(tenantId));
        logger.info(String.valueOf(solvingEndedLatch.getCount()));
    }

    @Test
    public void onBestSolutionChangeAndOnSolutionEnded() throws InterruptedException {
        TaskAssigningSolution problem =
                new TaskAssigningGenerator(tenantId).createTaskAssigningSolution(1, 1);
        solverManager.solve(tenantId, problem,
                taskAssigningSolution -> solutionChangedLatch.countDown(),
                taskAssigningSolution -> solvingEndedLatch.countDown());
        solutionChangedLatch.await(60, TimeUnit.SECONDS);
        assertEquals(solverManager.getSolverStatus(tenantId), SolverStatus.SOLVING);
        solvingEndedLatch.await(60, TimeUnit.SECONDS);
        assertEquals(solverManager.getSolverStatus(tenantId), SolverStatus.STOPPED);
    }

    @Test
    public void tryToGetNonExistingSolution() throws InterruptedException {
        TaskAssigningSolution problem =
                new TaskAssigningGenerator(tenantId).createTaskAssigningSolution(1, 1);
        solverManager.solve(tenantId, problem,
                taskAssigningSolution -> solutionChangedLatch.countDown(),
                taskAssigningSolution -> solvingEndedLatch.countDown());
        solutionChangedLatch.await(60, TimeUnit.SECONDS);
        assertNull(solverManager.getBestSolution(tenantId + 1));
    }

    @Test
    public void onBestSolutionChangedCalledEveryTimeASolutionIsChanged() throws InterruptedException {
        AtomicInteger bestSolutionChangedEventCount = new AtomicInteger(0);
        AtomicInteger onBestSolutionChangedEventInvocationCount = new AtomicInteger(0);
        TaskAssigningSolution problem =
                new TaskAssigningGenerator(tenantId).createTaskAssigningSolution(24, 8);
        solverManager.solve(tenantId, problem,
                taskAssigningSolution -> bestSolutionChangedEventCount.incrementAndGet(),
                taskAssigningSolution -> solvingEndedLatch.countDown());

        solverManager.addEventListener(tenantId, bestSolutionChangedEvent -> onBestSolutionChangedEventInvocationCount.incrementAndGet());
        solvingEndedLatch.await(60, TimeUnit.SECONDS);
        assertEquals(onBestSolutionChangedEventInvocationCount.get(), bestSolutionChangedEventCount.get());
        logger.info("Number of bestSolutionChangedEvents: {}.", bestSolutionChangedEventCount.get());
    }

    @Test
    public void shutdownShouldStopAllSolvers() {
        int[] problemIds = new int[Runtime.getRuntime().availableProcessors() * 3];
        for (int i = 0; i < problemIds.length; i++) {
            problemIds[i] = i;
        }
        Arrays.stream(problemIds)
                .forEach(problemId -> {
                    TaskAssigningSolution problem =
                            new TaskAssigningGenerator(tenantId).createTaskAssigningSolution(1, 1);
                    solverManager.solve(problemId, problem, null, null);
                });
        solverManager.shutdown(); // Calling it right away while some tasks might be on the queue
        Arrays.stream(problemIds)
                .forEach(problemId -> assertEquals(SolverStatus.TERMINATED_EARLY, solverManager.getSolverStatus(problemId)));
    }

    // ****************************
    // Exception handling tests
    // ****************************

    @Test
    public void shouldNotStartTwoSolverTasksWithSameProblemId() {
        TaskAssigningSolution problem =
                new TaskAssigningGenerator(tenantId).createTaskAssigningSolution(1, 1);
        solverManager.solve(tenantId, problem, null, null);
        try {
            solverManager.solve(tenantId, problem, null, null);
        } catch (IllegalArgumentException e) {
            assertEquals("Problem (" + tenantId + ") already exists.", e.getMessage());
        }
    }

    @Test
    public void shouldPropagateExceptionsFromSolverThread() throws Throwable {
        TaskAssigningSolution problem =
                new TaskAssigningGenerator(tenantId).createTaskAssigningSolution(1, 1);
        problem.setTaskList(null); // So that solver thread throws IllegalArgumentException
        final AtomicReference<Throwable> solverException = new AtomicReference<>();
        solverManager.solve(tenantId, problem, null,
                solution -> solvingEndedLatch.countDown(),
                solverException::set);
        solvingEndedLatch.await(10, TimeUnit.SECONDS);
        assertEquals(IllegalArgumentException.class, solverException.get().getClass());
    }

    @Test
    public void shouldNotStopASolverThatHasNotBeenSubmitted() {
        try {
            solverManager.stopSolver(tenantId);
        } catch (IllegalArgumentException e) {
            assertEquals("Problem (" + tenantId + ") was not submitted.", e.getMessage());
        }
    }

    @Test
    public void shouldNotGetMetadataOfSolverThatHasNotBeenSubmitted() {
        assertNull(solverManager.getBestSolution(tenantId));
        assertNull(solverManager.getBestScore(tenantId));
        assertNull(solverManager.getSolverStatus(tenantId));
        assertFalse(solverManager.addEventListener(tenantId, bestSolutionChangedEvent -> {
        }));
    }
}
