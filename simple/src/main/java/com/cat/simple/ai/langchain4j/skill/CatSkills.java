package com.cat.simple.ai.langchain4j.skill;

import dev.langchain4j.skills.ClassPathSkillLoader;
import dev.langchain4j.skills.Skill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatSkills {



//    @Bean
//    public Skill emojiSkill(){
//        return Skill.builder()
//                .name("emoji-style")
//                .description("When the user is chatting with the assistant, this skill should be used to add lively expressions to the responses.")
//                .content("""
//                From now on, decorate your replies with emojis:
//
//                ## Style rules
//                - Add 1-2 emojis per paragraph, placed at natural pause points
//                  (end of sentences or after key points), never mid-word
//                - Match emojis to the content:
//                  - Greetings / thanks → 👋 😊 🙏
//                  - Good news / success → 🎉 ✅ 👍
//                  - Questions / thinking → 🤔 ❓ 💭
//                  - Tips / warnings → 💡 ⚠️ 📌
//                  - Code / technical → 💻 ⚙️ 🔧
//                - Keep the text readable: emojis enhance, never replace words
//                - Match the user's language: if the user writes Chinese, use
//                  emojis common in Chinese chat culture (e.g. 哈哈 → 😄, ok → 👌)
//
//                ## Tone
//                - Overall tone becomes friendly and light
//                - One exclamation-worthy point = at most one emoji; avoid spam like
//                  🎉🎉🎉🎉🎉 unless the user explicitly enjoys that
//
//                ## Deactivation
//                - If the user says '别用表情了' / 'stop the emojis', tell them
//                  you can turn it off, and reply in plain text from then on
//                """)
//                .build();
//    }

    @Bean
    public Skill emojiSkill(){
        return ClassPathSkillLoader.loadSkill("skills/emoji-style");
    }


}
