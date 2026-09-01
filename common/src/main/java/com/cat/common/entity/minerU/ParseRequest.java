package com.cat.common.entity.minerU;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.web.multipart.MultipartFile;

import java.io.Serial;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "ParseRequest", description = "minerU文件解析请求参数")
public class ParseRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "文件集合")
    MultipartFile[] files;

    /**
     * pipeline: More general, supports multiple languages, hallucination-free.
     * vlm-engine: High accuracy via local computing power, supports Chinese and English documents only.
     * vlm-http-client: High accuracy via remote computing power(client suitable for openai-compatible servers), supports Chinese and English documents only.
     * hybrid-engine: Hybrid parsing via local computing power, supports multiple languages. Use effort to switch medium/high behavior.
     * hybrid-http-client: Hybrid parsing via remote computing power but requires a little local computing power(client suitable for openai-compatible servers), supports multiple languages. Use effort to switch medium/high behavior.
     */
    @Schema(description = "处理引擎")
    String backend = "vlm-engine";

    /**
     * ch: Chinese, English, Japanese, Chinese Traditional, Latin.
     * ch_server: Chinese, English, Japanese, Chinese Traditional, Latin.
     * korean: Korean, English.
     * ta: Tamil, English.
     * te: Telugu, English.
     * ka: Kannada.
     * th: Thai, English.
     * el: Greek, English.
     * arabic: Arabic, Persian, Uyghur, Urdu, Pashto, Kurdish, Sindhi, Balochi, English.
     * east_slavic: Russian, Belarusian, Ukrainian, English.
     * cyrillic: Russian, Belarusian, Ukrainian, Serbian (Cyrillic), Bulgarian, Mongolian, Abkhazian, Adyghe, Kabardian, Avar, Dargin, Ingush, Chechen, Lak, Lezgin, Tabasaran, Kazakh, Kyrgyz, Tajik, Macedonian, Tatar, Chuvash, Bashkir, Malian, Moldovan, Udmurt, Komi, Ossetian, Buryat, Kalmyk, Tuvan, Sakha, Karakalpak, English.
     * devanagari: Hindi, Marathi, Nepali, Bihari, Maithili, Angika, Bhojpuri, Magahi, Santali, Newari, Konkani, Sanskrit, Haryanvi, English.
     */
    @Schema(description = "语言列表")
    String lang_list = "ch";


    /**
     * medium: Faster parsing for most documents, balancing accuracy and efficiency. Image/chart analysis is disabled.
     * high: Higher-accuracy parsing with image/chart analysis support, which may take longer.
     */
    String effort = "medium";


    /**
     * auto: Automatically determine the method based on the file type
     * txt: Use text extraction method
     * ocr: Use OCR method for image-based PDFs
     */
    String parse_method = "auto";


    /**
     * Enable formula parsing.
     */
    Boolean formula_enable = true;

    /**
     * Enable table parsing.
     */
    Boolean table_enable = true;


    /**
     * Enable image/chart analysis for VLM and hybrid backends. Hybrid medium effort automatically disables image/chart analysis.
     */
    Boolean image_analysis = true;

    /**
     * Return markdown content in response
     */
    Boolean return_md = true;

    /**
     * Return middle JSON in response
     */
    Boolean return_middle_json = false;

    /**
     * Return model output JSON in response
     */
    Boolean return_model_output = false;

    /**
     * Return content list JSON in response
     */
    Boolean return_content_list = false;


    /**
     * Return extracted images in response
     */
    Boolean return_images = true;


    /**
     * Return results as a ZIP file instead of JSON
     */
    Boolean response_format_zip = false;

    /**
     * Include the processed original input file in the ZIP result; ignored unless response_format_zip=true
     */
    Boolean return_original_file = false;

    /**
     * The ending page for PDF parsing, beginning from 0
     */
    Integer end_page_id = 99999;



}
