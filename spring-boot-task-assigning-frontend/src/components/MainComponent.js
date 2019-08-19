import React, { Component } from 'react';
import { Switch, Route, Redirect } from 'react-router-dom';

import Header from './HeaderComponent';
import Home from './HomeComponent';
import TaskPage from './TaskPageComponent';

import { updateBestSolution } from '../shared/kie-server-client';

class Main extends Component {
  constructor(props) {
    super(props);

    this.state = {
      tenantId: 0,
      bestSolution: {},
    };

    this.handleGetSolution = this.handleGetSolution.bind(this);
    this.update = this.update.bind(this);
  }

  componentDidMount() {
    this.update();
  }

  update() {
    updateBestSolution(this.state.tenantId)
      .then((response) => {
        if (Object.prototype.hasOwnProperty.call(response, 'score')) {
          console.log(response);
          this.setState({ bestSolution: response });
        }
      });
  }

  handleGetSolution(event) {
    event.preventDefault();
    this.update();
  }

  render() {
    return (
      <div>
        <Header />
        <Switch>
          <Route
            path="/home"
            render={props => (
              <Home
                {...props}
                bestSolution={this.state.bestSolution}
                updateBestSolution={this.update}
              />
            )}
          />
          <Route
            exact
            path="/tasks"
            render={props => (
              <TaskPage
                {...props}
                tasks={this.state.bestSolution.taskList ? this.state.bestSolution.taskList : []}
                taskTypes={this.state.bestSolution.taskTypeList
                  ? this.state.bestSolution.taskTypeList : []}
                customers={this.state.bestSolution.customerList
                  ? this.state.bestSolution.customerList : []}
                updateBestSolution={this.update}
              />
            )}
          />
          <Redirect to="/home" />
        </Switch>
      </div>
    );
  }
}

export default Main;
