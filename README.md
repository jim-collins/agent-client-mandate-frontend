agent-client-mandate-frontend
=============================

This service creates a relationship between an agent and a client for a given service. Once the mandate has been agreed it's stored in a locally and ensures that the agent can work on behalf of the client for that service.


[![Build Status](https://travis-ci.org/hmrc/agent-client-mandate-frontend.svg)](https://travis-ci.org/hmrc/agent-client-mandate-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/agent-client-mandate-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/agent-client-mandate-frontend/_latestVersion)


## Useful Pages for external services to link to

| PATH | Supported Methods | Description |
|------|-------------------|-------------|
| ```/mandate/agent/service ``` | GET | Shows a list of services that the agent may have clients for. If there is only one then it should forward to the summary page |
| ```/mandate/agent/summary/:service``` | GET | Shows any pending or current clients for a given service. Also has a link to create a mandate for a new client |
| ```/mandate/email/:service``` | GET | Show the page to allow an agent to create a mandate for a new client |
| ```/mandate/client/email``` | GET | A link to a page for the client to enter their email and then the mandate reference so that they can accept it |


# Adding a new service

## First service to be added after ATED
Note: If this is the first service to be added after ATED then the feature switch at MandateFeatureSwitches.singleService will have to be removed.
This feature switch causes the ```/mandate/agent/service ``` to be skipped and go straight to the ATED summary page.

## Adding a new service to the view
Update the page selectService.scala.html to add any new services that the agent can choose from.

## Update the application.conf to add links back to the new service
Ensure that you update application.conf with the links back to the dev version of your service.
i.e.
  delegated-service-redirect-url {
    ated = "http://localhost:9916/ated/account-summary"
  }

  delegated-service-home-url {
    ated = "http://localhost:9916/ated/welcome"
  }

These also need to be updated in app-config-prod


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
