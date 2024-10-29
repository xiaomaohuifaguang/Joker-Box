package com.cat.common.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@Schema(name = "Cascade", description = "级联")
public class Cascade implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String value;

    private String label;

    private List<Cascade> children;


}
