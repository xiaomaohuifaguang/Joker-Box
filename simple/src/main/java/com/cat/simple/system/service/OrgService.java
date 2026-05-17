package com.cat.simple.system.service;


import com.cat.common.entity.Page;
import com.cat.common.entity.auth.Org;
import com.cat.common.entity.auth.OrgPageParam;
import com.cat.common.entity.auth.OrgTree;

public interface OrgService {

    boolean add(Org org);

    boolean delete(Org org);

    boolean update(Org org);

    Org info(Org org);

    Page<Org> queryPage(OrgPageParam pageParam);

    OrgTree getOrgTree();
}