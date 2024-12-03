package com.audora.lotting_be.model.customer;

import jakarta.persistence.Embeddable;
import lombok.Data;

import java.time.LocalDate;

@Embeddable
@Data
public class Votemachine {
    private Boolean machine1; //제1호
    private Boolean machine2_1; //제2-1호
    private Boolean machine2_2; //제2-2호
    private Boolean machine2_3; //제2-3호
    private Boolean machine2_4; //제2-4호
    private Boolean machine3; //제3호
    private Boolean machine4; //제4호
    private Boolean machine5; //제5호
    private Boolean machine6; //제6호
    private Boolean machine7; //제7호
    private Boolean machine8; //제8호
    private Boolean machine9; //제9호
    private Boolean machine10; //제10호
}