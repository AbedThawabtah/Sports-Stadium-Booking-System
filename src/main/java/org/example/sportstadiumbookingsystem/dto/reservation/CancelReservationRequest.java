package org.example.sportstadiumbookingsystem.dto.reservation;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelReservationRequest {

    // اختياري: الزبون ممكن يشرح سبب الإلغاء
    private String reason;
}