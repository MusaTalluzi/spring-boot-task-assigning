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

package org.optaplanner.springboottaskassigning.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.optaplanner.springboottaskassigning.domain.Affinity;
import org.optaplanner.springboottaskassigning.domain.Customer;
import org.optaplanner.springboottaskassigning.domain.Employee;
import org.optaplanner.springboottaskassigning.domain.Priority;
import org.optaplanner.springboottaskassigning.domain.Skill;
import org.optaplanner.springboottaskassigning.domain.Task;
import org.optaplanner.springboottaskassigning.domain.TaskAssigningSolution;
import org.optaplanner.springboottaskassigning.domain.TaskType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskAssigningGenerator {

    private static final Logger logger = LoggerFactory.getLogger(TaskAssigningGenerator.class);

    private static final int BASE_DURATION_MINIMUM = 1;
    private static final int BASE_DURATION_MAXIMUM = 3;
    private static final int BASE_DURATION_MULTIPLIER = 30;
    private static final int SKILL_SET_SIZE_MINIMUM = 2;
    private static final int SKILL_SET_SIZE_MAXIMUM = 4;

    private final StringDataGenerator skillNameGenerator = new StringDataGenerator()
            .addPart(true, 0,
                    "Problem",
                    "Team",
                    "Business",
                    "Risk",
                    "Creative",
                    "Strategic",
                    "Customer",
                    "Conflict",
                    "IT",
                    "Academic")
            .addPart(true, 1,
                    "Solving",
                    "Building",
                    "Storytelling",
                    "Management",
                    "Thinking",
                    "Planning",
                    "Service",
                    "Resolution",
                    "Engineering",
                    "Research");
    private final StringDataGenerator taskTypeNameGenerator = new StringDataGenerator()
            .addPart(true, 0,
                    "Improve",
                    "Expand",
                    "Shrink",
                    "Approve",
                    "Localize",
                    "Review",
                    "Clean",
                    "Merge",
                    "Double",
                    "Optimize")
            .addPart(true, 1,
                    "Sales",
                    "Tax",
                    "VAT",
                    "Legal",
                    "Cloud",
                    "Marketing",
                    "IT",
                    "Contract",
                    "Financial",
                    "Advertisement")
            .addPart(false, 2,
                    "Software",
                    "Development",
                    "Accounting",
                    "Management",
                    "Facilities",
                    "Writing",
                    "Productization",
                    "Lobbying",
                    "Engineering",
                    "Research");
    private final StringDataGenerator customerNameGenerator = StringDataGenerator.buildCompanyNames();
    private final StringDataGenerator employeeNameGenerator = StringDataGenerator.buildFullNames();

    private final long tenantId;
    private Long newEntityId = 0L;
    private Random random;

    public TaskAssigningGenerator(long tenantId) {
        this.tenantId = tenantId;
    }

    public TaskAssigningSolution createTaskAssigningSolution(int taskListSize, int employeeListSize) {
        int skillListSize = SKILL_SET_SIZE_MAXIMUM + (int) Math.log(employeeListSize);
        int taskTypeListSize = taskListSize / 5 + 1;
        int customerListSize = Math.min(taskTypeListSize, employeeListSize * 3);
        return createTaskAssigningSolution(taskListSize, skillListSize, employeeListSize,
                taskTypeListSize, customerListSize);
    }

    private TaskAssigningSolution createTaskAssigningSolution(int taskListSize, int skillListSize, int employeeListSize,
                                                              int taskTypeListSize, int customerListSize) {
        random = new Random(37);
        TaskAssigningSolution solution = new TaskAssigningSolution();
        solution.setId(newEntityId++);
        solution.setTenantId(tenantId);

        createSkillList(solution, skillListSize);
        createCustomerList(solution, customerListSize);
        createEmployeeList(solution, employeeListSize);
        createTaskTypeList(solution, taskTypeListSize);
        createTaskList(solution, taskListSize);
        solution.setFrozenCutoff(0);

        logger.info("TaskAssigningSolution {} has {} tasks, {} skills, {} employees, {} task types and {} customers.",
                solution.getId(),
                taskListSize,
                skillListSize,
                employeeListSize,
                taskTypeListSize,
                customerListSize);
        return solution;
    }

    private void createSkillList(TaskAssigningSolution solution, int skillListSize) {
        List<Skill> skillList = new ArrayList<>(skillListSize);
        skillNameGenerator.predictMaximumSizeAndReset(skillListSize);
        for (int i = 0; i < skillListSize; i++) {
            Skill skill = new Skill();
            skill.setId(newEntityId++);
            skill.setTenantId(tenantId);
            String skillName = skillNameGenerator.generateNextValue();
            skill.setName(skillName);
            logger.trace("Created skill with skillName ({}).", skillName);
            skillList.add(skill);
        }
        solution.setSkillList(skillList);
    }

    private void createCustomerList(TaskAssigningSolution solution, int customerListSize) {
        List<Customer> customerList = new ArrayList<>(customerListSize);
        customerNameGenerator.predictMaximumSizeAndReset(customerListSize);
        for (int i = 0; i < customerListSize; i++) {
            Customer customer = new Customer();
            customer.setId(newEntityId++);
            customer.setTenantId(tenantId);
            String customerName = customerNameGenerator.generateNextValue();
            customer.setName(customerName);
            logger.trace("Created skill with customerName ({}).", customerName);
            customerList.add(customer);
        }
        solution.setCustomerList(customerList);
    }

    private void createEmployeeList(TaskAssigningSolution solution, int employeeListSize) {
        List<Skill> skillList = solution.getSkillList();
        List<Customer> customerList = solution.getCustomerList();
        Affinity[] affinities = Affinity.values();
        List<Employee> employeeList = new ArrayList<>(employeeListSize);
        int skillListIndex = 0;
        employeeNameGenerator.predictMaximumSizeAndReset(employeeListSize);
        for (int i = 0; i < employeeListSize; i++) {
            Employee employee = new Employee();
            employee.setId(newEntityId++);
            employee.setTenantId(tenantId);
            String fullName = employeeNameGenerator.generateNextValue();
            employee.setFullName(fullName);
            int skillSetSize = SKILL_SET_SIZE_MINIMUM + random.nextInt(SKILL_SET_SIZE_MAXIMUM - SKILL_SET_SIZE_MINIMUM);
            if (skillSetSize > skillList.size()) {
                skillSetSize = skillList.size();
            }
            Set<Skill> skillSet = new LinkedHashSet<>(skillSetSize);
            for (int j = 0; j < skillSetSize; j++) {
                skillSet.add(skillList.get(skillListIndex));
                skillListIndex = (skillListIndex + 1) % skillList.size();
            }
            employee.setSkillSet(skillSet);
            Map<Long, Affinity> affinityMap = new LinkedHashMap<>(customerList.size());
            for (Customer customer : customerList) {
                affinityMap.put(customer.getId(), affinities[random.nextInt(affinities.length)]);
            }
            employee.setCustomerIdToAffinityMap(affinityMap);
            logger.trace("Created employee with fullName ({}).", fullName);
            employeeList.add(employee);
        }
        solution.setEmployeeList(employeeList);
    }

    private void createTaskTypeList(TaskAssigningSolution solution, int taskTypeListSize) {
        List<Employee> employeeList = solution.getEmployeeList();
        List<TaskType> taskTypeList = new ArrayList<>(taskTypeListSize);
        Set<String> codeSet = new LinkedHashSet<>(taskTypeListSize);
        taskTypeNameGenerator.predictMaximumSizeAndReset(taskTypeListSize);
        for (int i = 0; i < taskTypeListSize; i++) {
            TaskType taskType = new TaskType();
            taskType.setId(newEntityId++);
            taskType.setTenantId(tenantId);
            String title = taskTypeNameGenerator.generateNextValue();
            taskType.setTitle(title);
            String code;
            switch (title.replaceAll("[^ ]", "").length() + 1) {
                case 3:
                    code = title.replaceAll("(\\w)\\w* (\\w)\\w* (\\w)\\w*", "$1$2$3");
                    break;
                case 2:
                    code = title.replaceAll("(\\w)\\w* (\\w)\\w*", "$1$2");
                    break;
                case 1:
                    code = title.replaceAll("(\\w)\\w*", "$1");
                    break;
                default:
                    throw new IllegalStateException("Cannot convert title (" + title + ") into a code.");
            }
            if (codeSet.contains(code)) {
                int codeSuffixNumber = 1;
                while (codeSet.contains(code + codeSuffixNumber)) {
                    codeSuffixNumber++;
                }
                code = code + codeSuffixNumber;
            }
            codeSet.add(code);
            taskType.setCode(code);
            taskType.setBaseDuration(
                    (BASE_DURATION_MINIMUM + random.nextInt(BASE_DURATION_MAXIMUM - BASE_DURATION_MINIMUM))
                            * BASE_DURATION_MULTIPLIER);
            Employee randomEmployee = employeeList.get(random.nextInt(employeeList.size()));
            ArrayList<Skill> randomSkillList = new ArrayList<>(randomEmployee.getSkillSet());
            Collections.shuffle(randomSkillList, random);
            int requiredSkillListSize = 1 + random.nextInt(randomSkillList.size() - 1);
            taskType.setRequiredSkillSet(new HashSet<>(randomSkillList.subList(0, requiredSkillListSize)));
            logger.trace("Created taskType with title ({}).", title);
            taskTypeList.add(taskType);
        }
        solution.setTaskTypeList(taskTypeList);
    }

    private void createTaskList(TaskAssigningSolution solution, int taskListSize) {
        List<TaskType> taskTypeList = solution.getTaskTypeList();
        List<Customer> customerList = solution.getCustomerList();
        Priority[] priorities = Priority.values();
        List<Task> taskList = new ArrayList<>(taskListSize);
        Map<TaskType, Integer> maxIndexInTaskTypeMap = new LinkedHashMap<>(taskTypeList.size());
        for (int i = 0; i < taskListSize; i++) {
            Task task = new Task();
            task.setId(newEntityId++);
            task.setTenantId(tenantId);
            TaskType taskType = taskTypeList.get(random.nextInt(taskTypeList.size()));
            task.setTaskType(taskType);
            Integer indexInTaskType = maxIndexInTaskTypeMap.get(taskType);
            if (indexInTaskType == null) {
                indexInTaskType = 1;
            } else {
                indexInTaskType++;
            }
            task.setIndexInTaskType(indexInTaskType);
            maxIndexInTaskTypeMap.put(taskType, indexInTaskType);
            task.setCustomer(customerList.get(random.nextInt(customerList.size())));
            task.setReadyTime(0);
            task.setPriority(priorities[random.nextInt(priorities.length)]);
            taskList.add(task);
        }
        solution.setTaskList(taskList);
    }
}

