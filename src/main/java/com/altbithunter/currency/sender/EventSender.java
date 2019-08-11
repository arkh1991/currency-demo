package com.altbithunter.currency.sender;

import com.altbithunter.dto.EventDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Сервис отправки сообщений о событиях
 */
@Service
public class EventSender {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;


    /**
     * Отправка сообщения.
     * @param event сообщение.
     */
    public void send(EventDto event) {
        try {
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream("events.txt",
                    true), "UTF-8"));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            sdf.format(new Date(event.getCreateTime()));
            pw.println(sdf.format(new Date(event.getCreateTime())) + " - " + event.getDescription());
            pw.close();
            ObjectMapper mapper = new ObjectMapper();
            String message = mapper.writeValueAsString(event);
            kafkaTemplate.send("tg-topic", message);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }


    }
}
