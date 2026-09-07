package com.cat.simple.ai.langchain4j.skill;

import dev.langchain4j.skills.DefaultSkill;
import dev.langchain4j.skills.Skill;
import dev.langchain4j.skills.Skills;
import org.springframework.stereotype.Component;

@Component
public class SkillsBuilder {



    public Skills makeSkills(Skill... skill){
        return Skills.builder().skills(skill).build();
    }

}
