package com.greenroom.server.api.domain.greenroom.utils;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Component
public class GardeningDataUtil {

    @Value("${gardening-data.api-key}")
    private String apiKey;
    private WebClient webClient;

    @PostConstruct
    public WebClient webClient(){
        webClient = WebClient.create();
        return webClient;
    }
    ///실내 정원 식물 list 추출
    public Map<String, ArrayList<String>> plantList() {
        log.info("식물 list 받아오는 중");
        Map<String,ArrayList<String>> plantBasic = new HashMap<String,ArrayList<String>>();
        for (int num=1;num<=22;num++) {

            String baseURL = "http://api.nongsaro.go.kr/service/garden/gardenList?apiKey="+apiKey+"&pageNo="+Integer.toString(num);
            String listResult = webClient.get().uri(baseURL)
                    .retrieve()
                    .bodyToMono(String.class).block();

            log.info("page 번호 : " + String.valueOf(num));


            JSONObject jsonResult = XML.toJSONObject(listResult);
            String jsonStr = jsonResult.toString(4);

            JSONArray items = (JSONArray)jsonResult.getJSONObject("response")
                    .getJSONObject("body")
                    .getJSONObject("items")
                    .getJSONArray("item");

            for (int i =0; i<items.length();i++) {
                String name = (String) items.getJSONObject(i).get("cntntsSj");
                String id = (String) items.getJSONObject(i).get("cntntsNo");
                String imageURL = (String) items.getJSONObject(i).get("rtnFileUrl");
                String[] imageURLS = (String[])imageURL.split("\\|");

                ArrayList<String> nameAndUrl = new ArrayList<String>();
                nameAndUrl.add(name);
                nameAndUrl.add(imageURLS[0]);
                plantBasic.put(id,nameAndUrl);
            }
        }
        return plantBasic;

    }

