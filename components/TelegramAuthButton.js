import { graphql, withApollo, compose } from "react-apollo";
import gql from "graphql-tag";
import cookie from 'cookie'
import redirect from "../lib/redirect"

const TelegramAuthSubscription = gql`
  subscription TelegramAuth($token: String!) {
    trackTelegramAuth(token: $token) {
        token
    }
  }
`;

const TelegramAuthButton = ({mutate,client}) => {

  return (<div>
    <button
      onClick={() =>
        mutate().then(({data: {getTelegramLoginToken}}) => {
          client
            .subscribe({
              query: TelegramAuthSubscription,
              variables: {
                token: getTelegramLoginToken
              }
            })
            .subscribe({
              next({trackTelegramAuth: {token}}) {
                console.log("token: ", token)
                document.cookie = cookie.serialize('token', token, {
                  maxAge: 30 * 24 * 60 * 60 // 30 days
                });
                client.resetStore().then(() => {
                  // Now redirect to the homepage
                  redirect({}, '/')
                })
              },
              error(err) {
                console.err("TelegramAuthButton error", err);
              }
            });
        })}
    >
      Auth by Telegram
    </button>
  </div>);
}

export default compose(
  withApollo,
  graphql(gql`
    mutation GetTelegramAuthToken {
      getTelegramLoginToken
    }
  `)
)(TelegramAuthButton);
