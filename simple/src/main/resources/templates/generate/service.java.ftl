package ${servicePackage};


import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import ${entityPackage}.${tableNameUp};

public interface ${tableNameUp}Service {

    boolean add(${tableNameUp} ${tableNameDown});

    boolean delete(${tableNameUp} ${tableNameDown});

    boolean update(${tableNameUp} ${tableNameDown});

    ${tableNameUp} info(${tableNameUp} ${tableNameDown});

    Page<${tableNameUp}> queryPage(PageParam pageParam);
}