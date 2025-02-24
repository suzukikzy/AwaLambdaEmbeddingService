package com.ses.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import main.java.com.generativeailibrary.api.OpenAI;
import main.java.com.generativeailibrary.unit.Vector;
import main.java.com.generativeailibrary.util.LambdaLogger;

/**
 * 【SES AIアシスタント】
 * lambda関数がリクエストを受け取り処理を実行するMainクラス.
 *
 * @author 鈴木一矢.
 *
 */
public class LambdaHandler implements RequestHandler<SQSEvent, String> {
    /**
     * invoke_id.
     */
    private String invokeId;

    @Override
    public String handleRequest(SQSEvent input, Context context) {
        System.out.println(input);
        this.invokeId = UUID.randomUUID().toString();

        // 入力チェック
        if (input.getRecords() == null || input.getRecords().size() < 1) {
            LambdaLogger.errorLogMessage(this.invokeId + " レコードが存在しないSQSを受信しました。処理を終了します。");
            return null;
        }

        // OpenAIのAPIキーを環境変数から取得する
        final String OPEN_AI_API_KEY = System.getenv("OPEN_AI_API_KEY");
        if (OPEN_AI_API_KEY == null || OPEN_AI_API_KEY.length() < 1 || "".equals(OPEN_AI_API_KEY)) {
            LambdaLogger.errorLogMessage(this.invokeId + " OpenAI APIキーが定義されていません。処理を終了します。");
            return null;
        }

        // 結果リストを定義
        List<Vector> resultList = new ArrayList<Vector>();

        // OpenAIクライアントを生成
        OpenAI openAiClient = new OpenAI(OPEN_AI_API_KEY);

        // ObjectMapperのインスタンスを作成
        ObjectMapper objectMapper = new ObjectMapper();

        LambdaLogger.infoLogMessage(this.invokeId + " エンベディング処理を開始します。");
        LambdaLogger.debugLogMessage(this.invokeId + " SQSレコード数: " + Integer.toString(input.getRecords().size()));

        // SQSに渡されたレコードの数の分だけ繰り返す
        for (SQSEvent.SQSMessage message : input.getRecords()) {
            // ボディ部をコンソールに出力
            String body = message.getBody();
            LambdaLogger.debugLogMessage(this.invokeId + " Body: " + body);

            // bodyを解析してtargetStringの値を取得しコンソールに出力
            String targetString = null;
            try {
                JsonNode jsonNode = objectMapper.readTree(body);
                targetString = jsonNode.get("targetString").asText();
                LambdaLogger.debugLogMessage(this.invokeId + " targetString: " + targetString);
            } catch (Exception e) {
                LambdaLogger.errorLogMessage(this.invokeId + " Body部のパースに失敗しました。");
                e.printStackTrace();
                return e.getMessage();
            }

            // エンベディング処理を実行し、結果を返却する.
            Vector vector = new Vector(openAiClient);
            vector.setRawString(targetString);
            try {
                vector.embedding();
                resultList.add(vector);
                LambdaLogger.debugLogMessage(this.invokeId + " 「" + targetString + "」をエンベディングしました。");
            } catch (IOException | RuntimeException e) {
                LambdaLogger.errorLogMessage(this.invokeId + " エンベディング処理に失敗しました。処理を終了します。");
                e.printStackTrace();
                return e.getMessage();
            }
        }

        LambdaLogger.debugLogMessage(this.invokeId + " SQSレコード内の全ての文字列をエンベディングしました。");
        LambdaLogger.infoLogMessage(this.invokeId + " エンベディング処理を終了します。");
        return resultList.toString();
    }
}