package Forum.Help.PostSearcher;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;

class Settings {
    String filePath;
    String b_date;
    String e_date;
    String t_date;
    String et_date;
    String FORUM_URL;
    Integer GAME_POST_SIZE;
    Integer USERS_NAME_PAGE_SIZE;
    Integer PAGE_SEARCH_SIZE;
    String USER_NAME;
    String PASSWORD;
    String[] need_forums_array;
    String login_url;

    Date begin_date;
    Date end_date;
    Date today_date;
    Date etoday_date;

    Settings(String filename) throws IOException {

        Properties property = new Properties();
        property.load(new FileInputStream(filename));

        filePath =  property.getProperty("filePath");
        b_date = property.getProperty("b_date");
        e_date = property.getProperty("e_date");
        t_date = property.getProperty("t_date");
        et_date = property.getProperty("et_date");
        FORUM_URL = property.getProperty("FORUM_URL");
        GAME_POST_SIZE = Integer.parseInt(property.getProperty("GAME_POST_SIZE"));   // длина поста, который считается игровым!  ТОЖЕ ВАЖНО
        USERS_NAME_PAGE_SIZE = Integer.parseInt(property.getProperty("USERS_NAME_PAGE_SIZE")); // количество игроков в списке игроков
        PAGE_SEARCH_SIZE = Integer.parseInt(property.getProperty("PAGE_SEARCH_SIZE")); // число постов на странице поиска, 20 на фиаре, 30 - на охоте! надо будет это учесть!
        USER_NAME = property.getProperty("USER_NAME");
        PASSWORD = property.getProperty("PASSWORD");
        //соберем массив из наименований подфорумов
        String[] parts = property.getProperty("need_forums_array").split(";");
        need_forums_array = new String[parts.length];
        for (int i = 0; i < parts.length; ++i)
        {
            need_forums_array[i] = String.valueOf(parts[i]);
        }

        login_url = FORUM_URL + "/login.php";

        // работа с календарем

        Calendar b_Calendar = stringToDateFormat(b_date);
        begin_date = b_Calendar.getTime();

        Calendar e_Calendar = stringToDateFormat(e_date);
        end_date = e_Calendar.getTime();

        Calendar t_Calendar = stringToDateFormat(t_date);
        today_date = t_Calendar.getTime();

        Calendar et_Calendar = stringToDateFormat(et_date);
        etoday_date = et_Calendar.getTime();

    }

    public static Calendar stringToDateFormat(String stringDate){

        String need_year_string = stringDate.substring(0,4);
        String need_month_string = stringDate.substring(5,7);
        String need_day_string = stringDate.substring(8,10);
        int Date_year = Integer.parseInt(need_year_string);
        int Date_month = Integer.parseInt(need_month_string);
        int Date_day = Integer.parseInt(need_day_string);
        Calendar need_calendar = new GregorianCalendar(Date_year, Date_month-1, Date_day);
        return need_calendar;
    }

}
