package com.cat.simple.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cat.common.entity.Page;
import com.cat.common.entity.auth.Org;

import com.cat.common.entity.auth.OrgPageParam;
import com.cat.common.entity.auth.OrgTree;
import com.cat.simple.mapper.OrgMapper;
import com.cat.simple.service.OrgService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cat.common.entity.CONSTANTS.NIUBI_ORG_NAME;
import static com.cat.common.entity.CONSTANTS.ORG_PARENT;

@Service
public class OrgServiceImpl implements OrgService {


    @Resource
    private OrgMapper orgMapper;

    @Override
    public boolean add(Org org){
        org.setId(null);
        org.setCreateTime(LocalDateTime.now());
        org.setUpdateTime(LocalDateTime.now());
        org.setDeleted("0");

        if( ( ObjectUtils.isEmpty(org.getParentId()) || ObjectUtils.isEmpty(orgMapper.selectById(org.getParentId())) )  && !org.getParentId().equals(ORG_PARENT) ){
            return false;
        }
        return orgMapper.insert(org) == 1;
    }

    @Override
    public boolean delete(Org org){
        return orgMapper.deleteById(org.getId()) == 1;
    }

    @Override
    public boolean update(Org org){
        Org orgOri = orgMapper.selectById(org.getId());
        org.setCreateTime(orgOri.getCreateTime());
        org.setUpdateTime(LocalDateTime.now());
        org.setDeleted(orgOri.getDeleted());
        return orgMapper.updateById(org) == 1;
    }

    @Override
    public Org info(Org org){
        return  orgMapper.selectById(org.getId());
    }

    @Override
    public Page<Org> queryPage(OrgPageParam pageParam){
        Page<Org> page = new Page<>(pageParam);
        pageParam.init();
        page = orgMapper.selectPage(page, pageParam);
        List<Org> records = page.getRecords();
        records.forEach(r->{
            Integer parentId = r.getParentId();
            if(parentId.equals(ORG_PARENT)){
                r.setParentName(NIUBI_ORG_NAME);
            }else {
                r.setParentName(orgMapper.selectById(parentId).getName());
            }
        });
        return page;
    }

    @Override
    public OrgTree getOrgTree() {

        OrgTree orgTree;
        if(ORG_PARENT > 0 ){
            Org org = orgMapper.selectById(ORG_PARENT);
            orgTree = new OrgTree().setId(ORG_PARENT).setName(org.getName());
        }else {
            orgTree = new OrgTree().setId(ORG_PARENT).setName(NIUBI_ORG_NAME);
        }

        List<Org> orgs = orgMapper.selectList(new QueryWrapper<Org>());
        Map<Integer, List<Org>> collect = orgs.stream().collect(Collectors.groupingBy(Org::getParentId));

        orgTree.setChildren(OrgTree.getChildren(orgTree.getId(), orgTree.getName(), collect));

        return orgTree;
    }







}