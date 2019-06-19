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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKeyColumn;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

@Entity
public class Employee extends TaskOrEmployee {

    private String fullName;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            joinColumns = @JoinColumn(name = "employee_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id")
    )
    private Set<Skill> skillSet;

    @ElementCollection
    @MapKeyColumn(name = "customer_id")
    @Column(name = "affinity")
    @LazyCollection(LazyCollectionOption.FALSE)
    private Map<Long, Affinity> customerIdToAffinityMap;

    public Employee() {
    }

    public Employee(long id, long tenantId, String fullName) {
        super(id, tenantId);
        this.fullName = fullName;
        skillSet = new LinkedHashSet<>();
        customerIdToAffinityMap = new LinkedHashMap<>();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Set<Skill> getSkillSet() {
        return skillSet;
    }

    public void setSkillSet(Set<Skill> skillSet) {
        this.skillSet = skillSet;
    }

    public Map<Long, Affinity> getCustomerIdToAffinityMap() {
        return customerIdToAffinityMap;
    }

    public void setCustomerIdToAffinityMap(Map<Long, Affinity> customerIdToAffinityMap) {
        this.customerIdToAffinityMap = customerIdToAffinityMap;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    @Override
    @JsonIgnore
    public Employee getEmployee() {
        return this;
    }

    @Override
    public Integer getEndTime() {
        return 0;
    }

    /**
     * @param customer never null
     * @return never null
     */
    public Affinity getAffinity(Customer customer) {
        Affinity affinity = customerIdToAffinityMap.get(customer.getId());
        if (affinity == null) {
            affinity = Affinity.NONE;
        }
        return affinity;
    }

    public String getLabel() {
        return fullName;
    }

    @JsonIgnore
    public String getToolText() {
        StringBuilder toolText = new StringBuilder();
        toolText.append("<html><center><b>").append(fullName).append("</b><br/><br/>");
        toolText.append("Skills:<br/>");
        for (Skill skill : skillSet) {
            toolText.append(skill.getLabel()).append("<br/>");
        }
        toolText.append("</center></html>");
        return toolText.toString();
    }

    @Override
    public String toString() {
        return fullName;
    }
}
