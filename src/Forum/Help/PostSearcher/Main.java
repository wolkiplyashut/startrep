package Forum.Help.PostSearcher;


import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException, ParseException {
        // объявим список где будем хранить нужные нам данные об игроках
        List<Player> playerList = new ArrayList<>();

        // даты ИСКЛЮЧИТЕЛЬНЫЕ, то есть эти даты не будут обраатываться. Будут только те что между ними.
        String b_date = "2017-10-29";  // НАЧАЛО
        String e_date = "2017-11-06";  // КОНЕЦ
        String t_date = "2017-11-06";  // СЕГОДНЯ

        String FORUM_URL = "http://wildhunted.rusff.ru";
        Integer GAME_POST_SIZE = 1000;   // длина поста, который считается игровым!  ТОЖЕ ВАЖНО
        Integer USERS_NAME_PAGE_SIZE = 50; // количество игроков в списке игроков
        Integer PAGE_SEARCH_SIZE = 30; // число постов на странице поиска, 20 на фиаре, 30 - на охоте! надо будет это учесть!
        String USER_NAME = "Arthur Grant";
        String PASSWORD = "nasa1313";
        String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36";

        //TODO сделать имена форумов массивом или списком... потому что их количество может быть разное. сравнивать неудобно там ниже...
        String need_forum1 = "au";
        String need_forum2 = "storyline";
        String need_forum3 = "real time";
        String need_forum4 = "flash";
        String need_forum5 = "abandoned episodes";
        String need_forum6 = "closed episodes";


        System.out.println ("============================================================");
        System.out.println ("Считаем посты на форуме: " + FORUM_URL);
        System.out.println ("Сегодняшняя дата = " + t_date);
        System.out.println ("Дата, после которой начинается отсчет = " + b_date);
        System.out.println ("Дата, до которой идет отсчет = " + e_date);
        System.out.println ("Минимальное количество символов в игровом посте = " + GAME_POST_SIZE);
        System.out.println ("============================================================");

        // работа с календарем

        String b_year_string = b_date.substring(0,4);
        String b_month_string = b_date.substring(5,7);
        String b_day_string = b_date.substring(8);

        int b_year = Integer.parseInt(b_year_string);
        int b_month = Integer.parseInt(b_month_string);
        int b_day = Integer.parseInt(b_day_string);

        String e_year_string = e_date.substring(0,4);
        String e_month_string = e_date.substring(5,7);
        String e_day_string = e_date.substring(8);

        int e_year = Integer.parseInt(e_year_string);
        int e_month = Integer.parseInt(e_month_string);
        int e_day = Integer.parseInt(e_day_string);

        String t_year_string = t_date.substring(0,4);
        String t_month_string = t_date.substring(5,7);
        String t_day_string = t_date.substring(8);

        int t_year = Integer.parseInt(t_year_string);
        int t_month = Integer.parseInt(t_month_string);
        int t_day = Integer.parseInt(t_day_string);

        Calendar b_calendar = new GregorianCalendar(b_year, b_month-1, b_day);
        Calendar e_calendar = new GregorianCalendar(e_year, e_month-1, e_day);
        Calendar t_calendar = new GregorianCalendar(t_year, t_month-1, t_day);

        Date begin_date = b_calendar.getTime();
        Date end_date = e_calendar.getTime();
        Date today_date = t_calendar.getTime();

        // добавим контроль над потоком
        Thread mainThread = Thread.currentThread();

        // логинимся и соханяем кукисы впредь
        String login_url = FORUM_URL + "/login.php";

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
                .data("req_username", USER_NAME)
                .data("req_password", PASSWORD)
                .data("form_sent", "1")
                .data("redirect_url", "")
                .cookies(cookies)
                .method(Method.POST)
                .execute();
        cookies.putAll(res.cookies());
        //выведем результат
        System.out.println("Успешная авторизация: " + res.statusCode());

        //Узнаем сколько у нас страниц с пользователями.
        //сперва придется таки узнать сколько у нас игроков
        res = Jsoup.connect(FORUM_URL)
                .userAgent(USER_AGENT)
                .cookies(cookies)
                .method(Method.GET)
                .execute();

        cookies.putAll(res.cookies());

        Document doc2 = Jsoup.parse(res.body());
        //проверяем мы ли это
        Element statusElement = doc2.getElementById("pun-status");
        Element itemElement = statusElement.getElementsByClass("item1").first();
        String current_log_name_string = itemElement.text();
        String current_log_name = current_log_name_string.substring(8);
        System.out.println("Вы авторизовались как - " + current_log_name);  // если тут Гость - то авторизация прошла так себе...

        // получаем данные
        Element statElement = doc2.getElementsByAttributeValue("class", "statscon").first();
        Element conElement = statElement.child(0);
        Element liElement = conElement.child(2);
        Element strongElement = liElement.getElementsByTag("strong").first();
        String playerNumber = strongElement.text();
        System.out.println("Число игроков = " + playerNumber);
        //нашли число игроков, теперь найдем число страниц в списке игроков.
        int playerNumberInt = Integer.parseInt(playerNumber);
        // нашли число страниц с пользователями
        int stringsNumber = (( playerNumberInt - (playerNumberInt % USERS_NAME_PAGE_SIZE) )/USERS_NAME_PAGE_SIZE ) + 1;
        System.out.println("Число страниц в списке игроков = " + stringsNumber);

        // Добываем имена игроков и ссылки на их профили из Списка пользователей.
        // причем делаем это в цикле дя каждой страницы!
        for (int k = 1; k < stringsNumber + 1 ; k++ ){

            res = Jsoup.connect(FORUM_URL + "/userlist.php?show_group=-1&sort_by=last_visit&sort_dir=DESC&username=-&p=" + k)
                    .userAgent(USER_AGENT)
                    .cookies(cookies)
                    .method(Method.GET)
                    .execute();

            //cookies = res.cookies();

            Document doc3 = Jsoup.parse(res.body());
            Elements h2Elements = doc3.getElementsByAttributeValue("class", "usersname");
            h2Elements.forEach((Element h2Element) -> {
                String name = h2Element.child(0).text();
                String url = h2Element.child(0).attr("href");
                Element tc1Element = h2Element.parent();
                Element altElement = tc1Element.parent();
                Element soobshElement = altElement.child(3);
                String kolvo_soobsh_string = soobshElement.text();
                Integer all_post_number = Integer.parseInt(kolvo_soobsh_string);
                playerList.add(new Player(url, name, all_post_number));
            });
        }


        // Выводим эти данные в консоль
        int size = playerList.size();
        System.out.println("Количество игроков = " + size);

        // тут призываем процедуру подсчета постов этих юзеров что у нас в списке
        for ( int j = 0; j < size; j++){
            String author_url = playerList.get(j).getUrl();
            Integer number_of_game_post = 0;


            List<Post> postList = new ArrayList<>();
            Integer number_of_lists = 1;
            Boolean enough = false;

                // мы имеем не совсем тот адрес на руках. преобразуем. http://yaoi.9bb.ru/profile.php?id=2106 в - http://yaoi.9bb.ru/search.php?action=show_user_posts&user_id=2106
                String main_need_url = author_url.replaceFirst("profile", "search");
                main_need_url = main_need_url.replaceFirst("id=", "action=show_user_posts&user_id="); // http://testunicorn.0pk.ru/search.php?action=show_user_posts&user_id=2&p=2
                // усложним адрес до числа страниц
                int rolvo = playerList.get(j).getKolvoSoobsh();
                // количество листов с постами. именно изза этой строчки нам так важна авторизация полностью на форуме с правами админа
                int number_of_post_sheets = (( rolvo - (rolvo % PAGE_SEARCH_SIZE) )/PAGE_SEARCH_SIZE ) + 1;

                for (number_of_lists = 1; number_of_lists < number_of_post_sheets; number_of_lists++) {
                    String need_url = main_need_url + "&p=" + number_of_lists;

                    res = Jsoup.connect(need_url)
                            .userAgent(USER_AGENT)
                            .cookies(cookies)
                            .method(Method.GET)
                            .execute();

                    //cookies = res.cookies();

                    Document doc4 = Jsoup.parse(res.body());

                    Elements postElements = doc4.getElementsByAttributeValue("class", "post");

                    for (Element postElement : postElements) {
                        Element h3Element = postElement.child(0);
                        Element spanElement = h3Element.child(0);
                        String podforum_name = spanElement.child(1).text();
                        // тестовое значение
                        String post_url = spanElement.child(3).attr("href");
                        String string_date = spanElement.child(3).text();
                        Element contElement = postElement.child(1);
                        Element postbodyElement = contElement.child(1);
                        String post_text = postbodyElement.text();
                        Integer post_size = post_text.length();
                        Date post_date = null;
                        // разберемся с датой и проверки добавим на сегодня-вчера
                        if(string_date.length() == 19) {
                            String post_year_string = string_date.substring(0, 4);
                            String post_month_string = string_date.substring(5, 7);
                            String post_day_string = string_date.substring(8, 10);

                            int post_year = Integer.parseInt(post_year_string);
                            int post_month = Integer.parseInt(post_month_string);
                            int post_day = Integer.parseInt(post_day_string);

                            Calendar post_calendar = new GregorianCalendar(post_year, post_month - 1, post_day);
                            post_date = post_calendar.getTime();

                        } else {
                            int index1 = string_date.indexOf("Сегодня");
                            int index2 = string_date.indexOf("Вчера");
                            if (index1 != -1){
                                Calendar post_calendar = new GregorianCalendar(t_year, t_month - 1, t_day);
                                post_date = post_calendar.getTime();
                            } else {
                                if (index2 != -1){
                                    Calendar post_calendar = new GregorianCalendar(t_year, t_month - 1, t_day);
                                    post_calendar.add(Calendar.DAY_OF_YEAR, -1);
                                    post_date = post_calendar.getTime();
                                } else {
                                    Calendar post_calendar = new GregorianCalendar(t_year, t_month - 1, t_day);
                                    post_date = post_calendar.getTime();
                                }
                            }
                        }

                        if (begin_date.before(post_date) && end_date.after(post_date)) {    // сравним пост по дате - вообще попадать ли ему сюда!
                            if (post_size > GAME_POST_SIZE && (podforum_name.compareTo(need_forum1) == 0 ||  podforum_name.compareTo(need_forum2) == 0 ||  podforum_name.compareTo(need_forum3) == 0 ||  podforum_name.compareTo(need_forum4) == 0 || podforum_name.compareTo(need_forum5) == 0 || podforum_name.compareTo(need_forum6) == 0 )) {
                                number_of_game_post = number_of_game_post + 1;   //условный размер игрового поста
                            }
                            postList.add(new Post(playerList.get(j).getName(), post_size, string_date, number_of_game_post, podforum_name));  // имя автора можем достать из шапки поста - но зачем оно тут нам?
                        }
                    }
                }
            //тут мы тестово выводим весь список постов. пока что отключим.

            postList.forEach(System.out::println);

            // вывод финальных данных
            int psize = postList.size();
            System.out.println ("------------------------------------------------------------");
            System.out.println ("Количество постов у игрока " + playerList.get(j).getName() + " = " + psize);
            System.out.println ("Количество игровых постов у игрока " + playerList.get(j).getName() + " = " + number_of_game_post);
            System.out.println ("============================================================");

        }

        System.out.println ("============================================================");
        System.out.println ("Список игроков закончен.");
        System.out.println ("============================================================");
    }

    public static void checkElement(String name, Element elem) {
        if (elem == null) {
            throw new RuntimeException("Не вышло найти: " + name);
        }
    }

    // вот тут попытаемся считать число постов. хотя потом переделаем формат скорее всего
    public static void getPosts(String player_url) throws IOException {

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

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return " Осмотрен игрок " + name +
                ", всего у него сообщений = '" + all_post_number + '\'' +
                " с адресом '" + url + '\'';
    }
}
