
/** ${tableComment} */
export interface ${tableNameUp} {

<#list fieldInfos as field>
    /** ${field.comment!''} */
    ${field.fieldName}<#if !field.keyFlag>?</#if>:${field.tsType};
</#list>

}

export interface ${tableNameUp}PageParam {
    search?: string;
    current: number;
    size: number;
}