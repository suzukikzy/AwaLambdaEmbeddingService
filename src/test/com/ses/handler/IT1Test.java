package test.com.ses.handler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ses.handler.LambdaHandler;


public class IT1Test {
    /**
     * このlambda関数のIT1テスト.
     *
     * @throws IOException 
     */
    @Test
    void it1() throws IOException {
        // IT1テストのInputを読み込む
        String content = new String(Files.readAllBytes(Paths.get("src/test/resources/it1input.json")));

        // JSON を SQSEvent に変換
        ObjectMapper objectMapper = new ObjectMapper();
        SQSEvent sqsEvent = objectMapper.readValue(content, SQSEvent.class);

        // テスト実行
        LambdaHandler lambdaHandler = new LambdaHandler();
        String response = lambdaHandler.handleRequest(sqsEvent, null);
        System.out.println(response);
    }
}
