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

package org.optaplanner.springboottaskassigning.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.entity.PlanningPin;
import org.optaplanner.core.api.domain.variable.AnchorShadowVariable;
import org.optaplanner.core.api.domain.variable.CustomShadowVariable;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.api.domain.variable.PlanningVariableGraphType;
import org.optaplanner.core.api.domain.variable.PlanningVariableReference;
import org.optaplanner.springboottaskassigning.domain.solver.StartAndEndTimeUpdatingVariableListener;
import org.optaplanner.springboottaskassigning.domain.solver.TaskDifficultyComparator;

@PlanningEntity(difficultyComparatorClass = TaskDifficultyComparator.class)
public class Task extends TaskOrEmployee {

    private TaskType taskType;
    private int indexInTaskType;
    private Customer customer;
    private int readyTime;
    private Priority priority;
    @PlanningPin
    private boolean pinned;

    // Planning variables: changes during planning, between score calculations.
    @PlanningVariable(valueRangeProviderRefs = {"employeeRange", "taskRange"},
            graphType = PlanningVariableGraphType.CHAINED)
    // Ignore this in the json result to keep Tasks details in taskList only
    @JsonIgnore
    private TaskOrEmployee previousTaskOrEmployee;

    // Shadow variables
    // Task nextTask inherited from superclass
    @AnchorShadowVariable(sourceVariableName = "previousTaskOrEmployee")
    private Employee employee;
    @CustomShadowVariable(variableListenerClass = StartAndEndTimeUpdatingVariableListener.class,
            // Arguable, to adhere to API specs (although this works), nextTask and employee should also be a source,
            // because this shadow must be triggered after nextTask and employee (but there is no need to be triggered by those)
            sources = {@PlanningVariableReference(variableName = "previousTaskOrEmployee")})
    private Integer startTime; // In minutes
    @CustomShadowVariable(variableListenerRef = @PlanningVariableReference(variableName = "startTime"))
    private Integer endTime; // In minutes

    public Task() {
    }

    public Task(long id, long tenantId, TaskType taskType, int indexInTaskType, Customer customer, int readyTime, Priority priority) {
        super(id, tenantId);
        this.taskType = taskType;
        this.indexInTaskType = indexInTaskType;
        this.customer = customer;
        this.readyTime = readyTime;
        this.priority = priority;
        pinned = false;
    }

    public TaskType getTaskType() {
        return taskType;
    }

    public void setTaskType(TaskType taskType) {
        this.taskType = taskType;
    }

    public int getIndexInTaskType() {
        return indexInTaskType;
    }

    public void setIndexInTaskType(int indexInTaskType) {
        this.indexInTaskType = indexInTaskType;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public int getReadyTime() {
        return readyTime;
    }

    public void setReadyTime(int readyTime) {
        this.readyTime = readyTime;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public TaskOrEmployee getPreviousTaskOrEmployee() {
        return previousTaskOrEmployee;
    }

    public void setPreviousTaskOrEmployee(TaskOrEmployee previousTaskOrEmployee) {
        this.previousTaskOrEmployee = previousTaskOrEmployee;
    }

    @Override
    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Integer getStartTime() {
        return startTime;
    }

    public void setStartTime(Integer startTime) {
        this.startTime = startTime;
    }

    @Override
    public Integer getEndTime() {
        return endTime == null ? 0 : endTime;
    }

    public void setEndTime(Integer endTime) {
        this.endTime = endTime;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    public int getMissingSkillCount() {
        if (employee == null) {
            return 0;
        }
        int count = 0;
        for (Skill skill : taskType.getRequiredSkillList()) {
            if (!employee.getSkillSet().contains(skill)) {
                count++;
            }
        }
        return count;
    }

    /**
     * In minutes
     * @return at least 1 minute
     */
    public int getDuration() {
        Affinity affinity = getAffinity();
        return taskType.getBaseDuration() * affinity.getDurationMultiplier();
    }

    public Affinity getAffinity() {
        return (employee == null) ? Affinity.NONE : employee.getAffinity(customer);
    }

    public String getCode() {
        return taskType + "-" + indexInTaskType;
    }

    public String getTitle() {
        return taskType.getTitle();
    }

    public String getLabel() {
        return getCode() + ": " + taskType.getTitle();
    }

    @JsonIgnore
    public String getToolText() {
        StringBuilder toolText = new StringBuilder();
        toolText.append("<html><center><b>").append(getLabel()).append("</b><br/>")
                .append(priority.getLabel()).append("<br/><br/>");
        toolText.append("Required skills:<br/>");
        for (Skill skill : taskType.getRequiredSkillList()) {
            toolText.append(skill.getLabel()).append("<br/>");
        }
        toolText.append("<br/>Customer:<br/>").append(customer.getName()).append("<br/>(")
                .append(getAffinity().getLabel()).append(")<br/>");
        toolText.append("</center></html>");
        return toolText.toString();
    }

    @Override
    public String toString() {
        return getCode();
    }
}
