package Forum.Help.PostSearcher;

import com.sun.org.apache.xpath.internal.operations.Bool;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.io.*;
import java.text.ParseException;
import java.util.*;


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
    Integer ArrayForumSize;
    String[] need_forums_array;
    String[] parts;

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
        //соберем массив из наименований форумов
        parts = property.getProperty("need_forums_array").split(";");
        Integer ArrayForumSize = parts.length;
        need_forums_array = new String[ArrayForumSize];
        for (int i = 0; i < ArrayForumSize; ++i)
        {
            need_forums_array[i] = String.valueOf(parts[i]);
        }
    }
}
public class Main {

    public static void main(String[] args) throws IOException, ParseException {
        // объявим список где будем хранить нужные нам данные об игроках
        List<Player> playerList = new ArrayList<>();
        // запишем данные об агенте
        String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36";

        //dialog windows
        //JOptionPane.showMessageDialog( null, "Скрипт начал работу...", "Подсчет постов", JOptionPane.DEFAULT_OPTION );

        //импорт данных из конфиг.пропертис

        Settings settings = new Settings("src/Forum/Help/PostSearcher/config.properties");
        try {

            //объявляем исходные данные

            File file = new File(settings.filePath);
            BufferedWriter out = new BufferedWriter(new FileWriter(file));


            System.out.println ("============================================================");
            System.out.println ("Считаем посты на форуме: " + settings.FORUM_URL);
            System.out.println ("Сегодняшняя дата = " + settings.t_date);
            System.out.println ("Вчерашняя дата = " + settings.et_date);
            System.out.println ("Дата, после которой начинается отсчет = " + settings.b_date);
            System.out.println ("Дата, до которой идет отсчет = " + settings.e_date);
            System.out.println ("Минимальное количество символов в игровом посте = " + settings.GAME_POST_SIZE);
            System.out.println ("============================================================");

            out.write("============================================================"+ System.getProperty("line.separator"));
            out.write("Считаем посты на форуме: " + settings.FORUM_URL+ System.getProperty("line.separator"));
            out.write("Сегодняшняя дата = " + settings.t_date+ System.getProperty("line.separator"));
            out.write("Вчерашняя дата = " + settings.et_date+ System.getProperty("line.separator"));
            out.write("Дата, после которой начинается отсчет = " + settings.b_date + System.getProperty("line.separator"));
            out.write("Дата, до которой идет отсчет = " + settings.e_date+ System.getProperty("line.separator"));
            out.write("Минимальное количество символов в игровом посте = " + settings.GAME_POST_SIZE+ System.getProperty("line.separator"));
            out.write("============================================================"+ System.getProperty("line.separator"));

            // работа с календарем

            Calendar b_Calendar = stringToDateFormat(settings.b_date);
            Date begin_date = b_Calendar.getTime();
            Calendar e_Calendar = stringToDateFormat(settings.e_date);
            Date end_date = e_Calendar.getTime();
            Calendar t_Calendar = stringToDateFormat(settings.t_date);
            Date today_date = t_Calendar.getTime();
            Calendar et_Calendar = stringToDateFormat(settings.et_date);
            Date etoday_date = et_Calendar.getTime();

            // логинимся и соханяем кукисы впредь
            String login_url = settings.FORUM_URL + "/login.php";

            // АВТОРИЗАЦИЯ РАБОЧАЯ

            Connection.Response res = Jsoup
                    .connect(login_url)
                    .userAgent(USER_AGENT)
                    .timeout(5000)
                    .method(Method.GET)
                    .execute();

            // сохраним куки
            Map<String, String> cookies = res.cookies();

            res = Jsoup
                    .connect(login_url+"?action=in")
                    .timeout(5000)
                    .userAgent(USER_AGENT)
                    .data("req_username", settings.USER_NAME)
                    .data("req_password", settings.PASSWORD)
                    .data("form_sent", "1")
                    .data("redirect_url", "")
                    .cookies(cookies)
                    .method(Method.POST)
                    .execute();
            cookies.putAll(res.cookies());

            //Узнаем сколько у нас страниц с пользователями.
            //сперва придется таки узнать сколько у нас игроков
            res = Jsoup.connect(settings.FORUM_URL)
                    .userAgent(USER_AGENT)
                    .cookies(cookies)
                    .method(Method.GET)
                    .execute();

            cookies.putAll(res.cookies());

            Document doc2 = Jsoup.parse(res.body());
            //проверяем мы ли это
            Element itemElement = doc2.select("#pun-status .item1").first();
            String current_log_name_string = itemElement.text();
            String current_log_name = current_log_name_string.substring(8);

            System.out.println("Вы авторизовались как - " + current_log_name);  // если тут Гость - то авторизация прошла так себе...
            out.write("Вы авторизовались как - " + current_log_name+ System.getProperty("line.separator"));

            // получаем данные
            Element liElement = doc2.select("div.statscon").first().child(0).child(2);
            //нашли число игроков, теперь найдем число страниц в списке игроков.
            String playerNumber = liElement.select("strong").first().text();
            int playerNumberInt = Integer.parseInt(playerNumber);
            // нашли число страниц с пользователями
            int stringsNumber = (( playerNumberInt - (playerNumberInt % settings.USERS_NAME_PAGE_SIZE) )/settings.USERS_NAME_PAGE_SIZE ) + 1;
            System.out.println("Число страниц в списке игроков = " + stringsNumber);
            out.write("Число страниц в списке игроков = " + stringsNumber+ System.getProperty("line.separator"));

            // Добываем имена игроков и ссылки на их профили из Списка пользователей.
            // причем делаем это в цикле для каждой страницы!
            for (int numberOfPlayerPage = 1; numberOfPlayerPage < stringsNumber + 1 ; numberOfPlayerPage++ ){

                res = Jsoup.connect(settings.FORUM_URL + "/userlist.php?show_group=-1&sort_by=last_visit&sort_dir=DESC&username=-&p=" + numberOfPlayerPage)
                        .userAgent(USER_AGENT)
                        .cookies(cookies)
                        .method(Method.GET)
                        .execute();

                Document doc3 = Jsoup.parse(res.body());
                //ищем данные о пользователях
                Elements h2Elements = doc3.select("span.usersname");

                h2Elements.forEach((Element h2Element) -> {
                    //получим имя пользователя
                    String name = h2Element.child(0).text();
                    //получим ссылку на его профиль
                    String url = h2Element.child(0).attr("href");
                    //получим количество сообщений пользователя
                    String kolvo_soobsh_string = h2Element.parent().parent().child(3).text();
                    Integer all_post_number = Integer.parseInt(kolvo_soobsh_string);
                    //добавим эти данные в массив
                    playerList.add(new Player(url, name, all_post_number));
                });
            }

            // Выводим эти данные в консоль
            int size = playerList.size();
            System.out.println("Количество игроков = " + size);
            System.out.println ("============================================================");
            out.write("Количество игроков = " + size+ System.getProperty("line.separator"));
            out.write("============================================================"+ System.getProperty("line.separator"));

            // тут призываем процедуру подсчета постов этих юзеров что у нас в списке
            for ( int j = 0; j < size; j++){
                String author_url = playerList.get(j).getUrl();
                Integer number_of_game_post = 0;

                List<Post> postList = new ArrayList<>();
                Integer number_of_lists = 1;

                // мы имеем не совсем тот адрес на руках. преобразуем. http://yaoi.9bb.ru/profile.php?id=2106 в - http://yaoi.9bb.ru/search.php?action=show_user_posts&user_id=2106
                String main_need_url = author_url.replaceFirst("profile", "search");
                main_need_url = main_need_url.replaceFirst("id=", "action=show_user_posts&user_id="); // http://testunicorn.0pk.ru/search.php?action=show_user_posts&user_id=2&p=2
                // усложним адрес до числа страниц
                int playersNumberOfMessages = playerList.get(j).getKolvoSoobsh();
                System.out.println ("Количество постов y " + playerList.get(j).getName() + " = " + playersNumberOfMessages);
                // количество листов с постами. TODO лагает количество постов! уточнить! потому что люди удаляют свои посты и счетчик playersNumberOfMessages не точный...
                int number_of_post_sheets = (( playersNumberOfMessages - (playersNumberOfMessages % settings.PAGE_SEARCH_SIZE) )/settings.PAGE_SEARCH_SIZE ) + 1;
                System.out.println ("Количество листов постов y " + playerList.get(j).getName() + " = " + number_of_post_sheets);
                // TODO добавить ограничение по датам - чтобы не ходил по ВСЕМ страницам, ибо если дата уже достигнута - дальше постов не будет.
                for (number_of_lists = 1; number_of_lists <= number_of_post_sheets; number_of_lists++) {
                    String need_url = main_need_url + "&p=" + number_of_lists;

                    res = Jsoup.connect(need_url)
                            .userAgent(USER_AGENT)
                            .cookies(cookies)
                            .method(Method.GET)
                            .execute();
                    //собираем данные о постах
                    Document doc4 = Jsoup.parse(res.body());

                    //находим все посты на странице
                    Elements postElements = doc4.select("div.post");
                    //для каждого поста находим данные
                    for (Element postElement : postElements) {
                        //получим имя подфорума
                        String podforum_name = postElement.child(0).child(0).child(1).text();
                        //получим адрес поста
                        String post_url = postElement.child(0).child(0).child(3).attr("href");
                        //получим строку с датой поста
                        String string_date = postElement.child(0).child(0).child(3).text();
                        //получим текст поста
                        String post_text = postElement.child(1).child(1).text();
                        //получим количество символов в посте
                        Integer post_size = post_text.length();
                        Date post_date = null;
                        // Определим дату
                        if(!string_date.contains("Сегодня") && !string_date.contains("Вчера"))  {
                            Calendar post_Calendar = stringToDateFormat(string_date);
                            post_date = post_Calendar.getTime();
                        } else {
                            //если дата содержит слово "Сегодня" то присвоим ей сегодняшнюю дату
                            if (string_date.contains("Сегодня")){
                                post_date = today_date;
                                //если дата содержит слово "Вчера" то присвоим ей вчерашнюю дату
                            } else {
                                if (string_date.contains("Вчера")){
                                    post_date = etoday_date;
                                } else {
                                    post_date = today_date;
                                }
                            }
                        }
                        // сравним пост по дате - вообще попадать ли ему сюда!
                        if (begin_date.before(post_date) && end_date.after(post_date)) {
                            //сравним по размеру поста и по форуму - игровой ли это пост
                            if ((post_size > settings.GAME_POST_SIZE) && Arrays.asList(settings.need_forums_array).contains(podforum_name)) {
                                //увеличим число игровых постов на 1
                                number_of_game_post = number_of_game_post + 1;
                            }
                            //записываем полученные о посте данные в массив постов
                            postList.add(new Post(playerList.get(j).getName(), post_size, string_date, number_of_game_post, podforum_name));  // имя автора можем достать из шапки поста - но зачем оно тут нам?
                        }
                    }
                }
                //тут мы тестово выводим весь список постов. отключим после тестирования.
                postList.forEach(System.out::println);

                // вывод финальных данных
                // TODO хорошо бы добавить сортировку по playerList по number_of_game_post

                int psize = postList.size();
                System.out.println ("Количество постов у игрока * " + playerList.get(j).getName() + " * равно * " + psize + " * Количество игровых постов  равно * " + number_of_game_post);
                out.write("Количество постов у игрока * " + playerList.get(j).getName() + " * равно * " + psize + " * Количество игровых постов  равно * " + number_of_game_post+ System.getProperty("line.separator"));
            }

            System.out.println ("============================================================");
            System.out.println ("Список игроков закончен.");
            System.out.println ("============================================================");

            out.write("============================================================"+ System.getProperty("line.separator"));
            out.write("Список игроков закончен."+ System.getProperty("line.separator"));
            out.write("============================================================"+ System.getProperty("line.separator"));

            JOptionPane.showMessageDialog( null, "Скрипт закончил работу. Проверьте файл с результатами...", "Конец", JOptionPane.DEFAULT_OPTION );
            out.close();

        } catch (IOException e) {
            System.err.println("Ошибка: Нет файла конфигурации...");
        }
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

class Post {
    private String author_name;
    private Integer post_size;
    private String post_date;
    private Integer number_of_game_post;
    private String podforum_name;

    Post (String authorName, Integer razmerPosta, String post_date, Integer number_of_game_post, String podforum_name) {
        this.author_name = authorName;
        this.post_size = razmerPosta;
        this.number_of_game_post = number_of_game_post;
        this.post_date = post_date;
        this.podforum_name = podforum_name;

    }

    @Override
    public String toString() {
        return "Пост {" +
                "Автор = '" + author_name + '\'' +
                ", Размер поста = " + post_size +
                ", Дата поста = '" + post_date + '\'' +
                ", Количество игровых постов = " + number_of_game_post +
                ", на подфоруме = '" + podforum_name + '\'' +
                '}';
    }
}

class Player {
    private String url;
    private String name;
    private Integer all_post_number;

    Player(String url, String name, Integer all_post_number) {
        this.url = url;
        this.name = name;
        this.all_post_number = all_post_number;
    }

    public String getUrl() {
        return url;
    }

    public Integer getKolvoSoobsh() {
        return all_post_number;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return " Осмотрен игрок " + name +
                ", всего у него сообщений = '" + all_post_number + '\'' +
                " с адресом '" + url + '\'';
    }
}
