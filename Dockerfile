FROM node:8

RUN mkdir -p /opt/app
WORKDIR /opt/app

ADD node_modules /opt/app/node_modules
ADD .next /opt/app/.next
ADD package.json /opt/app/package.json
ADD server.js /opt/app/server.js

EXPOSE 3000
CMD [ "npm", "start" ]