package com.altbithunter.currency.service;


import com.altbithunter.domain.repository.CurrencyRepository;
import com.altbithunter.domain.repository.KandleRepository;
import com.altbithunter.domain.vo.CurrencyVO;
import com.altbithunter.dto.EventDto;
import com.altbithunter.dto.KandleDto;
import com.altbithunter.utils.EventType;
import com.altbithunter.utils.IntervalType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Сервис работы со свечами.
 */
@Component
@Slf4j
public class KandleService {


    @Autowired
    private CurrencyRepository currencyRepository;


    /**
     * коэффикицент, при котором объём будет считаться крайне большим.
     */
    @Value("${kandle.event.excessive.rate}")
    private double excessiveRate;

    /**
     * коэфициент превышения длины тела свечи хвостом продаж .
     */
    @Value("${kandle.event.excessive.tale.sale.rate}")
    private double saleRate;

    /**
     * коэфициент превышения длины тела свечи хвостом покупок.
     */
    @Value("${kandle.event.excessive.tale.buy.rate}")
    private double buyRate;


    /**
     * коэффикицент, при котором объём будет считаться высоким.
     */
    @Value("${kandle.event.excessive.exceed}")
    private int exceed;


    /**
     * Обнаружение событий для свечи.
     * @param kandleDto свеча, для которой определяются события
     * @param previousKandles предыдущие свечи
     * @return событие
     */
    public EventDto getEventForKandle(KandleDto kandleDto, List<KandleDto> previousKandles) {

        CurrencyVO vo = currencyRepository.getByPairName(kandleDto.getStringPairName());

        if (vo == null) {
            vo = new CurrencyVO();
            vo.setCurrencyGuid(UUID.randomUUID());
            vo.setSource(kandleDto.getSource());
            vo.setPairName(kandleDto.getStringPairName());
            currencyRepository.save(vo);
        }

        CurrencyVO currency = currencyRepository.getByPairName(kandleDto.getStringPairName());
        EventDto event = getExcessiveEvent(kandleDto, previousKandles);
        IntervalType currType = null;

        for (IntervalType intervalType : IntervalType.values()) {
            if ((long) intervalType.getMsValue() == kandleDto.getCloseTime() - kandleDto.getOpenTime() + 1) {
                currType = intervalType;
            }
        }

        event.setIntervalType(currType);
        event.setSourceCurrency(currency.getCurrencyGuid());

        return event;
    }

    /**
     * Обнаружение события связанного с высоким объёмом
     * @param dto свеча, для которой определяются события
     * @param previousKandles предыдущие свечи
     * @return событие
     */
    private EventDto getExcessiveEvent(KandleDto dto, List<KandleDto> previousKandles) {

        EventDto event = new EventDto();
        double totalVolume = 0;
        int i = 0;
        int interval = previousKandles.size() - 2;

        for (KandleDto kandle : previousKandles) {

            if (kandle == dto) {
                continue;
            }

            if (i < interval && kandle.getVolume() * exceed > dto.getVolume()) {
                log.debug("founded bigger volume on prev. actual:{}, prev:{}", dto.getVolume(), kandle.getVolume());
                return event;
            } else {

                if (i < interval) {
                    log.debug("ok, dtoVolume: {} kandle:{}, can be excessive ", dto.getVolume(), kandle);
                }

            }

            totalVolume += kandle.getVolume();
            i++;
        }

        double average = totalVolume / previousKandles.size();

        if (dto.getVolume() > average * excessiveRate) {
            log.debug("ok, dtoVolume: {} average:{}, is excessive ", dto.getVolume(), average);
            event.setEventType(EventType.EXCESSIVE_VOLUME);
            event.setDescription(getExcessiveDescription(dto));
        } else {
            log.debug("kandleVolume not enough: {} average: {} ", dto.getVolume(), average);

        }

        return event;
    }

    /**
     * Получение текста описания для события превышения объёма.
     * @param dto свеча, для которой обнаржуено событие.
     * @return текстовое описание.
     */
    private String getExcessiveDescription(KandleDto dto) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String description = dto.getStringPairName() + " " + sdf.format(new Date(dto.getOpenTime())) + " - высокий объём";
        double kandleBodyLength = Math.abs(dto.getOpen() - dto.getClose());

        if (dto.getOpen() > dto.getClose()) {
            description += " на баре продаж.";
            double saleTale = dto.getClose() - dto.getLow();

            if (saleTale > saleRate * kandleBodyLength) {
                description += " Свеча не полная.";
            }

        } else {
            description += " на баре покупок.";
            double buyTale = dto.getHigh() - dto.getClose();

            if (buyTale > buyRate * kandleBodyLength) {
                description += " Свеча не полная.";
            }

        }

        return description;

    }

}
