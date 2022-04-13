
package com.crio.warmup.stock;


import com.crio.warmup.stock.dto.*;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {

  //  Note:
  //  1. There can be few unused imports, you will need to fix them to make the build pass.
  //  2. You can use "./gradlew build" to check if your code builds successfully.

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
    
    File jsonFile = resolveFileFromResources(args[0]);
    ObjectMapper om = getObjectMapper();
    PortfolioTrade[] pt = om.readValue(jsonFile, PortfolioTrade[].class);
    List<String> stockSymbols = new ArrayList<String>();

    for (PortfolioTrade trade : pt) {
      stockSymbols.add(trade.getSymbol());
    }

    return stockSymbols;
  }

  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(
        Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = 
        "/home/crio-user/workspace/rachana-singh0172-ME_QMONEY_V2/qmoney/bin/main/trades.json";
    String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@2f9f7dcf";
    String functionNameFromTestFileInStackTrace = "mainReadFile";
    String lineNumberFromTestFileInStackTrace = "47";


    return Arrays.asList(new String[]{valueOfArgument0, resultOfResolveFilePathArgs0,
        toStringOfObjectMapper, functionNameFromTestFileInStackTrace,
        lineNumberFromTestFileInStackTrace});
  }

  public static String getToken() {
    return "d7ee5290251fd4882f10fde8ada179ccc1450745";
  }


  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
    List<PortfolioTrade> portfolioStocks = readTradesFromJson(args[0]);

    LocalDate endDate = LocalDate.parse(args[1]);
    final String token = "d7ee5290251fd4882f10fde8ada179ccc1450745";
    ArrayList<TotalReturnsDto> listOfReturns = new ArrayList<TotalReturnsDto>();
    RestTemplate restTemplate = new RestTemplate();

    for (PortfolioTrade stock : portfolioStocks) {
      String api = prepareUrl(stock, endDate, token);
      TiingoCandle[] response = restTemplate.getForObject(api, TiingoCandle[].class);
      TiingoCandle lastResponseCandle = response[response.length - 1];
      TotalReturnsDto returns = new TotalReturnsDto(stock.getSymbol(), 
          lastResponseCandle.getClose());
      listOfReturns.add(returns);
    }

    Collections.sort(listOfReturns, new ClosePriceComparator());

    ArrayList<String> sortedSymbols = new ArrayList<String>();
    for (TotalReturnsDto totalReturn : listOfReturns) {
      sortedSymbols.add(totalReturn.getSymbol());
    }

    return sortedSymbols;
  }

  public static List<PortfolioTrade> readTradesFromJson(String filename) 
      throws IOException, URISyntaxException {
    File jsonFile = resolveFileFromResources(filename);
    ObjectMapper om = getObjectMapper();
    PortfolioTrade[] pt = om.readValue(jsonFile, PortfolioTrade[].class);
    List<PortfolioTrade> stocks = new ArrayList<PortfolioTrade>();

    for (PortfolioTrade trade : pt) {
      stocks.add(trade);
    }
    return stocks;
  }


  public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {
    String uri = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
    return uri.replace("$APIKEY", token).replace("$SYMBOL", trade.getSymbol())
           .replace("$STARTDATE", trade.getPurchaseDate().toString())
           .replace("$ENDDATE", endDate.toString());

  }

  static Double getOpeningPriceOnStartDate(List<Candle> candles) {
    Candle firstCandle = candles.get(0);
    return firstCandle.getOpen();
  }

  public static Double getClosingPriceOnEndDate(List<Candle> candles) {
    Candle lastCandle = candles.get(candles.size() - 1);
    return lastCandle.getClose();
  }

  public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {
    RestTemplate restTemplate = new RestTemplate();
    String url = prepareUrl(trade, endDate, token);
    Candle[] response = restTemplate.getForObject(url, TiingoCandle[].class);
    return Arrays.asList(response);
  }

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
    PortfolioTrade trade, Double buyPrice, Double sellPrice) {
    LocalDate startDate = trade.getPurchaseDate();
    double total_num_years = startDate.until(endDate, ChronoUnit.DAYS)/365.24;
    double totalReturns = (sellPrice - buyPrice) / buyPrice;
    double annualizedReturns = Math.pow((1 + totalReturns), (1.0/total_num_years)) - 1; 
    return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturns);
  }

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
      throws IOException, URISyntaxException {

        List<PortfolioTrade> portfolioStocks = readTradesFromJson(args[0]);
        LocalDate endDate = LocalDate.parse(args[1]);
        List<AnnualizedReturn> listOfAnnualizedReturns = new ArrayList<AnnualizedReturn>();

        for (PortfolioTrade trade : portfolioStocks) {
          List<Candle> candles = fetchCandles(trade, endDate, PortfolioManagerApplication.getToken());
          double buyPrice = getOpeningPriceOnStartDate(candles);
          double sellPrice = getClosingPriceOnEndDate(candles);
          listOfAnnualizedReturns.add(calculateAnnualizedReturns(endDate, trade, buyPrice, sellPrice));
        }

        Collections.sort(listOfAnnualizedReturns, new AnnualizedReturnsComparator());


     return listOfAnnualizedReturns;
  }

  


  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Once you are done with the implementation inside PortfolioManagerImpl and
  //  PortfolioManagerFactory, create PortfolioManager using PortfolioManagerFactory.
  //  Refer to the code from previous modules to get the List<PortfolioTrades> and endDate, and
  //  call the newly implemented method in PortfolioManager to calculate the annualized returns.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
       String file = args[0];
       LocalDate endDate = LocalDate.parse(args[1]);
       File jsonFile = resolveFileFromResources(file);
       ObjectMapper om = getObjectMapper();
       PortfolioTrade[] portfolioTrades = om.readValue(jsonFile, PortfolioTrade[].class);
       RestTemplate restTemplate = new RestTemplate();

       PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(restTemplate);
       return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
  }



  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());
    

    printJsonObject(mainCalculateReturnsAfterRefactor(args));


  }
}

class ClosePriceComparator implements Comparator<TotalReturnsDto> {

  @Override
  public int compare(TotalReturnsDto d1, TotalReturnsDto d2) {
    if (d1.getClosingPrice() > d2.getClosingPrice()) {
      return 1;
    } else if (d1.getClosingPrice() < d2.getClosingPrice()) {
      return -1;
    }
    return 0;
  }

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

