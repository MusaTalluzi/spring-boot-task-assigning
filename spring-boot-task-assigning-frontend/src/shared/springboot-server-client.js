export const loadSubmittedProblemIds = () => (
  fetch('/tenants')
    .then(response => verifyResponse(response),
      (error) => { throw new Error(error.message); })
    .catch(error => console.log(error))
);

export const addProblem = (problemId, taskLiseSize, employeeListSize) => (
  fetch(`/tenants/${problemId}/solver/generate/${taskLiseSize}/${employeeListSize}`, {
    method: 'POST',
  })
    .then(response => verifyResponse(response),
      (error) => { throw new Error(error.message); })
    .catch((error) => {
      console.log(error);
      error.response.json().then(body => console.log(body.message));
    })
);

export const updateBestSolution = tenantId => (
  fetch(`/tenants/${tenantId}/solver/bestSolution`)
    .then(response => verifyResponse(response),
      (error) => { throw new Error(error.message); })
    .then(response => response.json())
    .catch((error) => {
      console.log(error);
      error.response.json().then(body => console.log(body.message));
    })
);

function verifyResponse(response) {
  if (response.ok) {
    return response;
  }
  const error = new Error(`${response.status}: ${response.statusText}`);
  error.response = response;
  throw error;
}
