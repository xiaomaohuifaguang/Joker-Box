package com.cat.common.entity.process.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum SearchType {

    DRAFT("0","草稿"),
    TODO("1","待办"),
    DID("2","已办")
    ;




    private final String type;

    private final String description;



}
