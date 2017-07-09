import { ApolloClient, createNetworkInterface } from "react-apollo";
import { SubscriptionClient, addGraphQLSubscriptions } from './subscriptions-transport-sse'

import fetch from "isomorphic-fetch";

let apolloClient = null;

// Polyfill fetch() on the server (used by apollo-client)
if (!process.browser) {
  global.fetch = fetch;
}

function create(initialState, { getToken }) {

  const httpClient = createNetworkInterface({
    uri: "http://localhost:9000/graphql"
  });

  httpClient.use([
    {
      applyMiddleware(req, next) {
        if (!req.options.headers) {
          req.options.headers = {}; // Create the header object if needed.
        }
        const token = null; // getToken()
        req.options.headers.authorization = token ? `Bearer ${token}` : null;
        next();
      }
    }
  ]);

  const networkInterface = process.browser ?
    addGraphQLSubscriptions(httpClient, new SubscriptionClient("http://localhost:9000/graphql", {timeout:10000}))
    : httpClient;

  return new ApolloClient({
    initialState,
    ssrMode: !process.browser, // Disables forceFetch on the server (so queries are only run once)
    networkInterface
  });
}

export default function initApollo(initialState, options) {
  // Make sure to create a new client for every server-side request so that data
  // isn't shared between connections (which would be bad)
  if (!process.browser) {
    return create(initialState, options);
  }

  // Reuse client on the client-side
  if (!apolloClient) {
    apolloClient = create(initialState, options);
  }

  return apolloClient;
}
