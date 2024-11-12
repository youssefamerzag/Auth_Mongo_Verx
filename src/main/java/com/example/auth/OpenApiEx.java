package com.example.auth;

import com.sun.nio.sctp.HandlerResult;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.openapi.router.OpenAPIRoute;
import io.vertx.ext.web.openapi.router.RequestExtractor;
import io.vertx.ext.web.openapi.router.RouterBuilder;
import io.vertx.openapi.contract.OpenAPIContract;
import io.vertx.openapi.contract.Operation;
import io.vertx.openapi.validation.RequestValidator;
import io.vertx.openapi.validation.ResponseValidator;
import io.vertx.openapi.validation.ValidatableResponse;

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;

public class OpenApiEx extends AbstractVerticle {

  public RouterBuilder routerBuilder;
  public Router router;
  public RequestValidator requestValidator;
  public ResponseValidator responseValidator;
  public Future<OpenAPIContract> contract;

  public void start() {
    String pathContract = "./api.yaml";
    contract = OpenAPIContract.from(vertx , pathContract);

    contract.onSuccess(contractRes -> {
      routerBuilder = RouterBuilder.create(vertx, contractRes, RequestExtractor.withBodyHandler());
      routerBuilder.rootHandler(BodyHandler.create());

      requestValidator = RequestValidator.create(vertx , contractRes);
      responseValidator = ResponseValidator.create(vertx , contractRes);

      routerBuilder.getRoute("addUser").addHandler(this::addUser);
      routerBuilder.getRoute("addPet").addHandler(this::addPet);

      for (OpenAPIRoute route : routerBuilder.getRoutes()) {
        Operation operation = route.getOperation();
        //route.setDoValidation(false);

//        route.addFailureHandler(routingContext -> {
//          System.out.println("error " + operation.getAbsoluteOpenAPIPath());
//          System.out.println("error " + operation.getHttpMethod());
//          System.out.println("error " + operation.getRequestBody().getOpenAPIModel());
//        });
      }
      router = routerBuilder.createRouter();

      vertx.createHttpServer().requestHandler(router).listen(8000 , res -> {
        if (res.succeeded()) {
          System.out.println("server is running");
        } else {
          System.out.println("error " + res.cause());
        }
      });
    }).onFailure(error -> {
      System.out.println("Failed to load OpenAPI contract: " + error.getMessage());
    });
  }

//  public void getPets(RoutingContext ctx) {
//    JsonObject pet = new JsonObject();
//    JsonArray petsArray = new JsonArray().add(pet);
//    ValidatableResponse response = ValidatableResponse.create(200, petsArray.toBuffer(), APPLICATION_JSON.toString());
//
//    responseValidator.validate(response, "getPets").onSuccess(validatedResponse -> {
//      validatedResponse.send(ctx.response());
//    }).onFailure(err -> {
//      ctx.response().setStatusCode(500).end("error : " + err.getMessage());
//    });
//  }

  public void addPet(RoutingContext ctx) {
      JsonObject bodyData = ctx.getBodyAsJson();
      String name = bodyData.getString("name");

      if(!name.isEmpty()) {
        ctx.response().end("name is : " + name);
      }else {
        ctx.response().end("error " + name);
      }
  }

//  public void addUser(RoutingContext ctx) {
//    requestValidator.validate(ctx.request(), "addUser").onSuccess(validatedRequest -> {
//      JsonObject bodyData = validatedRequest.getBody().getJsonObject();
//      String name = bodyData.getString("name");
//
//      if(!name.isEmpty()) {
//        ctx.response().end("name is : " + bodyData);
//      }else {
//        ctx.response().end("error " + name);
//      }
//    }).onFailure(err -> {
//      ctx.response().setStatusCode(400).end("error " + err.getMessage());
//    });
//  }

  public void addUser(RoutingContext ctx) {
      JsonObject bodyData = ctx.getBodyAsJson();
      String name = bodyData.getString("name");

      if(!name.isEmpty()) {
        ctx.response().end("name is : " + bodyData);
      }else {
        ctx.response().end("error " + name);
      }
  }

//  public void addPet(RoutingContext ctx) {
//    JsonObject bodyData = ctx.getBodyAsJson();
//    String name = bodyData.getString("name");
//
//    if(!name.isEmpty()) {
//      ctx.response().end("name " + name);
//    }else {
//      ctx.response().end("error");
//    }
//  }

  public static void main(String[] args) {
    Vertx vertx1 = Vertx.vertx();
    vertx1.deployVerticle(new OpenApiEx());
  }
}
