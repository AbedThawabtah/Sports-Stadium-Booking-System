package org.example.sportstadiumbookingsystem.dto.review;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewModerationRequest {

    // اختياري: سبب الرفض/الحذف من قبل الأدمن
    private String reason;
}