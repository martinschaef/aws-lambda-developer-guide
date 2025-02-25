package com.martinschaef.mylambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyResponseEvent;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvokeTest {
  private static final Logger logger = LoggerFactory.getLogger(InvokeTest.class);

  @Test
  void invokeTest() {
    logger.info("Invoke TEST");
    APIGatewayV2ProxyRequestEvent event = new APIGatewayV2ProxyRequestEvent();
    Context context = new TestContext();
    String requestId = context.getAwsRequestId();
    Handler handler = new Handler();
    APIGatewayV2ProxyResponseEvent result = handler.handleRequest(event, context);
    assertEquals(200, result.getStatusCode());
  }

}