    ///실내 정원 식물 세부 정보
    public JSONArray plantInfo(Map<String,ArrayList<String>> plantList) {
        log.info("식물 상세 정보 받아오는 중");
        String name = "";
        String imageURL = "";
        String sname = "";
        String manageLevel = "";
        String temperature = "";
        String hdCode = "";
        String fertilizer = "";
        String light = "";
        String place = "";
        String waterSpring = "";
        String waterSummer = "";
        String waterAutumn = "";
        String waterWinter = "";
        String origin = "";
        String height = "";
        String type= "";
        String leafColor = "";
        String flowerColor = "";


        JSONArray objList = new JSONArray();
        Iterator<String> keys = plantList.keySet().iterator();

        while(keys.hasNext()){
            String strKey = keys.next();
            name = plantList.get(strKey).get(0);
            imageURL = plantList.get(strKey).get(1);
            String baseURL = "http://api.nongsaro.go.kr/service/garden/gardenDtl?apiKey="+apiKey+"&cntntsNo="+strKey;


            String infoResult = webClient.get().uri(baseURL)
                    .retrieve()
                    .bodyToMono(String.class).block();
            JSONObject jsonResult = XML.toJSONObject(infoResult);

            JSONObject information = jsonResult.getJSONObject("response")
                    .getJSONObject("body")
                    .getJSONObject("item");

            try{
                sname=(String)information.get("plntbneNm");
            }
            catch(JSONException e) {
                sname = "";
            }
            try{
                manageLevel=(String)information.get("managelevelCode");
            }
            catch(JSONException e) {
                manageLevel = "";
            }
            try{
                temperature =(String)information.get("grwhTpCode");
            }
            catch(JSONException e) {
                temperature = "";
            }
            try{
                hdCode=(String)information.get("hdCode");
            }
            catch(JSONException e) {
                hdCode = "";
            }
            try{
                fertilizer=(String)information.get("frtlzrInfo");
            }
            catch(JSONException e) {
                fertilizer = "";
            }
            try{
                light=(String)information.get("lighttdemanddoCodeNm");
            }
            catch(JSONException e) {
                light = "";
            }
            try{
                waterSpring=(String)information.get("watercycleSprngCode");
            }
            catch(JSONException e) {
                waterSpring = "";}
            try{
                waterSummer=(String)information.get("watercycleSummerCode");
            }
            catch(JSONException e) {
                waterSummer = "";}
            try{
                waterAutumn=(String)information.get("watercycleAutumnCode");
            }
            catch(JSONException e) {
                waterAutumn = "";}
            try{
                waterWinter=(String)information.get("watercycleWinterCode");
            }
            catch(JSONException e) {
                waterWinter = "";}

            try{
                origin=(String)information.get("orgplceInfo");
            }
            catch(JSONException e) {
                origin = "";}

            try{
                height=(String)information.get("growthHgInfo");
            }
            catch(JSONException e) {
                height = "";}

            try{
                type =(String)information.get("fmlCodeNm");
            }
            catch(JSONException e) {
                type = "";}

            try{
                place =(String)information.get("postngplaceCodeNm");
            }
            catch(JSONException e) {
                place = "";}

            try{
                leafColor =(String)information.get("lefcolrCodeNm");
            }
            catch(JSONException e) {
                leafColor = "";}

            try{
                flowerColor =(String)information.get("flclrCodeNm");
            }
            catch(JSONException e) {
                flowerColor = "";}

            JSONObject obj = new JSONObject();


            place = place.replace(" (실내깊이 300~500cm)", "").replace(" (실내깊이 150~300cm)", "")
                    .replace(" (실내깊이 50~150cm)", "")
                    .replace(" (실내깊이 0~50cm)", "")
                    .replace(" (실내깊이 500 이상cm)","");

            fertilizer = fertilizer.replace("요구함","요구해요.").replace("하지않음","하지 않아요.");

            String leafinformation = "";
            String flowerinformation = "";
            String baseinformation = name+"는(은) "+type+" 식물입니다. 높이는 "+height+"cm까지 자라며, "+ place+"에서 키우는 것이 가장 적합합니다. ";
            if (!leafColor.isEmpty()) {leafinformation = "잎은 "+ leafColor;}
            if (!flowerColor.isEmpty()) {flowerinformation = "이며 " +flowerColor+"의 꽃이 핍니다."; }
            else {flowerinformation ="입니다.";}

            String otherInformation = baseinformation + leafinformation+flowerinformation;

            //// 물주기 계절별로 요약
            List<String> one = new ArrayList<>();
            List<String> two = new ArrayList<>();
            List<String> three = new ArrayList<>();
            List<String> four = new ArrayList<>();

            if (waterSpring.equals("053001")) {one.add("봄");}
            else if (waterSpring.equals("053002")) {two.add("봄");}
            else if (waterSpring.equals("053003")) {three.add("봄");}
            else if (waterSpring.equals("053004")) {four.add("봄");}

            if (waterSummer.equals("053001")) {one.add("여름");}
            else if (waterSummer.equals("053002")) {two.add("여름");}
            else if (waterSummer.equals("053003")) {three.add("여름");}
            else if (waterSummer.equals("053004")) {four.add("여름");}

            if (waterAutumn.equals("053001")) {one.add("가을");;}
            else if (waterAutumn.equals("053002")) {two.add("가을");}
            else if (waterAutumn.equals("053003")) {three.add("가을");}
            else if (waterAutumn.equals("053004")) {four.add("가을");}

            if (waterWinter.equals("053001")) {one.add("겨울");}
            else if (waterWinter.equals("053002")) {two.add("겨울");}
            else if (waterWinter.equals("053003")) {three.add("겨울");}
            else if (waterWinter.equals("053004")) {four.add("겨울");}

            String watercycle = "";

            if(!one.isEmpty()) {
                String  temp = String.join(",", one);
                watercycle = watercycle +temp+"에는 흙을 항상 축축하게 물에 잠길 정도로 유지해주세요. ";
            }
            if(!two.isEmpty()) {
                String  temp = String.join(",", two);
                watercycle = watercycle +temp+"에는 물에 잠기지 않도록 주의하여 흙을 촉촉하게 유지해주세요. ";
            }
            if(!three.isEmpty()) {
                String  temp = String.join(",", three);
                watercycle = watercycle +temp+"에는 토양 표면이 말랐을때 충분히 관수해주세요. ";
            }
            if(!four.isEmpty()) {
                String  temp = String.join(",", four);
                watercycle = watercycle +temp+"에는 화분 흙이 대부분 말랐을때 충분히 관수해주세요. ";
            }


            /////광도 표현 방법 변경

            if (light.contains("낮은 광도(300~800 Lux)") && light.contains("중간 광도(800~1,500 Lux)") && light.contains("높은 광도(1,500~10,000 Lux)")) {light="햇빛에 크게 영향을 받지 않아요.";}
            else if (light.contains("낮은 광도(300~800 Lux)") && light.contains("중간 광도(800~1,500 Lux)"))  {light="햇빛이 필요하지 않지만 적당한 햇빛은 괜찮아요.";}
            else if (light.contains("낮은 광도(300~800 Lux)") && light.contains("높은 광도(1,500~10,000 Lux)")) {light="햇빛이 필요하지 않지만 때로는 강한 햇빛이 필요해요.";}
            else if (light.contains("중간 광도(800~1,500 Lux)") && light.contains("높은 광도(1,500~10,000 Lux)")) {light="적당한 햇빛 또는 강한 햇빛이 필요해요.";}
            else if (light.contains("낮은 광도(300~800 Lux)")) {light="햇빛이 필요하지 않아요.";}
            else if (light.contains("중간 광도(800~1,500 Lux)")) {light="적당한 햇빛을 유지해주세요.";}
            else if (light.contains("높은 광도(1,500~10,000 Lux)")) {light="아주 강한 햇빛이 필요해요.";}


            /// 온도 코드에서 다른 표현으로 변경
            if (temperature.equals("082001")) {temperature = "10~15℃";}
            else if (temperature.equals("082002")) {temperature = "16~20℃";}
            else if (temperature.equals("082003")) {temperature = "21~25℃";}
            else if (temperature.equals("082004")) {temperature = "26~30℃";}

            /// 습도 코드에서 다른 표현으로 변경
            if (hdCode.equals("083001")) {hdCode = "40%미만의 건조한 환경을 유지해주세요.";}
            else if (hdCode.equals("083002")) {hdCode = "40~70%의 적당한 습도를 유지해주세요.";}
            else if (hdCode.equals("083003")) {hdCode = "70%이상의 습한 환경을 유지해주세요.";}

            // 관리 난이도 코드에서 다른 표현으로 변경
            if (manageLevel.equals("089001")) {manageLevel = "쉬움";}
            else if (manageLevel.equals("089002")) {manageLevel = "보통";}
            else if (manageLevel.equals("089003")) {manageLevel = "어려움";}

            obj.put("commonName", name);
            obj.put("scientificName", sname);
            obj.put("plantPictureUrl", imageURL);
            obj.put("manageLevel",manageLevel );
            obj.put("growthTemperature", temperature);
            obj.put("humidity", hdCode);
            obj.put("fertilizer", fertilizer);
            obj.put("lightDemand", light);
            obj.put("otherInformation", otherInformation);
            obj.put("waterCycle",watercycle);
            obj.put("plantCount",0);
            obj.put("plantCategory",type);
            obj.put("plantPictureUrlS3"," ");

            log.info(String.valueOf(obj.length()));

            objList.put(obj);}

        log.info(String.format("끝: "+ objList.length()));
        return objList;


    }

}
