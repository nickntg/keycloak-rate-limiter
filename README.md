# Keycloak Rate Limiter

This is an authenticator plugin that can rate-limit requests based on 1 second and 1 minute window configured velocities. The default velocities are one request per second and 3 requests per minute. The default velocities can be overriden when the authenticator is added on an authorization flow. When the limiter denies a request, it responds with 429 and the limit that was breached.

## Pros
* Performs the work with a single trip to the database when using PostgreSQL or MSSQL, or two trips when using MySQL.
* One single record required for each user checked per attached client.

## Cons
* Supports only PostgreSQL, MSSQL and MySQL.
