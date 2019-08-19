import React, { Component } from 'react';
import {
  Card, CardHeader, CardBody, Button, Modal,
  Form, FormGroup, ActionGroup, Toolbar, ToolbarGroup, TextArea,
} from '@patternfly/react-core';
import JXON from 'jxon';
import PropTypes from 'prop-types';

import Schedule from './ScheduleComponent';
import AutoProduceConsume from './AutoProduceConsumeComponent';

import PROBLEM from '../shared/24tasks';
// import PROBLEM from '../shared/simpleProblem';

import { addProblem } from '../shared/kie-server-client';

class Home extends Component {
  constructor(props) {
    super(props);
    this.state = {
      isAddProblemModalOpen: false,
      problem: JXON.xmlToString(JXON.jsToXml(PROBLEM)),
    };

    this.handleAddProblemModalToggle = this.handleAddProblemModalToggle.bind(this);
    this.handleAddProblemModalConfirm = this.handleAddProblemModalConfirm.bind(this);
    this.handleGetSolution = this.handleGetSolution.bind(this);
  }

  handleAddProblemModalToggle = () => {
    this.setState(({ isAddProblemModalOpen }) => ({
      isAddProblemModalOpen: !isAddProblemModalOpen,
    }));
  }

  handleAddProblemModalConfirm(event) {
    event.preventDefault();
    this.handleAddProblemModalToggle();

    addProblem(this.state.problem, this.state.container.containerId, this.state.solver.id)
      .then(response => alert('Problem submitted successfully, solver is solving now.'));
  }

  handleGetSolution(event) {
    event.preventDefault();
    this.props.updateBestSolution();
  }

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
                  fieldId="problem"
                >
                  <TextArea
                    isRequired
                    id="problem"
                    rows="20"
                    value={this.state.problem}
                    onChange={(problem) => { this.setState({ problem }); }}
                  />
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
                &nbsp;Hard:
                {this.props.bestSolution.score.hardScores[0]}
                &nbsp;Soft0:
                {this.props.bestSolution.score.softScores[0]}
                &nbsp;Soft1:
                {this.props.bestSolution.score.softScores[1]}
                &nbsp;Soft2:
                {this.props.bestSolution.score.softScores[2]}
                &nbsp;Soft3:
                {this.props.bestSolution.score.softScores[3]}
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
};

export default Home;
