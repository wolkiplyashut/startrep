package Forum.Help.PostSearcher;

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

public class Main {

    private static String USER_AGENT =
            "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36";

    public static void main(String[] args) throws IOException, ParseException {
        // объявим список где будем хранить нужные нам данные об игроках
        List<Player> playerList = new ArrayList<>();
        // запишем данные об агенте

        //импорт данных из конфиг.пропертис

        Settings settings = new Settings("src/Forum/Help/PostSearcher/config.properties");
        try {
            print_settings(settings, new OutputStreamWriter(System.out));

            File file = new File(settings.filePath);
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            print_settings(settings, out);


            // логинимся и соханяем кукисы впредь


            // АВТОРИЗАЦИЯ РАБОЧАЯ

            Connection.Response res = Jsoup
                    .connect(settings.login_url)
                    .userAgent(USER_AGENT)
                    .timeout(5000)
                    .method(Method.GET)
                    .execute();

            // сохраним куки
            Map<String, String> cookies = res.cookies();

            res = Jsoup
                    .connect(settings.login_url+"?action=in")
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
                    //String kolvo_soobsh_string = h2Element.parent().parent().child(3).text();
                    //Integer all_post_number = Integer.parseInt(kolvo_soobsh_string);
                    //добавим эти данные в массив
                    playerList.add(new Player(url, name, 0));
                });
            }

            // Выводим эти данные в консоль

            System.out.println("Количество игроков = " + playerList.size());
            System.out.println ("============================================================");
            out.write("Количество игроков = " + playerList.size()+ System.getProperty("line.separator"));
            out.write("============================================================"+ System.getProperty("line.separator"));

            // тут призываем процедуру подсчета постов этих юзеров что у нас в списке
            for (Player player: playerList){

                List<Post> postList = getPlayerPosts(player, settings, cookies, out);
                //тут мы тестово выводим весь список постов. отключим после тестирования.
                postList.forEach(System.out::println);

                // вывод финальных данных
                // TODO хорошо бы добавить сортировку по playerList по number_of_game_post

            }

            System.out.println ("============================================================");
            System.out.println ("Список игроков закончен.");
            System.out.println ("============================================================");

            out.write("============================================================"+ System.getProperty("line.separator"));
            out.write("Список игроков закончен."+ System.getProperty("line.separator"));
            out.write("============================================================"+ System.getProperty("line.separator"));

            JOptionPane.showMessageDialog( null, "Скрипт закончил работу. Проверьте файл с результатами...", "Конец", JOptionPane.INFORMATION_MESSAGE );
            out.close();

        } catch (IOException e) {
            System.err.println("Ошибка: Нет файла конфигурации...");
        }
    }


    private  static List<Post> getPlayerPosts(Player player, Settings settings, Map<String, String> cookies, Writer out) throws IOException {
        String author_url = player.getUrl();
        Integer number_of_game_post = 0;
        List<Post> postList = new ArrayList<>();

        // мы имеем не совсем тот адрес на руках. преобразуем. http://yaoi.9bb.ru/profile.php?id=2106 в - http://yaoi.9bb.ru/search.php?action=show_user_posts&user_id=2106
        String main_need_url = author_url.replaceFirst("profile", "search");
        main_need_url = main_need_url.replaceFirst("id=", "action=show_user_posts&user_id="); // http://testunicorn.0pk.ru/search.php?action=show_user_posts&user_id=2&p=2
        //найдем число листов с постами
        String url_first_player_page = main_need_url + "&p=1";
        Connection.Response res1 = Jsoup.connect(url_first_player_page)
                .userAgent(USER_AGENT)
                .cookies(cookies)
                .method(Method.GET)
                .execute();

        Document doc5 = Jsoup.parse(res1.body());
        int number_of_post_sheets = 1;
        Element pageLinkElement = doc5.select("div.linkst").first();
        if(pageLinkElement != null) {
            Elements numberOfPageElements = pageLinkElement.select("a[href]");
            if (numberOfPageElements.size() > 0) {
                int[] mas_of_string_pages = new int[numberOfPageElements.size()];
                int i = 0;
                for (Element numberOfLastPageElement : numberOfPageElements) {
                    if (!(numberOfLastPageElement.text().equals("«")) && !(numberOfLastPageElement.text().equals("»"))) {
                        mas_of_string_pages[i] = Integer.parseInt(numberOfLastPageElement.text());
                    }
                    if (i < numberOfPageElements.size() - 1) {
                        i++;
                    }
                }
                int maxString = mas_of_string_pages[0];
                for (i = 0; i < mas_of_string_pages.length; i++) {
                    if (mas_of_string_pages[i] > maxString)
                        maxString = mas_of_string_pages[i];
                }
                number_of_post_sheets = maxString;
            }
        }
        System.out.println ("Количество листов постов y " + player.getName() + " = " + number_of_post_sheets);
        // TODO добавить ограничение по датам - чтобы не ходил по ВСЕМ страницам, ибо если дата уже достигнута - дальше постов не будет.
        for (int number_of_lists = 1; number_of_lists <= number_of_post_sheets; number_of_lists++) {
            String need_url = main_need_url + "&p=" + number_of_lists;

            Connection.Response res = Jsoup.connect(need_url)
                    .userAgent(USER_AGENT)
                    .cookies(cookies)
                    .method(Method.GET)
                    .execute();
            //собираем данные о постах
            Document doc4 = Jsoup.parse(res.body());

            //находим все посты на странице
            Elements postElements = doc4.select("div.post");
            if (postElements.size() > 0) {
                //для каждого поста находим данные
                for (Element postElement : postElements) {
                    //получим имя подфорума
                    String podforum_name = postElement.child(0).child(0).child(1).text();
                    //получим адрес поста
                    String post_url = postElement.child(0).child(0).child(3).attr("href");
                    //получим строку с датой поста
                    String string_post_date = postElement.child(0).child(0).child(3).text();
                    //получим текст поста
                    String post_text = postElement.child(1).child(1).text();
                    //получим количество символов в посте
                    Integer post_size = post_text.length();
                    Date post_date = null;
                    // Определим дату
                    if (string_post_date.contains("Сегодня")) {
                        post_date = settings.today_date;
                    } else if (string_post_date.contains("Вчера")) {
                        post_date = settings.etoday_date;
                    } else {
                        Calendar post_Calendar = Settings.stringToDateFormat(string_post_date);
                        post_date = post_Calendar.getTime();
                    }


                    // сравним пост по дате - вообще попадать ли ему сюда!
                    if (settings.begin_date.before(post_date) && post_date.before(settings.end_date)) {
                        //сравним по размеру поста и по форуму - игровой ли это пост
                        if ((post_size > settings.GAME_POST_SIZE) && Arrays.asList(settings.need_forums_array).contains(podforum_name)) {
                            //увеличим число игровых постов на 1
                            number_of_game_post = number_of_game_post + 1;
                        }
                        //записываем полученные о посте данные в массив постов
                        Forum.Help.PostSearcher.Post post = new Forum.Help.PostSearcher.Post(player.getName(), post_size, string_post_date, number_of_game_post, podforum_name);
                        postList.add(post);  // имя автора можем достать из шапки поста - но зачем оно тут нам?
                    }
                }
            }
        }

        int psize = postList.size();
        System.out.println ("Количество постов у игрока * " + player.getName() + " * равно * " + psize + " * Количество игровых постов  равно * " + number_of_game_post);
        out.write("Количество постов у игрока * " + player.getName() + " * равно * " + psize + " * Количество игровых постов  равно * " + number_of_game_post+ System.getProperty("line.separator"));

        return postList;
    }
    private static void print_settings(Settings settings, Writer out) throws IOException {

        out.write("============================================================"+ System.getProperty("line.separator"));
        out.write("Считаем посты на форуме: " + settings.FORUM_URL+ System.getProperty("line.separator"));
        out.write("Сегодняшняя дата = " + settings.t_date+ System.getProperty("line.separator"));
        out.write("Вчерашняя дата = " + settings.et_date+ System.getProperty("line.separator"));
        out.write("Дата, после которой начинается отсчет = " + settings.b_date + System.getProperty("line.separator"));
        out.write("Дата, до которой идет отсчет = " + settings.e_date+ System.getProperty("line.separator"));
        out.write("Минимальное количество символов в игровом посте = " + settings.GAME_POST_SIZE+ System.getProperty("line.separator"));
        out.write("============================================================"+ System.getProperty("line.separator"));
    }

}

