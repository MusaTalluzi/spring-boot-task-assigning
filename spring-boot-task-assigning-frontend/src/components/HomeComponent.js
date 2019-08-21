import React, { Component } from 'react';
import {
  Card, CardHeader, CardBody, Button, Modal,
  Form, FormGroup, ActionGroup, Toolbar, ToolbarGroup, TextArea, FormSelect, FormSelectOption,
} from '@patternfly/react-core';
import JXON from 'jxon';
import PropTypes from 'prop-types';

import Schedule from './ScheduleComponent';
import AutoProduceConsume from './AutoProduceConsumeComponent';

import PROBLEM from '../shared/24tasks';
// import PROBLEM from '../shared/simpleProblem';
import { problems } from '../shared/constants';

import { addProblem } from '../shared/springboot-server-client';

class Home extends Component {
  constructor(props) {
    super(props);
    this.state = {
      isAddProblemModalOpen: false,
      selectedProblemId: props.tenantId,
      toBeSubmittedProblemId: 0,
      allProblems: problems,
      submittedProblems: [],
    };

    this.handleAddProblemModalToggle = this.handleAddProblemModalToggle.bind(this);
    this.handleAddProblemModalConfirm = this.handleAddProblemModalConfirm.bind(this);
    this.handleGetSolution = this.handleGetSolution.bind(this);
    this.populateSelections = this.populateSelections.bind(this);
  }

  componentDidMount() {
    // TODO: query server for existing problems
    this.submitProblem(this.state.toBeSubmittedProblemId);
  }

  submitProblem = (problemId) => {
    const newProblem = this.state.allProblems[problemId];
    addProblem(newProblem.id, newProblem.taskLiseSize, newProblem.employeeListSize)
      .then((response) => {
        // TODO when adding querying server for existing problems, only execute the next lines if response is defined
        this.setState(prevState => ({
          submittedProblems: [...prevState.submittedProblems, newProblem],
        }));
        alert(`Problem ${newProblem.id} was submitted successfully.`);
        this.props.updateBestSolution();
      });
  }

  handleAddProblemModalToggle = () => {
    this.setState(({ isAddProblemModalOpen }) => ({
      isAddProblemModalOpen: !isAddProblemModalOpen,
    }));
  }

  displayScore = score => `[${score.hardScores[0]}]hard/`
    + `[${score.softScores[0]}`
    + `/${score.softScores[1]}`
    + `/${score.softScores[2]}`
    + `/${score.softScores[3]}]soft`;

  onProblemSelectionChange = (selectedProblemId, event) => {
    event.preventDefault();
    this.setState({ selectedProblemId });
    this.props.setTenantId(parseInt(selectedProblemId, 10));
  }

  onSubmitProblemSelectionChange = (toBeSubmittedProblemId, event) => {
    event.preventDefault();
    this.setState({ toBeSubmittedProblemId });
  }

  handleAddProblemModalConfirm = (event) => {
    event.preventDefault();
    this.handleAddProblemModalToggle();
    this.submitProblem(this.state.toBeSubmittedProblemId);
  }

  handleGetSolution = (event) => {
    event.preventDefault();
    this.props.updateBestSolution();
  }

  populateSelections = options => (
    options.map((option, index) => (
      <FormSelectOption
        key={option.id}
        isDisabled={false}
        value={option.id}
        label={option.label}
      />
    ))
  );

  render() {
    return (
      <div className="container">
        <br />
        <div className="row mb-3">
          <div className="col">
            <Card className="text-center">
              <CardHeader>New problem</CardHeader>
              <CardBody>
                <div className="row">
                  <div className="col">
                    Submit a task assignment problem and start solving it
                  </div>
                </div>
                <br />
                <div className="row">
                  <div className="col">
                    <Button onClick={this.handleAddProblemModalToggle} variant="primary">Add a problem</Button>
                    <Button onClick={this.handleGetSolution} variant="secondary" className="ml-2"> Get solution</Button>
                  </div>
                </div>
              </CardBody>
            </Card>
            <Modal
              title="Add problem"
              isOpen={this.state.isAddProblemModalOpen}
              onClose={this.handleAddProblemModalToggle}
            >
              <Form>
                <FormGroup
                  label="Problem"
                  isRequired
                  fieldId="problemSubmit"
                >
                  <FormSelect
                    value={this.state.toBeSubmittedProblemId}
                    onChange={this.onSubmitProblemSelectionChange}
                    id="problemSubmit"
                    name="Submit problem"
                  >
                    {this.populateSelections(this.state.allProblems)}
                  </FormSelect>
                </FormGroup>
                <ActionGroup>
                  <Toolbar>
                    <ToolbarGroup>
                      <Button key="confirmAddProblem" variant="primary" onClick={this.handleAddProblemModalConfirm}>Add</Button>
                    </ToolbarGroup>
                    <ToolbarGroup>
                      <Button key="cancelAddProblem" variant="secondary" onClick={this.handleAddProblemModalToggle}>Cancel</Button>
                    </ToolbarGroup>
                  </Toolbar>
                </ActionGroup>
              </Form>
            </Modal>
          </div>
        </div>

        <div className="row mb-3">
          <div className="col">
            <Card className="text-center">
              <CardHeader>Load an existing problem</CardHeader>
              <CardBody>
                <Form>
                  <FormGroup label="Problem" fieldId="problemSelectionId">
                    <FormSelect
                      value={this.state.selectedProblemId}
                      onChange={this.onProblemSelectionChange}
                      id="problemSelectionId"
                      name="Problem Selection"
                    >
                      {this.populateSelections(this.state.submittedProblems)}
                    </FormSelect>
                  </FormGroup>
                </Form>
              </CardBody>
            </Card>
          </div>
        </div>

        <div className="row mb-4">
          <div className="col-12">
            <AutoProduceConsume
              tasks={this.props.bestSolution.taskList ? this.props.bestSolution.taskList : []}
              taskTypes={this.props.bestSolution.taskTypeList
                ? this.props.bestSolution.taskTypeList : []}
              customers={this.props.bestSolution.customerList
                ? this.props.bestSolution.customerList : []}
              updateBestSolution={this.props.updateBestSolution}
            />
          </div>
        </div>

        <Card>
          <CardHeader className="text-center">
            {this.props.bestSolution.score && (
              <div className="col-12">
                Score:&nbsp;
                {this.displayScore(this.props.bestSolution.score)}
              </div>
            )}
          </CardHeader>
          <CardBody>
            <Schedule bestSolution={this.props.bestSolution} />
          </CardBody>
        </Card>
      </div>
    );
  }
}

Home.propTypes = {
  bestSolution: PropTypes.instanceOf(Object).isRequired,
  updateBestSolution: PropTypes.func.isRequired,
  tenantId: PropTypes.number.isRequired,
  setTenantId: PropTypes.func.isRequired,
};

export default Home;
