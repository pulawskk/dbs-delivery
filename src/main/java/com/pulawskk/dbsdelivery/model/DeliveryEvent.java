package com.pulawskk.dbsdelivery.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
public class DeliveryEvent implements Serializable {
    private static final Long serialVersionUID = 4675673123L;

    private String orderId;
    private String deliveryId;
    private String orderStatus;
    private String burgersAmount;
    private String estimatedTimeOfArrival;
}
