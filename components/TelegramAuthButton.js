import { graphql, withApollo, compose } from "react-apollo";
import gql from "graphql-tag";

const RepeatSubscription = gql`
  subscription onRepeat($token: String!) {
    repeat(token: $token)
  }
`;

const TelegramAuthButton = ({ mutate, client }) =>
  <div>
    <button
      onClick={() =>
        mutate().then(({ data: { getTelegramLoginToken } }) => {
          client
            .subscribe({
              query: RepeatSubscription,
              variables: {
                token: getTelegramLoginToken
              }
            })
            .subscribe({
              next(data) {
                console.log("GOT NEXT: ", data);
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
