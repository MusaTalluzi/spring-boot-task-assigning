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

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.springboottaskassigning.domain.TaskAssigningSolution;
import org.optaplanner.springboottaskassigning.solver.DefaultSolverManager;
import org.optaplanner.springboottaskassigning.solver.SolverStatus;
import org.optaplanner.springboottaskassigning.utils.TaskAssigningGenerator;
import org.optaplanner.test.impl.score.buildin.bendable.BendableScoreVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class TaskAssigningSolverManagerControllerTest {

    private static final Logger logger = LoggerFactory.getLogger(TaskAssigningSolverManagerControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private BendableScoreVerifier<TaskAssigningSolution> scoreVerifier = new BendableScoreVerifier<>(
            SolverFactory.createFromXmlResource(DefaultSolverManager.SOLVER_CONFIG)
    );

    private static AtomicLong newTenantId;

    @BeforeClass
    public static void setup() {
        newTenantId = new AtomicLong(0);
    }

    @Test
    public void simpleProblemWithOneSolver() throws InterruptedException {
        submitProblemsAndSolveThem(1, 1, 1);
    }

    @Test
    public void simpleProblemsLessThanAvailableProcessors() {
        int numOfProblems = Runtime.getRuntime().availableProcessors() - 1;
        submitProblemsAndSolveThem(numOfProblems, 1, 1);
    }

    @Test
    public void simpleProblemsEqualToAvailableProcessors() {
        int numOfProblems = Runtime.getRuntime().availableProcessors();
        submitProblemsAndSolveThem(numOfProblems, 1, 1);
    }

    @Test
    public void complexProblemWithOneSolver() {
        submitProblemsAndSolveThem(1, 10, 10);
    }

    @Test
    public void complexProblemsLessThanAvailableProcessors() {
        int numOfProblems = Runtime.getRuntime().availableProcessors() - 1;
        submitProblemsAndSolveThem(numOfProblems, 10, 4);
    }

    @Test
    public void complexProblemsEqualToAvailableProcessors() {
        int numOfProblems = Runtime.getRuntime().availableProcessors();
        submitProblemsAndSolveThem(numOfProblems, 10, 4);
    }

    @Test
    public void simpleProblemsMoreThanAvailableProcessors() {
        int numOfProblems = Runtime.getRuntime().availableProcessors() * 2;
        submitProblemsAndSolveThem(numOfProblems, 1, 1);
    }

    @Test
    public void complexProblemsMoreThanAvailableProcessors() {
        int numOfProblems = Runtime.getRuntime().availableProcessors() * 2;
        submitProblemsAndSolveThem(numOfProblems, 10, 4);
    }

    private void submitProblemsAndSolveThem(int problemSize, int taskListSizeBound, int employeeListSizeBound) {
        logger.info("Sumbitting {} problems with taskListSizeBound ({}) and employeeListSizeBound ({}).",
                problemSize, taskListSizeBound, employeeListSizeBound);
        IntStream.range(0, problemSize).parallel().forEach(i -> {
            try {
                logger.info("Submitting problem " + i);
                // FIXME ThreadLocalRandom does not support setting a Random seed
                submitOneProblemAndSolveIt(ThreadLocalRandom.current().nextInt(taskListSizeBound) + 1,
                        ThreadLocalRandom.current().nextInt(employeeListSizeBound) + 1);
            } catch (Exception e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new IllegalStateException("Cannot test solving a problem after " + i + " iterations.", e);
            }
        });
    }

    private void submitOneProblemAndSolveIt(int taskListSize, int employeeListSize) throws Exception {
        TaskAssigningSolution planningProblem =
                new TaskAssigningGenerator(newTenantId.getAndIncrement()).createTaskAssigningSolution(taskListSize, employeeListSize);
        String planningProblemAsJsonString = objectMapper.writeValueAsString(planningProblem);
        Long tenantId = planningProblem.getTenantId();

        mockMvc.perform(post("/tenants/{tenantId}/solver", tenantId)
                .content(planningProblemAsJsonString)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        SolverStatus solverStatus = SolverStatus.STOPPED;
        while (!solverStatus.equals(SolverStatus.SOLVING)) {
            String solverStatusAsJsonString = mockMvc.perform(get("/tenants/{tenantId}/solver/status", tenantId)
                    .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            solverStatus = objectMapper.readValue(solverStatusAsJsonString, SolverStatus.class);
            Thread.sleep(1000);
        }

        String solutionAsJsonString = mockMvc.perform(get("/tenants/{tenantId}/solver/bestSolution", tenantId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        TaskAssigningSolution solution = objectMapper.readValue(solutionAsJsonString, TaskAssigningSolution.class);
        scoreVerifier.assertHardWeight("Skill requirements",
                0, solution.getScore().getHardScore(0), solution);
        scoreVerifier.assertSoftWeight("Critical priority",
                0, solution.getScore().getSoftScore(0), solution);
        scoreVerifier.assertSoftWeight("Minimze makespan (starting with the latest ending employee first)",
                1, solution.getScore().getSoftScore(1), solution);
        scoreVerifier.assertSoftWeight("Major priority",
                2, solution.getScore().getSoftScore(2), solution);
        scoreVerifier.assertSoftWeight("Minor priority",
                3, solution.getScore().getSoftScore(3), solution);

        mockMvc.perform(get("/tenants/{tenantId}/solver/bestScore", tenantId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}
