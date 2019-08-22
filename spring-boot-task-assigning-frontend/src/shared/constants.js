export default {
  MINUTE_STEP: 30,
  UNASSIGNED_ID: Number.MAX_SAFE_INTEGER,
  PRIORITIES: ['MINOR', 'MAJOR', 'CRITICAL'],
  START_DATE: {
    year: 2018,
    month: 0,
    day: 1,
  },
};

export const problems = [
  {
    id: 0,
    taskLiseSize: 1,
    employeeListSize: 1,
    label: '1 task 1 employee',
  },
  {
    id: 1,
    taskLiseSize: 10,
    employeeListSize: 4,
    label: '10 tasks 4 employees',
  },
  {
    id: 2,
    taskLiseSize: 20,
    employeeListSize: 4,
    label: '20 tasks 4 employees',
  },
  {
    id: 3,
    taskLiseSize: 40,
    employeeListSize: 10,
    label: '40 tasks 10 employees',
  },
  {
    id: 4,
    taskLiseSize: 100,
    employeeListSize: 16,
    label: '100 tasks 16 employees',
  },
  {
    id: 5,
    taskLiseSize: 200,
    employeeListSize: 16,
    label: '200 tasks 16 employees',
  },
  {
    id: 6,
    taskLiseSize: 400,
    employeeListSize: 32,
    label: '400 tasks 32 employees',
  },
  {
    id: 7,
    taskLiseSize: 400,
    employeeListSize: 64,
    label: '400 tasks 32 employees',
  },
  {
    id: 8,
    taskLiseSize: 600,
    employeeListSize: 64,
    label: '400 tasks 32 employees',
  },
  {
    id: 9,
    taskLiseSize: 1000,
    employeeListSize: 128,
    label: '400 tasks 32 employees',
  },
];
