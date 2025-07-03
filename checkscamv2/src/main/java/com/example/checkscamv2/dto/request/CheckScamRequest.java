package com.example.checkscamv2.dto.request;

import com.example.checkscamv2.constant.ScamInfoType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class CheckScamRequest {
    private String info;
    private Integer type;

    public ScamInfoType getType() {
        return ScamInfoType.parse(type);
    }
}
