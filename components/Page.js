import Link from "next/link";
import Head from "next/head";

export default ({ title, children }) =>
  <div>
    <Head>
      <meta charset="UTF-8" />
      <title>
        {title}
      </title>
    </Head>
    <header>
      <Link href="/">
        <a>Mono(noke )



                    </a>
      </Link>
    </header>
    {children}
  </div>;
