### Play Framework 2.2 with Angular Dart and WebSockets

In this branch I am just playing (everything is going to be changed) with Neo4j graph db.

For accessing the db its REST api is used (in this branch, there is also a branch for the embedded mode).

To make this branch work it is necessary to have a local Neo4j instance running.

The client side uses Dart and AngularDart to build the UIs. Connection between client and server is implemented
with WebSockets (after the page is loaded, the scripts opens up a WebSocket connection; once the connection is
ready the script sends a message to the server, and the server responds to it with message containing the requested data).






