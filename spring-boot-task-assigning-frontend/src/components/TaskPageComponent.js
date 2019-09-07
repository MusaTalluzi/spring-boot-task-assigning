import React, { Component } from 'react';
import {
  DataList, DataListItem, DataListCell, Button, Modal,
  Form, FormGroup, TextInput, ActionGroup, Toolbar, ToolbarGroup,
  Checkbox, FormSelect, FormSelectOption,
} from '@patternfly/react-core';
import PropTypes from 'prop-types';

import { submitProblemFactChange } from '../shared/kie-server-client';

class TaskPage extends Component {
  constructor(props) {
    super(props);
    this.state = {
      isAddTaskModalOpen: false,
      newTask: {
        readyTime: 0,
        priority: 'MINOR',
        pinned: false,
        taskTypeId: 0,
        customerId: 0,
      },
    };

    this.handleAddTaskModalToggle = this.handleAddTaskModalToggle.bind(this);
    this.handleAddTask = this.handleAddTask.bind(this);
    this.removeTask = this.removeTask.bind(this);
  }

  handleAddTaskModalToggle() {
    this.setState(({ isAddTaskModalOpen }) => ({
      isAddTaskModalOpen: !isAddTaskModalOpen,
    }));
  }

  handleAddTask(event) {
    event.preventDefault();
    this.handleAddTaskModalToggle();

    const body = {
      'problem-fact-change': {
        $class: 'TaAddTaskProblemFactChange',
        task: {
          readyTime: this.state.newTask.readyTime,
          priority: this.state.newTask.priority,
          pinned: this.state.newTask.pinned,
        },
        taskTypeId: this.state.newTask.taskTypeId,
        customerId: this.state.newTask.customerId,
      },
    };

    submitProblemFactChange(body, `Task ${JSON.stringify(this.state.newTask)} added successfully`,
      this.props.container.containerId, this.props.solver.id);
  }

  removeTask(taskId) {
    const body = {
      'problem-fact-change': {
        $class: 'TaDeleteTaskProblemFactChange',
        taskId,
      },
    };

    submitProblemFactChange(body, `Task with id ${taskId} was removed successfully`,
      this.props.container.containerId, this.props.solver.id)
      .then(() => this.props.updateBestSolution());
  }

  render() {
    const taskList = this.props.tasks.map((task) => {
      const { id } = task;
      const taskType = this.props.taskTypes.filter(type => type.id === task.taskType)[0].label;
      const customer = this.props.customers.filter(c => c.id === task.customer)[0].label;
      return (
        <DataListItem key={task.id} aria-labelledby={`Task ${task.id}`}>
          <DataListCell>{id}</DataListCell>
          <DataListCell>{taskType}</DataListCell>
          <DataListCell>{customer}</DataListCell>
          <DataListCell>
            <Button variant="danger" onClick={() => this.removeTask(id)}>Remove</Button>
          </DataListCell>
        </DataListItem>
      );
    });

    const taskTypeOptions = this.props.taskTypes.map(taskType => (
      <FormSelectOption
        key={taskType.id.toString()}
        value={taskType.id}
        label={taskType.title}
      />
    ));
    const customerOptions = this.props.customers.map(customer => (
      <FormSelectOption
        key={customer.id.toString()}
        value={customer.id}
        label={customer.name}
      />
    ));

    return (
      <div className="container">
        <DataList aria-label="Checkbox and action data list example">
          <DataListItem id="header" aria-labelledby="Task page header">
            <DataListCell>Task Id</DataListCell>
            <DataListCell>Task type</DataListCell>
            <DataListCell>Customer </DataListCell>
            <DataListCell>
              <Button onClick={this.handleAddTaskModalToggle} variant="primary">Add task</Button>
            </DataListCell>
          </DataListItem>
          {taskList}
        </DataList>

        <Modal
          title="Add Task"
          isOpen={this.state.isAddTaskModalOpen}
          onClose={this.handleAddTaskModalToggle}
        >
          <Form>
            <FormGroup
              label="Ready Time"
              isRequired
              fieldId="readyTime"
            >
              <TextInput
                isRequired
                type="number"
                id="readyTime"
                name="readyTime"
                value={this.state.newTask.readyTime}
                onChange={(readyTime) => {
                  this.setState(
                    prevState => ({ newTask: { ...prevState.newTask, readyTime } }),
                  );
                }}
              />
            </FormGroup>
            <FormGroup
              label="Task Type"
              isRequired
              fieldId="taskTypeId"
            >
              <FormSelect
                id="taskTypeId"
                name="taskTypeId"
                value={this.state.newTask.taskTypeId}
                onChange={(taskTypeId) => {
                  this.setState(
                    prevState => ({ newTask: { ...prevState.newTask, taskTypeId } }),
                  );
                }}
              >
                {taskTypeOptions}
              </FormSelect>
            </FormGroup>
            <FormGroup
              label="Customer"
              isRequired
              fieldId="customerId"
            >
              <FormSelect
                id="customerId"
                name="customerId"
                value={this.state.newTask.customerId}
                onChange={(customerId) => {
                  this.setState(
                    prevState => ({ newTask: { ...prevState.newTask, customerId } }),
                  );
                }}
              >
                {customerOptions}
              </FormSelect>
            </FormGroup>
            <FormGroup
              label="Priority"
              isRequired
              fieldId="priority"
            >
              <FormSelect
                id="priority"
                name="Priority"
                value={this.state.newTask.priority}
                onChange={(priority) => {
                  this.setState(
                    prevState => ({ newTask: { ...prevState.newTask, priority } }),
                  );
                }}
              >
                <FormSelectOption key="0" value="MINOR" label="Minor priority" />
                <FormSelectOption key="1" value="MAJOR" label="Major priority" />
                <FormSelectOption key="2" value="CRITICAL" label="Critical priority" />
              </FormSelect>
            </FormGroup>
            <FormGroup
              label="Pinned"
              isRequired
              fieldId="pinned"
            >
              <Checkbox
                label="Pinned"
                id="pinned"
                name="pinned"
                aria-label="pinned"
                onChange={(pinned) => {
                  this.setState(
                    prevState => ({ newTask: { ...prevState.newTask, pinned } }),
                  );
                }}
              />
            </FormGroup>
            <ActionGroup>
              <Toolbar>
                <ToolbarGroup>
                  <Button key="confirmAddTask" variant="primary" onClick={this.handleAddTask}>Add</Button>
                </ToolbarGroup>
                <ToolbarGroup>
                  <Button key="cancelAddTask" variant="secondary" onClick={this.handleAddTaskModalToggle}>Cancel</Button>
                </ToolbarGroup>
              </Toolbar>
            </ActionGroup>
          </Form>
        </Modal>
      </div>
    );
  }
}

TaskPage.propTypes = {
  tasks: PropTypes.instanceOf(Array).isRequired,
  taskTypes: PropTypes.instanceOf(Array).isRequired,
  customers: PropTypes.instanceOf(Array).isRequired,
  container: PropTypes.instanceOf(Object).isRequired,
  solver: PropTypes.instanceOf(Object).isRequired,
  updateBestSolution: PropTypes.func.isRequired,
};

export default TaskPage;
