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

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.TypeDef;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.drools.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.bendable.BendableScore;
import org.optaplanner.persistence.jpa.impl.score.buildin.bendable.BendableScoreHibernateType;

@PlanningSolution
@JsonPropertyOrder({"skillList", "taskTypeList", "customerList", "employeeList", "taskList", "score", "frozenCutoff"})
@Entity
@TypeDef(defaultForType = BendableScore.class, typeClass = BendableScoreHibernateType.class, parameters = {
        @Parameter(name = "hardLevelsSize", value = "1"),
        @Parameter(name = "softLevelsSize", value = "4")
})
public class TaskAssigningSolution extends AbstractPersistable {

    @ProblemFactCollectionProperty
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "cloud_balance_id")
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<Skill> skillList;

    @ProblemFactCollectionProperty
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "cloud_balance_id")
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<TaskType> taskTypeList;

    @ProblemFactCollectionProperty
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "cloud_balance_id")
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<Customer> customerList;

    @ValueRangeProvider(id = "employeeRange")
    @ProblemFactCollectionProperty
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "cloud_balance_id")
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<Employee> employeeList;

    @PlanningEntityCollectionProperty
    @ValueRangeProvider(id = "taskRange")
    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "cloud_balance_id")
    @LazyCollection(LazyCollectionOption.FALSE)
    private List<Task> taskList;

    @PlanningScore(bendableHardLevelsSize = 1, bendableSoftLevelsSize = 4)
    @Columns(columns = {
            @Column(name = "initScore"),
            @Column(name = "hardScore"),
            @Column(name = "soft0Score"),
            @Column(name = "soft1Score"),
            @Column(name = "soft2Score"),
            @Column(name = "soft3Score")
    })
    private BendableScore score;

    /**
     * Relates to {@link Task#getStartTime()}.
     */
    private int frozenCutoff; // In minutes

    public TaskAssigningSolution() {
    }

    public TaskAssigningSolution(long id, long tenantId, List<Skill> skillList, List<TaskType> taskTypeList,
                                 List<Customer> customerList, List<Employee> employeeList, List<Task> taskList) {
        super(id, tenantId);
        this.skillList = skillList;
        this.taskTypeList = taskTypeList;
        this.customerList = customerList;
        this.employeeList = employeeList;
        this.taskList = taskList;
    }

    public List<Skill> getSkillList() {
        return skillList;
    }

    public void setSkillList(List<Skill> skillList) {
        this.skillList = skillList;
    }

    public List<TaskType> getTaskTypeList() {
        return taskTypeList;
    }

    public void setTaskTypeList(List<TaskType> taskTypeList) {
        this.taskTypeList = taskTypeList;
    }

    public List<Customer> getCustomerList() {
        return customerList;
    }

    public void setCustomerList(List<Customer> customerList) {
        this.customerList = customerList;
    }

    public List<Employee> getEmployeeList() {
        return employeeList;
    }

    public void setEmployeeList(List<Employee> employeeList) {
        this.employeeList = employeeList;
    }

    public List<Task> getTaskList() {
        return taskList;
    }

    public void setTaskList(List<Task> taskList) {
        this.taskList = taskList;
    }

    public BendableScore getScore() {
        return score;
    }

    public void setScore(BendableScore score) {
        this.score = score;
    }

    public int getFrozenCutoff() {
        return frozenCutoff;
    }

    public void setFrozenCutoff(int frozenCutoff) {
        this.frozenCutoff = frozenCutoff;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************
}
