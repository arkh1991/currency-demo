package com.altbithunter;

import com.altbithunter.currency.service.CurrencyService;
import com.altbithunter.domain.vo.KandleVO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CurrencyServiceTest {

    @Autowired
    CurrencyService currencyService;


    @Test
    public void uniteTest() {
        KandleVO kandle1 = new KandleVO();
        KandleVO kandle2 = new KandleVO();
        KandleVO kandle3 = new KandleVO();

        kandle1.setOpen(3);
        kandle1.setClose(5);
        kandle1.setHigh(33);
        kandle1.setLow(-1);
        kandle1.setQuoteAssetVolume(3);
        kandle1.setBaseAssetVolume(4);
        kandle1.setVolume(2);
        kandle1.setTrades(1);
        kandle1.setQuoteVolume(12);

        kandle2.setOpen(5);
        kandle2.setClose(12);
        kandle2.setHigh(13);
        kandle2.setLow(8);
        kandle2.setQuoteAssetVolume(2);
        kandle2.setBaseAssetVolume(2);
        kandle2.setVolume(12);
        kandle2.setTrades(2);
        kandle2.setQuoteVolume(3);


        kandle3.setOpen(12);
        kandle3.setClose(6);
        kandle3.setHigh(12);
        kandle3.setLow(-14);
        kandle3.setQuoteAssetVolume(2);
        kandle3.setBaseAssetVolume(2);
        kandle3.setVolume(1);
        kandle3.setTrades(1);
        kandle3.setQuoteVolume(1);

        KandleVO res = currencyService.uniteKandles(Arrays.asList(kandle1, kandle2, kandle3).toArray(new KandleVO[3]));

        Assert.assertEquals(3, res.getOpen(), 0.00001);
        Assert.assertEquals(6, res.getClose(), 0.00001);
        Assert.assertEquals(33, res.getHigh(), 0.00001);
        Assert.assertEquals(-14, res.getLow(), 0.00001);
        Assert.assertEquals(7, res.getQuoteAssetVolume(), 0.00001);
        Assert.assertEquals(8, res.getBaseAssetVolume(), 0.00001);
        Assert.assertEquals(15, res.getVolume(), 0.00001);
        Assert.assertEquals(4, res.getTrades(), 0.00001);
        Assert.assertEquals(16, res.getQuoteVolume(), 0.00001);
    }
}
