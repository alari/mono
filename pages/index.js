import Page from "../components/Page.js";
import Link from "next/link";

import { graphql, withApollo, compose } from "react-apollo";
import gql from "graphql-tag";

import withData from "../lib/with-data";

const Index = ({ data: { articles: { count, values } } }) => {
  return (
    <Page title="Mono(noke) Index">
      <h1>
        Articles: {count}
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
  }
`;

export default compose(
  // withData gives us server-side graphql queries before rendering
  withData,
  // withApollo exposes `this.props.client` used when logging out
  withApollo,
  graphql(ArticlesQuery, {
    options: (props) => {
      console.log("GET: ", props)
      return ({
        variables: {offset: 0, limit: 10}
      })
    }
  })
)(Index);
