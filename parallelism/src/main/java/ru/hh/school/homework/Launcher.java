package ru.hh.school.homework;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import static java.util.Collections.reverseOrder;
import static java.util.Map.Entry.comparingByValue;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

public class Launcher {
  private static final int ONLY_DIR_WITHOUT_SUB = 1;
  private static final int TOP = 10;
  public static void main(String[] args) throws IOException, InterruptedException {
    // Написать код, который, как можно более параллельно:
    // - по заданному пути найдет все "*.java" файлы
    // - для каждого файла вычислит 10 самых популярных слов (см. #naiveCount())
    // - соберет top 10 для каждой папки в которой есть хотя-бы один java файл
    // - для каждого слова сходит в гугл и вернет количество результатов по нему (см. #naiveSearch())
    // - распечатает в консоль результаты в виде:
    // <папка1> - <слово #1> - <кол-во результатов в гугле>
    // <папка1> - <слово #2> - <кол-во результатов в гугле>
    // ...
    // <папка1> - <слово #10> - <кол-во результатов в гугле>
    // <папка2> - <слово #1> - <кол-во результатов в гугле>
    // <папка2> - <слово #2> - <кол-во результатов в гугле>
    // ...
    // <папка2> - <слово #10> - <кол-во результатов в гугле>
    // ...
    //
    // Порядок результатов в консоли не обязательный.
    // При желании naiveSearch и naiveCount можно оптимизировать.

    int numOfTHreads = Runtime.getRuntime().availableProcessors();
    ExecutorService executor = Executors.newFixedThreadPool(numOfTHreads);
    Path path = Paths.get("").toAbsolutePath();

    launch(path, Launcher::naiveSearch, executor);

    executor.shutdown();
  }

  private static void launch(Path path,  Function<String, Long> search, ExecutorService executor) throws IOException {
    try (Stream<Path> paths = Files.walk(path)) {
      paths
              .parallel()
              .filter(Files::isDirectory)
              .map(p -> processDirectory(p, executor))
              .filter(info -> !info.getValue().isEmpty())
              .forEach(q -> processQueries(q, search, executor));
    }
  }

  private static Map.Entry<Path, List<String>> processDirectory(Path path, ExecutorService executor) {
    try {
      return Map.entry(path, calcStat(path, executor));
    }
    catch (IOException ignored) {
      return Map.entry(path, List.of());
    }
  }

  private static List<String> calcStat(Path path,  ExecutorService executor) throws IOException {
    return Files.walk(path, ONLY_DIR_WITHOUT_SUB)
            .filter(Launcher::isJavaFile)
//            .map(Launcher::naiveCount)
//            Было сомнение, следует ли делать чтение с диска асинхронным.
//            Всё-таки это едичный ресурс, который слабо умеет параллелится.
            .map((file) -> CompletableFuture.supplyAsync(() -> naiveCount(file), executor))
            .map(CompletableFuture::join)
            .flatMap(map -> map.entrySet().stream())
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    Long::sum
            ))
            .entrySet()
            .stream()
            .sorted(comparingByValue(reverseOrder()))
            .limit(TOP)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
  }

  private static boolean isJavaFile(Path path) {
    return Files.isRegularFile(path) && path.getFileName().toString().endsWith("java");
  }

  private static void processQueries(Map.Entry<Path, List<String>> info, Function<String, Long> search, ExecutorService executor) {
    var path = info.getKey();
    var queries = info.getValue();
    var output = queries.stream()
            .map(q -> CompletableFuture.supplyAsync(() -> Map.entry(q, search.apply(q)), executor))
            .map(CompletableFuture::join)
            .map(res -> formatOutput(path, res))
            .collect(Collectors.joining("\n"));
    // Вывод ускорится, если сразу выводить на консоль.
    // Однако, вывод будет без группировки по директориям
    // т.к. будет осуществляться для тех слов, которые были
    // обработаны раньше других.
    System.out.println(output);
  }

  private static String formatOutput(Path path, Map.Entry<String, Long> result) {
    return String.join(" - ", path.toString(), result.getKey(), result.getValue().toString());
  }

  private static void testCount() {
    Path path = Path.of("d:\\projects\\work\\hh-school\\parallelism\\src\\main\\java\\ru\\hh\\school\\parallelism\\Runner.java");
    System.out.println(naiveCount(path));
  }

  private static Map<String, Long> naiveCount(Path path) {
    try {
      return Files.lines(path)
        .flatMap(line -> Stream.of(line.split("[^a-zA-Z0-9]")))
        .filter(word -> word.length() > 3)
        .collect(groupingBy(identity(), counting()))
        .entrySet()
        .stream()
        .sorted(comparingByValue(reverseOrder()))
        .limit(TOP)
        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void testSearch() throws IOException {
    System.out.println(naiveSearch("public"));
  }

  private static long naiveSearch(String query) {
    try {
      Document document = Jsoup //
              .connect("https://www.google.com/search?q=" + query) //
              .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.116 Safari/537.36") //
              .get();

      Element divResultStats = document.select("div#slim_appbar").first();
      String text = divResultStats.text();
      String resultsPart = text.substring(0, text.indexOf('('));
      return Long.parseLong(resultsPart.replaceAll("[^0-9]", ""));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static final Random rnd = new Random();
  private static long mockSearch(String query) {
    try {
      Thread.sleep(1000);
      return rnd.nextLong(500_000);
    }
    catch (InterruptedException ignored) {
      return 0;
    }
  }
}
