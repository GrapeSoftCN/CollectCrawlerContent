package Test;

import httpServer.booter;
import nlogger.nlogger;

public class TestCrawlerContent {
    public static void main(String[] args) {
        booter booter = new booter();
        try {
            System.out.println("CrawlerContent");
            System.setProperty("AppName", "CrawlerContent");
            booter.start(1006);
        } catch (Exception e) {
            nlogger.logout(e);
        }
    }
}
