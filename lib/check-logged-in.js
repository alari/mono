import gql from "graphql-tag";

export default (context, apolloClient) =>
  apolloClient
    .query({
      query: gql`
        query me {
          person
          me {
            id
            name
          }
        }
      `
    })
    .then(({ data }) => {
      return { loggedInUser: data };
    })
    .catch(() => {
      // Fail gracefully
      return { loggedInUser: {} };
    });
