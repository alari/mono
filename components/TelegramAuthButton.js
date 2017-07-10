import { graphql, withApollo, compose } from "react-apollo";
import gql from "graphql-tag";

const TelegramAuthSubscription = gql`
  subscription TelegramAuth($token: String!) {
    trackTelegramAuth(token: $token) {
        token
    }
  }
`;

const TelegramAuthButton = ({ mutate, client }) =>
  <div>
    <button
      onClick={() =>
        mutate().then(({ data: { getTelegramLoginToken } }) => {
        console.log("token = ",getTelegramLoginToken)
          client
            .subscribe({
              query: TelegramAuthSubscription,
              variables: {
                token: getTelegramLoginToken
              }
            })
            .subscribe({
              next({trackTelegramAuth:{token}}) {
                // TODO: save token, refetch all queries
                console.log("got: ", token);
              },
              error(err) {
                console.err("err: ", err);
              }
            });
        })}
    >
      Auth by Telegram
    </button>
  </div>;

export default compose(
  withApollo,
  graphql(gql`
    mutation GetTelegramAuthToken {
      getTelegramLoginToken
    }
  `)
)(TelegramAuthButton);
