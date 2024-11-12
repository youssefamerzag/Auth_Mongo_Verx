package com.example.auth;

import io.vertx.core.Future;
import io.vertx.ext.auth.HashingStrategy;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.mongo.MongoAuthentication;
import io.vertx.ext.auth.mongo.MongoAuthenticationOptions;
import io.vertx.ext.auth.mongo.MongoUserUtil;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.openapi.router.OpenAPIRoute;
import io.vertx.ext.web.openapi.router.RequestExtractor;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.openapi.contract.OpenAPIContract;

public class AuthVerticle extends AbstractVerticle {

  public MongoClient mongoClient;
  public MongoAuthenticationOptions options;
  public MongoAuthentication authenticationProvider;
  public HashingStrategy hashingStrategy;
  public MongoUserUtil userUtil;
  public RouterBuilder routerBuilder;

  public void start() {

    JsonObject config = new JsonObject()
      .put("connection_string" , "mongodb://localhost:27017")
      .put("db_name" , "vertxAuth");

    mongoClient = MongoClient.createShared(vertx , config);

    options = new MongoAuthenticationOptions().setCollectionName("user");
    authenticationProvider = MongoAuthentication.create(mongoClient, options);

    userUtil = MongoUserUtil.create(mongoClient);

//    router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
//    router.route().handler(BodyHandler.create());
//    router.route("/*").handler(StaticHandler.create("webroot"));
//    router.route("/home").handler(this::authCheck).handler(StaticHandler.create("webroot/home.html"));
//    router.post("/signUp").handler(this::signUp);
//    router.post("/login").handler(this::login);

    //openApi
    String pathContract = "./auth.yaml";
    Future<OpenAPIContract> contract = OpenAPIContract.from(vertx , pathContract);

    contract.onSuccess(contractResult -> {
      routerBuilder = RouterBuilder.create(vertx, contractResult, RequestExtractor.withBodyHandler());
      routerBuilder.rootHandler(SessionHandler.create(LocalSessionStore.create(vertx)));
      routerBuilder.rootHandler(BodyHandler.create());

      routerBuilder.getRoute("signUp").addHandler(this::signUp);
      routerBuilder.getRoute("login").addHandler(this::login);
      routerBuilder.getRoute("home").addHandler(this::authCheck);

      Router router = routerBuilder.createRouter();
      router.route().handler(StaticHandler.create("webroot"));

      vertx.createHttpServer().requestHandler(router).listen(8000 , res -> {
        if(res.succeeded()) {
          System.out.println("server is running");
        }else {
          System.out.println("error " + res.cause());
        }
      });
    }).onFailure(error -> {
      System.out.println("Failed to load OpenAPI contract: " + error.getMessage());
    });
  }

//  public void signUp1 (RoutingContext ctx) {
//    JsonObject bodyData = ctx.getBodyAsJson();
//    String username = bodyData.getString("username");
//    String password = bodyData.getString("password");
//
//    //JsonObject ifExists = new JsonObject().put("username" , username);
//
////    mongoClient.find("users" , ifExists , res -> {
////      if(!res.result().isEmpty()){
////        ctx.response().setStatusCode(500).end("username is already exists");
////        return;
////      }
//
////    Map <String, String> options = new HashMap<>();
////    options.put("username" , username);
////
////    String hashedPassword = hashingStrategy.hash("sha512", options, username, password);
////
////    JsonObject user = new JsonObject()
////      .put("username" , username)
////      .put("password" , hashedPassword);
//
////    mongoClient.insert("users" , user , insertRes -> {
////      if(insertRes.succeeded()){
////        ctx.response().end("account created");
////      }else {
////        ctx.response().setStatusCode(500).end("error : " + insertRes.cause());
////      }
////    });
//    //});
//  }

  public void signUp(RoutingContext ctx) {
    JsonObject bodyData = ctx.getBodyAsJson();
    String username = bodyData.getString("username");
    String password = bodyData.getString("password");

    JsonObject ifExists = new JsonObject()
      .put("username", username);

    mongoClient.find("user" , ifExists, findRes -> {
      if (!findRes.result().isEmpty()){
        ctx.response().setStatusCode(500).end("this username is already exists!");
        return;
      }
      userUtil.createUser(username, password, res -> {
        if (res.succeeded()) {
          ctx.response().setStatusCode(200).end("Account created successfully " + username);
        } else {
          ctx.response().setStatusCode(500).end("Error: " + res.cause().getMessage());
        }
      });
    });
  }

//  public void login1 (RoutingContext ctx) {
//    JsonObject bodyData = ctx.getBodyAsJson();
//    String username = bodyData.getString("username");
//    String password = bodyData.getString("password");
//
//    JsonObject infos = new JsonObject()
//      .put("username", username)
//      .put("password", password);
//
//    authenticationProvider.authenticate(infos , res -> {
//      if(res.succeeded()) {
//        ctx.session().put("username" ,username );
//        ctx.session().put("password" ,password );
//        ctx.response().end("done");
//      }else {
//        ctx.response().setStatusCode(401).end("error : " + res.cause());
//      }
//    });
//  }

  public void login (RoutingContext ctx) {
    JsonObject bodyData = ctx.getBodyAsJson();
    String username = bodyData.getString("username");
    String password = bodyData.getString("password");

    JsonObject infos = new JsonObject()
      .put("username" , username)
      .put("password" , password);

  authenticationProvider.authenticate(infos , res -> {
    if(res.succeeded()) {
      User user = res.result();
      ctx.setUser(user);
      ctx.response().end("user is logged " + ctx.get("user"));
    }else {
      ctx.response().setStatusCode(401).end("error : " + res.cause());
    }
  });
  }

  public void authCheck(RoutingContext ctx){
    User user = ctx.user();
    if(user != null){
      ctx.response().sendFile("webroot/home.html");
    }else {
      ctx.redirect("/login.html");
    }
  }

  public static void main(String[] args) {
    Vertx vertx1 = Vertx.vertx();
    vertx1.deployVerticle(new AuthVerticle());
  }
}
