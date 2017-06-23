# Mono

Blogging platform, with read-side on Web and Chatbot, and write-side via Chatbot only.

Not yet ready for production, but going to be. Written on functional Scala.

## Startup

All you need is:

`sbt run`

App is launched on `http://localhost:9000`.

If you have `src/main/resources/bot.token` with telegram's bot token, bot will be launched. Use it.

Anyway, console bot should be working. 

Try working with either using `/new Draft Title` command.

Telegram refuses URLs with localhost domain. If you get edit link with wrong domain, substitute `http://localhost:9000` instead.

App serves static files from `sec/main/resources/web` folder. No build process, no javascript, no SPA. At least, yet.