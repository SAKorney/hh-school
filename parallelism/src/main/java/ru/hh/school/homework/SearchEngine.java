package ru.hh.school.homework;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;

public class SearchEngine {
    public static void testSearch() throws IOException {
        System.out.println(naiveSearch("public"));
    }

    public static long naiveSearch(String query) {
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
}
