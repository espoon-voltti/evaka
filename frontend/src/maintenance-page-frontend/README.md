# maintenance-page-frontend

Simple static maintenance page that can be deployed alongside eVaka to show
when the application is otherwise unavailable (e.g. under maintenance).

This page is provided here as an example but is complete enough to be deployed
as-is.

## Usage

As how you display a maintenance page is entirely dependent on your deployment
environment, there is no specific way to use this page but here is one way to
do it:

- Copy this directory to some stable file host (e.g. S3 w/ website enabled)
- Configure a condition on your load balancer / reverse proxy that responds
  to all requests with this `index.html` when enabled

## Development

From `frontend/`, run `yarn dev-maintenance-page`.
