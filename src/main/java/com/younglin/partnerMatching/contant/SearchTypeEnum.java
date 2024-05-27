package com.younglin.partnerMatching.contant;

public enum SearchTypeEnum {

    USERTYPE("user","用户"),
    TEAMTYPE("team","队伍");


    private String value;

    private String text;

    SearchTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    public String getValue() {
        return value;
    }


    public String getText() {
        return text;
    }

}
