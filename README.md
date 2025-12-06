# LARLEW-BACKEND
- A SpringBoot microservice that serves various RESTful services over HTTP calls.
- Used as a development playground for lightweight REST API implementation and testing.

## SwaggerUI Documentation
- Run the project and navigate to this link:
http://localhost:8080/swagger-ui/index.html
- Not all endpoints utilize OpenAPI specs, but Swagger tags have been added to the MathController as a way to highlight using Swagger for manually developed APIs

## Available Endpoints

### Math API
- `GET /math/sum` - Calculate the sum of two integers

### Users API
- `GET /users` - Get all users
- `POST /users` - Create a new user
- `GET /users/{userId}` - Get user by ID

### Actuator
- `GET /actuator/health` - Check application health status
- `GET /actuator/health/liveness` - Check application liveness
- `GET /actuator/health/readiness` - Check application readiness



