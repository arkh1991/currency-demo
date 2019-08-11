package com.altbithunter.currency.receiver;

import com.altbithunter.currency.service.CurrencyService;
import com.altbithunter.dto.KandleDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;


/**
 * Принимает сообщения из service по образованию.
 */
@Service
@Slf4j
public class VolumeMessageReceiver {


    @Autowired
    CurrencyService currencyService;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(VolumeMessageReceiver.class);

    private CountDownLatch latch = new CountDownLatch(1);

    public CountDownLatch getLatch() {
        return latch;
    }

    @KafkaListener(topics = "volume-topic")
    public void receive(@Payload String payload) {
        LOGGER.info("received volume payload='{}'", payload);
        ObjectMapper mapper = new ObjectMapper();
        try {
            List<KandleDto> dtos = mapper.readValue(payload, new TypeReference<List<KandleDto>>() {
            });

            currencyService.proccessVolumeKandles(dtos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        latch.countDown();
    }
}