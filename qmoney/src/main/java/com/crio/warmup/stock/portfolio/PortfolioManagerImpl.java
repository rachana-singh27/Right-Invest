
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.PortfolioManagerApplication;
import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {




  private RestTemplate restTemplate;

  // Caution: Do not delete or modify the constructor, or else your build will
  // break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  //TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  //    Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  //    into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  //    clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  //CHECKSTYLE:OFF
  static Double getOpeningPriceOnStartDate(List<Candle> candles) {
    Candle firstCandle = candles.get(0);
    return firstCandle.getOpen();
  }

  public static Double getClosingPriceOnEndDate(List<Candle> candles) {
    Candle lastCandle = candles.get(candles.size() - 1);
    return lastCandle.getClose();
  }

  
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades, LocalDate endDate)
      throws JsonProcessingException {

    List<AnnualizedReturn> listOfAnnualizedReturns = new ArrayList<AnnualizedReturn>();

    for (PortfolioTrade trade : portfolioTrades) {
      List<Candle> candles = getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
      double buyPrice = getOpeningPriceOnStartDate(candles);
      double sellPrice = getClosingPriceOnEndDate(candles);
      LocalDate startDate = trade.getPurchaseDate();
      double total_num_years = startDate.until(endDate, ChronoUnit.DAYS)/365.24;
      double totalReturns = (sellPrice - buyPrice) / buyPrice;
      double annualizedReturn = Math.pow((1 + totalReturns), (1.0/total_num_years)) - 1; 
      listOfAnnualizedReturns.add(new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturns));
    }

    Collections.sort(listOfAnnualizedReturns, new AnnualizedReturnsComparator());

    for(AnnualizedReturn ar : listOfAnnualizedReturns) {
      System.out.println(ar.getSymbol() + " " + ar.getAnnualizedReturn());
    }

    return listOfAnnualizedReturns;

  }




  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  class AnnualizedReturnsComparator implements Comparator<AnnualizedReturn> {

    @Override
    public int compare(AnnualizedReturn a1, AnnualizedReturn a2) {
      if (a1.getAnnualizedReturn() < a2.getAnnualizedReturn()) {
        return 1;
      } else if (a1.getAnnualizedReturn() > a2.getAnnualizedReturn()) {
        return -1;
      }
      return 0;
  
    }
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {
        String url = buildUri(symbol, from, to);
        Candle[] response = restTemplate.getForObject(url, TiingoCandle[].class);
        return Arrays.asList(response);
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    String uri = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
    return uri.replace("$APIKEY", PortfolioManagerApplication.getToken()).replace("$SYMBOL", symbol)
           .replace("$STARTDATE", startDate.toString())
           .replace("$ENDDATE", endDate.toString());

  }
}
