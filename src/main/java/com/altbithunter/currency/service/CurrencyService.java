package com.altbithunter.currency.service;

import com.altbithunter.currency.sender.EventSender;
import com.altbithunter.domain.repository.CurrencyRepository;
import com.altbithunter.domain.repository.KandleRepository;
import com.altbithunter.domain.vo.CurrencyVO;
import com.altbithunter.domain.vo.KandleVO;
import com.altbithunter.dto.EventDto;
import com.altbithunter.dto.KandleDto;
import com.altbithunter.utils.EventType;
import com.altbithunter.utils.IntervalType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Сервис работы со парами.
 */
@Component
@Slf4j
public class CurrencyService {
    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private KandleRepository kandleRepository;

    @Autowired
    private KandleService kandleService;

    @Autowired
    private EventSender eventSender;

    /**
     * Максимальное количество данных, хранимое по одной паре.
     */
    @Value("${kandle.interval.size}")
    private int intervalSize;

    /**
     * Минимальный размер данных для анализа объёмов.
     */
    private int MIN_VOLUME_INTERVAL = 10;

    /**
     * Получить последнюю актуалальную свечу с полностью заполненными данными
     * @param dtos список свечей.
     * @return
     */
    private KandleDto getTargetKandle(List<KandleDto> dtos) {

        int lastIdx = 0;
        for (int i = 1; i < dtos.size(); i++) {
            if (dtos.get(i).getCloseTime() > dtos.get(lastIdx).getCloseTime()) {
                lastIdx = i;
            }
        }

        if (lastIdx == 0) {
            if (dtos.get(lastIdx).getCloseTime() - dtos.get(lastIdx).getOpenTime()
                    != dtos.get(lastIdx + 1).getCloseTime() - dtos.get(lastIdx + 1).getOpenTime()) {

                return dtos.get(lastIdx + 1);

            } else {

                return dtos.get(lastIdx);
            }
        } else {
            if (dtos.get(lastIdx).getCloseTime() - dtos.get(lastIdx).getOpenTime()
                    != dtos.get(lastIdx - 1).getCloseTime() - dtos.get(lastIdx - 1).getOpenTime()) {

                return dtos.get(lastIdx - 1);

            } else {

                return dtos.get(lastIdx);

            }
        }
    }

    /**
     * Поиск событий связанных с объёмом.
     * @param kandles свечи.
     */
    public void proccessVolumeKandles(List<KandleDto> kandles) {

        if (kandles.size() < MIN_VOLUME_INTERVAL) {
            return;
        }

        KandleDto dto = getTargetKandle(kandles);
        log.info("try to getEvent :{}", dto);
        EventDto event = kandleService.getEventForKandle(dto, kandles);

        if (!EventType.NONE.equals(event.getEventType())) {
            log.info("event sended :{}", event);
            eventSender.send(event);
        }
    }


    /**
     * Обработка очередной свечи (сохранение в БД)
     * @param kandleDto свеча.
     */
    public void proccessKandle(KandleDto kandleDto) {

        CurrencyVO currencyVO = currencyRepository.getByPairName(kandleDto.getStringPairName());
        if (currencyVO == null) {
            currencyVO = new CurrencyVO(kandleDto.getStringPairName(), kandleDto.getSource());
            currencyRepository.save(currencyVO);
        }

        KandleVO kandleVO = new KandleVO(kandleDto, currencyVO.getCurrencyGuid());
        List<KandleVO> exist = kandleRepository.getAllByCurrencyGuidAndOpenTime(currencyVO.getCurrencyGuid(),
                kandleDto.getOpenTime());

        if (exist != null && exist.size() == 0) {
            kandleRepository.save(kandleVO);
        }
    }

    /**
     * Оптимизация свечей в БД.
     */
    public void kandlesOptimization() {

        List<CurrencyVO> currencyVOS = currencyRepository.findAll();
        for (CurrencyVO currency : currencyVOS) {
            removeOldKandles(currency.getCurrencyGuid());
        }

    }

    /**
     * Удаление старых свечей.
     * @param currencyGuid - guid пары.
     */
    private void removeOldKandles(UUID currencyGuid) {

        List<KandleVO> kandles = kandleRepository.getAllByCurrencyGuidOrderByOpenTimeAsc(currencyGuid);
        if (kandles.size() > intervalSize) {
            for (int i = 0; i < kandles.size() - intervalSize; i++) {
                kandleRepository.delete(kandles.get(i));
            }
        }

    }

    /**
     * Оптимизация свечей (объединение)
     * @param currencyGuid guid монеты.
     */
    private void currencyKandleOptimization(UUID currencyGuid) {

        for (IntervalType interval : IntervalType.values()) {
            optimizeInterval(currencyGuid, interval);
        }

    }

    /**
     * Оптимизация свечей определённого интервала.
     * @param currencyGuid guid монеты
     * @param interval интервал
     */
    private void optimizeInterval(UUID currencyGuid, IntervalType interval) {

        List<KandleVO> listForOptimization = kandleRepository
                .getAllByCurrencyGuidAndInteval(currencyGuid, interval.getMsValue() - 1);
        if (listForOptimization.size() <= intervalSize) {
            return;
        }

        // объединения происходят по парам, кроме интервалов в 1 день и 5 минут.
        int uniteSize = 2;
        if (interval.equals(IntervalType.INTERVAL_1D) || interval.equals(IntervalType.INTERVAL_5M)) {
            uniteSize = 3;
        }


        for (int i = listForOptimization.size() - 1; i >= intervalSize; i -= uniteSize) {

            KandleVO[] uniteSet = new KandleVO[uniteSize];

            for (int j = uniteSize - 1; j >= 0; j--) {
                uniteSet[j] = listForOptimization.get(i - j);
            }
            KandleVO newKandle = uniteKandles(uniteSet);
            kandleRepository.save(newKandle);

            for (KandleVO old : uniteSet) {
                kandleRepository.delete(old);
            }

        }
    }

    /**
     * Объединение свечей.
     * @param vos свечи для объединения
     * @return результирующая свеча
     */
    public KandleVO uniteKandles(KandleVO[] vos) {

        KandleVO newVo = new KandleVO();
        newVo.setOpen(vos[0].getOpen());
        newVo.setClose(vos[vos.length - 1].getClose());
        newVo.setOpenTime(vos[0].getOpenTime());
        newVo.setCloseTime(vos[vos.length - 1].getCloseTime());
        newVo.setCurrencyGuid(vos[0].getCurrencyGuid());

        double baseAssetVolume = 0;
        double quoteVolume = 0;
        int trades = 0;
        double volume = 0;
        double quoteAssetVolume = 0;

        for (int i = 0; i < vos.length; i++) {
            baseAssetVolume += vos[i].getBaseAssetVolume();
            quoteVolume += vos[i].getQuoteVolume();
            trades += vos[i].getTrades();
            volume += vos[i].getVolume();
            quoteAssetVolume += vos[i].getQuoteAssetVolume();
        }

        newVo.setQuoteAssetVolume(quoteAssetVolume);
        newVo.setBaseAssetVolume(baseAssetVolume);
        newVo.setQuoteVolume(quoteVolume);
        newVo.setTrades(trades);
        newVo.setVolume(volume);

        double high = vos[0].getHigh();
        double low = vos[0].getLow();

        for (int i = 0; i < vos.length; i++) {
            high = Math.max(high, vos[i].getHigh());
            low = Math.min(low, vos[i].getLow());
        }

        newVo.setHigh(high);
        newVo.setLow(low);

        return newVo;

    }
}
