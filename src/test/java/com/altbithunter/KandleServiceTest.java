package com.altbithunter;

import com.altbithunter.currency.service.KandleService;
import com.altbithunter.domain.repository.CurrencyRepository;
import com.altbithunter.dto.EventDto;
import com.altbithunter.dto.KandleDto;
import com.altbithunter.utils.EventType;
import com.altbithunter.utils.IntervalType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class KandleServiceTest {

    @Autowired
    KandleService kandleService;


    @Autowired
    CurrencyRepository repository;

    @After
    public void tearDown() {
        repository.deleteAll();
    }

    @Test
    public void getEventTest() {

        List<KandleDto> kandles = new ArrayList<>();
        Long current = System.currentTimeMillis();

        for (int i = 0; i < 15; i++) {

            KandleDto kandleDto = new KandleDto();
            kandleDto.setStringPairName("BTCUSDT");
            kandleDto.setVolume(2);
            kandleDto.setOpenTime(current);
            kandleDto.setCloseTime(current + IntervalType.INTERVAL_4H.getMsValue() - 1);
            kandles.add(kandleDto);
        }

        KandleDto dto = new KandleDto();
        dto.setStringPairName("BTCUSDT");
        dto.setVolume(25);
        dto.setOpenTime(current);
        dto.setCloseTime(current + IntervalType.INTERVAL_4H.getMsValue() - 1);

        EventDto edto = kandleService.getEventForKandle(dto, kandles);
        Assert.assertEquals(EventType.EXCESSIVE_VOLUME, edto.getEventType());

        kandles.get(6).setVolume(10);
        edto = kandleService.getEventForKandle(dto, kandles);
        Assert.assertEquals(EventType.NONE, edto.getEventType());

    }
}
