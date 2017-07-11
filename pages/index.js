import Page from "../components/Page.js";
import Link from "next/link";

import { graphql, withApollo, compose } from "react-apollo";
import gql from "graphql-tag";

import TelegramAuthButton from "../components/TelegramAuthButton"

import withData from "../lib/with-data";

const Index = ({ data: { articles: { count, values }, me } }) => {
  console.log("me: ", me)
  return (
    <Page title={"Mono(noke) Index" + me}>
      <h1>
        Articles: {count} | <TelegramAuthButton/>
      </h1>
      {values.map(article =>
        <article id={article.id}>
          <Link
            to={"/article?id=" + article.id}
            as={article.alias && `/${article.alias}`}
          >
            <h1>
              {article.title}
            </h1>
          </Link>
          <div>
            {article.authors.map(person =>
              <Link to={`/person?id=${person.id}`}>
                <a>
                  {person.name}
                </a>
              </Link>
            )}
          </div>
        </article>
      )}
    </Page>
  );
};

const ArticlesQuery = gql`
  query ArticlesQuery($offset: Int!, $limit: Int!) {
    articles(offset: $offset, limit: $limit) {
      count
      values {
        id
        alias
        title
        authors {
          id
          alias
          name
        }
      }
    }
      me {
          person {
              id
          }
      }
  }
`;

export default compose(
  // withData gives us server-side graphql queries before rendering
  withData,
  // withApollo exposes `this.props.client` used when logging out
  withApollo,
  graphql(ArticlesQuery, {
    options: props => ({
      variables: {
        offset: props.url.query.page ? props.url.query.page * 10 : 0,
        limit: 10
      }
    })
  })
)(Index);
