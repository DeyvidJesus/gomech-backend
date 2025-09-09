package com.gomech.dto.ServiceOrder;

import com.gomech.model.ServiceOrderStatus;
import lombok.Data;

@Data
public class UpdateStatusDTO {
    private ServiceOrderStatus status;
}
