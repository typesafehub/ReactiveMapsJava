package controllers;

import actors.Actors;
import actors.ClientConnection;
import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.*;

public class Application extends Controller {

  /**
   * The index page.
   */
  public static Result index() {
    return ok(views.html.index.render());
  }

  /**
   * The WebSocket
   */
  public static WebSocket<JsonNode> stream(String email) {
      return WebSocket.withActor(upstream -> ClientConnection.props(email, upstream, Actors.regionManagerClient()));
  }
}