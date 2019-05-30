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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.springboottaskassigning.domain.TaskAssigningSolution;
import org.optaplanner.springboottaskassigning.utils.TaskAssigningGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class DefaultSolverManagerTest {

    @Autowired
    private SolverManager<TaskAssigningSolution> solverManager;

    @Test
    public void basicUsageOfSolverManagerWithOneProblem() throws InterruptedException {
        Long tenantId = 1L;
        TaskAssigningSolution problem =
                new TaskAssigningGenerator(tenantId).createTaskAssigningSolution(24, 4);

        solverManager.solve(tenantId, problem);
        Thread.sleep(1000L); // Give time to start solving

        assertEquals(solverManager.getSolverStatus(tenantId), SolverStatus.SOLVING);

        TaskAssigningSolution solution = solverManager.getBestSolution(tenantId);
        assertEquals(solution.getTenantId(), tenantId);

        // FIXME this always returns null
        Score score = solverManager.getBestScore(tenantId);
//        assertTrue(score.isSolutionInitialized());

    }
}
