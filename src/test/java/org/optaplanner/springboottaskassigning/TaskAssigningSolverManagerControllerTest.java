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

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.optaplanner.core.api.score.buildin.bendable.BendableScore;
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
    // FIXME set a seed for random in a thread safe way
    private static Random random;

    @BeforeClass
    public static void setup() {
        newTenantId = new AtomicLong(0);
        random = new Random(47);
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

        /* Only for testing persistence */

//        URL url = new URL("http://localhost:8080/solvers/" + tenantId);
//        URLConnection con = url.openConnection();
//        HttpURLConnection httpURLConnection = (HttpURLConnection) con;
//        httpURLConnection.setRequestMethod("POST");
//        httpURLConnection.setDoOutput(true);
//
//        byte[] out = planningProblemAsJsonString.getBytes(StandardCharsets.UTF_8);
//
//        httpURLConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
//        httpURLConnection.setFixedLengthStreamingMode(out.length);
//        httpURLConnection.connect();
//        try (OutputStream os = httpURLConnection.getOutputStream()) {
//            os.write(out);
//        }
//        httpURLConnection.disconnect();

        /* End of testing persistence */

        mockMvc.perform(post("/solvers/{tenantId}", tenantId)
                .content(planningProblemAsJsonString)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Thread.sleep(1000L); // Give solver thread time to start

        // FIXME: when number of solvers is more than available processors, 1s isn't enough for all solvers to start
        String solvingStatusJsonString = objectMapper.writeValueAsString(SolverStatus.SOLVING);
        mockMvc.perform(get("/solvers/{tenantId}/solverStatus", tenantId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
//                .andExpect(content().string(solvingStatusJsonString));

        Thread.sleep(2000L); // Give solver time to solve

        String solutionAsJsonString = mockMvc.perform(get("/solvers/{tenantId}/bestSolution", tenantId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
//        CloudBalance solution = objectMapper.readValue(solutionAsJsonString, TaskAssigningSolution.class);

        // FIXME the score might change between the two REST request invocations
        // FIXME Fix: after adding persistence, compare score of solution with score stored
        String bestScoreAsString = mockMvc.perform(get("/solvers/{tenantId}/bestScore", tenantId)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        BendableScore score = objectMapper.readValue(bestScoreAsString, BendableScore.class);
    }
}
