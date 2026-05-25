package com.cat.simple.process.service;


import com.cat.common.entity.DTO;
import com.cat.common.entity.Page;
import com.cat.common.entity.PageParam;
import com.cat.common.entity.process.ProcessDefinition;
import com.cat.common.entity.process.ProcessDefinitionBytearray;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;

public interface ProcessDefinitionService {

    boolean add(ProcessDefinition processDefinition) throws ParserConfigurationException, IOException, SAXException;

    boolean save(ProcessDefinition processDefinition) throws ParserConfigurationException, IOException, SAXException;

    DTO<?> deploy(Integer id);

    boolean delete(ProcessDefinition processDefinition);

    boolean destroy(ProcessDefinition processDefinition);

    boolean stop(ProcessDefinition processDefinition);

    ProcessDefinition info(ProcessDefinition processDefinition);

    ProcessDefinition info(ProcessDefinition processDefinition, String version);

    Page<ProcessDefinition> queryPage(PageParam pageParam);

    List<ProcessDefinition> deployList();

    List<ProcessDefinitionBytearray> versionList(Integer processDefinitionId);

    boolean rollback(Integer processDefinitionId, String targetVersion);
}
