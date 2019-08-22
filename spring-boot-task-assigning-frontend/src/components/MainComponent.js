import React, { Component } from 'react';
import { Switch, Route, Redirect } from 'react-router-dom';

import Header from './HeaderComponent';
import Home from './HomeComponent';

import { updateBestSolution } from '../shared/springboot-server-client';

class Main extends Component {
  constructor(props) {
    super(props);

    this.state = {
      tenantId: 0,
      bestSolution: {},
    };

    this.setTenantId = this.setTenantId.bind(this);
    this.handleGetSolution = this.handleGetSolution.bind(this);
    this.update = this.update.bind(this);
  }

  componentDidMount() {
    this.update();
  }

  setTenantId(tenantId) {
    this.setState({ tenantId }, () => this.update());
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
                tenantId={this.state.tenantId}
                setTenantId={this.setTenantId}
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
