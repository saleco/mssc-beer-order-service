package guru.sfg.beer.order.service.services.testcomponents;

import guru.sfg.beer.order.service.config.JmsConfig;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
import guru.sfg.brewery.model.events.AllocateOrderResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * Created by jt on 2/15/20.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class BeerOrderAllocationListener {

    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void list(Message msg){

        boolean allocationError = false;
        boolean pendingInventory = false;
        boolean sendResponse = true;

        AllocateOrderRequest request = (AllocateOrderRequest) msg.getPayload();

        //condition to partial allocation
        if("partial-allocation".equals(request.getBeerOrderDto().getCustomerRef())) {
            pendingInventory = true;
        }

        //condition to fail allocation
        if("fail-allocation".equals(request.getBeerOrderDto().getCustomerRef())) {
            allocationError = true;
        } else if("dont-allocate".equals(request.getBeerOrderDto().getCustomerRef())) {
            sendResponse = false;
        }


        boolean finalPendingInventory = pendingInventory;

        request.getBeerOrderDto().getBeerOrderLines().forEach(beerOrderLineDto -> {
            if(finalPendingInventory) {
                beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity() - 1);
            } else {
                beerOrderLineDto.setQuantityAllocated(beerOrderLineDto.getOrderQuantity());
            }
        });

        if(sendResponse) {

            jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE,
                    AllocateOrderResult.builder()
                            .beerOrderDto(request.getBeerOrderDto())
                            .allocationError(allocationError)
                            .pendingInventory(pendingInventory)
                            .build());

        }

    }
}
