package com.younglin.partnerMatching.contant;

public enum ChatTypeEnum {

    PRIVATE_CHAT(0,"私聊"),
    TEAM_CHAT(1,"队伍聊天");


    private Integer value;

    private String text;

    ChatTypeEnum(Integer value, String text) {
        this.value = value;
        this.text = text;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
