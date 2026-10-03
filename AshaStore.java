```java
import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import java.io.*;

public class AshaStore extends MIDlet implements CommandListener, Runnable {

    private Display display;

    // Главные экраны UI
    private List mainMenuList;
    private List catalogList;
    private Form searchForm;
    private Form detailForm;
    private Form settingsForm;
    private Form aboutForm;
    private Form debugForm;

    // Элементы управления
    private TextField searchInput;
    private StringItem statusLabel;
    private StringItem appTitleLabel;
    private StringItem appDescLabel;
    private StringItem appUrlLabel;
    private StringItem debugText;

    // Команды навигации
    private Command exitCmd;
    private Command backCmd;
    private Command searchCmd;
    private Command executeSearchCmd;
    private Command downloadCmd;
    private Command settingsCmd;
    private Command aboutCmd;
    private Command debugCmd;
    private Command refreshCmd;

    // Переменные состояния и данных
    private String baseUrl;
    private String currentCatalogUrl;
    private boolean currentIsSearch;

    private String selectedDownloadUrl;

    private String[] itemUrls;
    private String[] itemNames;
    private int itemCount;

    private boolean isInitialized = false;

    public AshaStore() {
        // Пустой конструктор
    }

    protected void startApp() {
        if (display == null) {
            display = Display.getDisplay(this);
        }

        if (!isInitialized) {
            try {
                baseUrl = "http://series40.kiev.ua/";

                itemUrls = new String[40];
                itemNames = new String[40];
                itemCount = 0;

                currentCatalogUrl = null;
                currentIsSearch = false;
                selectedDownloadUrl = null;

                initCommands();
                initMainMenu();
                initSearchForm();
                initDetailForm();
                initSettingsForm();
                initAboutForm();
                initDebugForm();

                isInitialized = true;

                display.setCurrent(mainMenuList);

            } catch (Throwable t) {
                showFatalError(t);
            }
        }
    }

    public void run() {
        // Резервный метод
    }

    // ---------------------------------------------------------
    // Команды
    // ---------------------------------------------------------

    private void initCommands() {
        exitCmd = new Command("Выход", Command.EXIT, 1);
        backCmd = new Command("Назад", Command.BACK, 1);

        searchCmd = new Command("Поиск", Command.SCREEN, 2);
        executeSearchCmd = new Command("Искать", Command.OK, 1);

        downloadCmd = new Command("Скачать", Command.SCREEN, 1);

        settingsCmd = new Command("Настройки", Command.SCREEN, 3);
        aboutCmd = new Command("О программе", Command.SCREEN, 4);
        debugCmd = new Command("Логи", Command.SCREEN, 5);

        refreshCmd = new Command("Обновить", Command.SCREEN, 2);
    }

    // ---------------------------------------------------------
    // Главное меню
    // ---------------------------------------------------------

    private void initMainMenu() {
        mainMenuList = new List("Asha Store", List.IMPLICIT);

        mainMenuList.append("Каталог приложений", null);
        mainMenuList.append("Игры", null);
        mainMenuList.append("Темы", null);
        mainMenuList.append("Новинки", null);
        mainMenuList.append("Поиск", null);
        mainMenuList.append("Настройки", null);
        mainMenuList.append("О программе", null);
        mainMenuList.append("Системный лог", null);

        mainMenuList.addCommand(exitCmd);
        mainMenuList.addCommand(searchCmd);
        mainMenuList.addCommand(settingsCmd);
        mainMenuList.addCommand(aboutCmd);

        mainMenuList.setCommandListener(this);
    }

    // ---------------------------------------------------------
    // Поиск
    // ---------------------------------------------------------

    private void initSearchForm() {
        searchForm = new Form("Поиск приложений");

        searchInput = new TextField(
            "Поиск:",
            "",
            32,
            TextField.ANY
        );

        searchForm.append(searchInput);

        searchForm.addCommand(executeSearchCmd);
        searchForm.addCommand(backCmd);

        searchForm.setCommandListener(this);
    }

    // ---------------------------------------------------------
    // Информация о приложении
    // ---------------------------------------------------------

    private void initDetailForm() {
        detailForm = new Form("Программа");

        statusLabel = new StringItem(
            "Статус: ",
            "Готово"
        );

        appTitleLabel = new StringItem(
            "Название: ",
            ""
        );

        appDescLabel = new StringItem(
            "Описание: ",
            ""
        );

        appUrlLabel = new StringItem(
            "Ссылка: ",
            ""
        );

        detailForm.append(statusLabel);
        detailForm.append(appTitleLabel);
        detailForm.append(appDescLabel);
        detailForm.append(appUrlLabel);

        detailForm.addCommand(downloadCmd);
        detailForm.addCommand(backCmd);

        detailForm.setCommandListener(this);
    }

    // ---------------------------------------------------------
    // Настройки
    // ---------------------------------------------------------

    private void initSettingsForm() {
        settingsForm = new Form("Настройки");

        settingsForm.append(
            new StringItem("Сервер: ", baseUrl)
        );

        settingsForm.append(
            new StringItem(
                "Платформа: ",
                "Nokia Asha Platform"
            )
        );

        settingsForm.addCommand(backCmd);
        settingsForm.setCommandListener(this);
    }

    // ---------------------------------------------------------
    // О программе
    // ---------------------------------------------------------

    private void initAboutForm() {
        aboutForm = new Form("О программе");

        StringBuffer aboutText = new StringBuffer();

        aboutText.append(
            "Магазин приложений для Nokia Asha"
        );

        aboutText.append(
            "\nВерсия: 1.1.1"
        );

        aboutText.append(
            "\nПлатформа: MIDP 2.1 / CLDC 1.1"
        );

        aboutForm.append(
            new StringItem(
                "Asha Store",
                aboutText.toString()
            )
        );

        aboutForm.addCommand(backCmd);
        aboutForm.setCommandListener(this);
    }

    // ---------------------------------------------------------
    // Отладчик
    // ---------------------------------------------------------

    private void initDebugForm() {
        debugForm = new Form("Отладчик");

        debugText = new StringItem(
            "Статус: ",
            "Система инициализирована.\n"
        );

        debugForm.append(debugText);

        debugForm.addCommand(backCmd);
        debugForm.setCommandListener(this);
    }

    // ---------------------------------------------------------
    // Лог
    // ---------------------------------------------------------

    private void appendLog(final String msg) {
        if (display == null) {
            return;
        }

        display.callSerially(new Runnable() {
            public void run() {
                if (debugText != null) {
                    StringBuffer sb = new StringBuffer();

                    String oldText = debugText.getText();

                    if (oldText != null) {
                        sb.append(oldText);
                    }

                    sb.append("\n> ");
                    sb.append(msg);

                    debugText.setText(
                        sb.toString()
                    );
                }
            }
        });
    }

    // ---------------------------------------------------------
    // Загрузка страницы
    // ---------------------------------------------------------

    private void loadUrlData(
        final String requestUrl,
        final boolean isSearch
    ) {
        currentCatalogUrl = requestUrl;
        currentIsSearch = isSearch;

        catalogList = new List(
            "Загрузка...",
            List.IMPLICIT
        );

        catalogList.addCommand(backCmd);
        catalogList.addCommand(searchCmd);
        catalogList.addCommand(refreshCmd);

        catalogList.setCommandListener(this);

        display.setCurrent(catalogList);

        appendLog(
            "Запрос: " + requestUrl
        );

        new Thread(new Runnable() {
            public void run() {

                HttpConnection conn = null;
                InputStream is = null;

                try {
                    appendLog("Открытие соединения...");

                    conn = (HttpConnection)
                        Connector.open(requestUrl);

                    conn.setRequestMethod(
                        HttpConnection.GET
                    );

                    conn.setRequestProperty(
                        "User-Agent",
                        "NokiaAsha311/14.06"
                    );

                    conn.setRequestProperty(
                        "Accept",
                        "text/html"
                    );

                    int responseCode =
                        conn.getResponseCode();

                    appendLog(
                        "HTTP: " + responseCode
                    );

                    if (responseCode ==
                        HttpConnection.HTTP_OK) {

                        is = conn.openInputStream();

                        StringBuffer sb =
                            new StringBuffer();

                        int ch;
                        int readLimit = 0;

                        while (
                            (ch = is.read()) != -1 &&
                            readLimit < 16000
                        ) {
                            sb.append((char) ch);
                            readLimit++;
                        }

                        final String responseData =
                            sb.toString();

                        display.callSerially(
                            new Runnable() {
                                public void run() {
                                    parseData(
                                        responseData,
                                        isSearch
                                    );
                                }
                            }
                        );

                    } else {

                        final String errCode =
                            "Ошибка сервера: " +
                            responseCode;

                        display.callSerially(
                            new Runnable() {
                                public void run() {
                                    showErrorScreen(
                                        errCode
                                    );
                                }
                            }
                        );
                    }

                } catch (Throwable t) {

                    final String errMsg =
                        "Ошибка сети: " +
                        (
                            t.getMessage() != null
                            ? t.getMessage()
                            : t.getClass().getName()
                        );

                    display.callSerially(
                        new Runnable() {
                            public void run() {
                                showErrorScreen(
                                    errMsg
                                );
                            }
                        }
                    );

                } finally {

                    try {
                        if (is != null) {
                            is.close();
                        }

                        if (conn != null) {
                            conn.close();
                        }

                    } catch (Exception e) {
                        // Игнорируем ошибку закрытия
                    }
                }
            }
        }).start();
    }

    // ---------------------------------------------------------
    // Разбор HTML
    // ---------------------------------------------------------

    private void parseData(
        String data,
        boolean isSearch
    ) {
        if (catalogList == null) {
            return;
        }

        catalogList.deleteAll();

        catalogList.setTitle(
            isSearch
            ? "Результаты поиска"
            : "Каталог"
        );

        itemCount = 0;

        if (data == null || data.length() == 0) {
            catalogList.append(
                "Ничего не найдено",
                null
            );
            return;
        }

        int index = 0;

        while (index < data.length()) {

            int linkStart =
                findAnchorStart(data, index);

            if (linkStart < 0) {
                break;
            }

            int tagEnd =
                data.indexOf(">", linkStart);

            if (tagEnd < 0) {
                break;
            }

            String tag =
                data.substring(
                    linkStart,
                    tagEnd + 1
                );

            String link =
                extractHref(tag);

            int textEnd =
                data.indexOf(
                    "</a>",
                    tagEnd + 1
                );

            if (textEnd < 0) {
                textEnd =
                    data.indexOf(
                        "</A>",
                        tagEnd + 1
                    );
            }

            if (textEnd < 0) {
                index = tagEnd + 1;
                continue;
            }

            String title =
                data.substring(
                    tagEnd + 1,
                    textEnd
                );

            title = stripTags(title);
            title = decodeHtml(title);
            title = title.trim();

            if (
                link != null &&
                link.length() > 0 &&
                title.length() > 0
            ) {

                if (itemCount < itemUrls.length) {

                    itemUrls[itemCount] =
                        link;

                    itemNames[itemCount] =
                        title;

                    catalogList.append(
                        title,
                        null
                    );

                    itemCount++;
                }
            }

            index = textEnd + 4;

            if (itemCount >= 40) {
                break;
            }
        }

        if (itemCount == 0) {
            catalogList.append(
                "Ничего не найдено",
                null
            );

            appendLog(
                "Ссылки в HTML не найдены."
            );

        } else {
            appendLog(
                "Найдено элементов: " +
                itemCount
            );
        }
    }

    // ---------------------------------------------------------
    // Поиск начала ссылки
    // ---------------------------------------------------------

    private int findAnchorStart(
        String data,
        int from
    ) {
        int a =
            data.indexOf("<a", from);

        int b =
            data.indexOf("<A", from);

        if (a < 0) {
            return b;
        }

        if (b < 0) {
            return a;
        }

        return a < b ? a : b;
    }

    // ---------------------------------------------------------
    // Получение href
    // ---------------------------------------------------------

    private String extractHref(String tag) {

        if (tag == null) {
            return null;
        }

        String lower = tag.toLowerCase();

        int hrefPos =
            lower.indexOf("href");

        if (hrefPos < 0) {
            return null;
        }

        int equalPos =
            lower.indexOf(
                "=",
                hrefPos + 4
            );

        if (equalPos < 0) {
            return null;
        }

        int pos = equalPos + 1;

        while (
            pos < tag.length() &&
            (
                tag.charAt(pos) == ' ' ||
                tag.charAt(pos) == '\t' ||
                tag.charAt(pos) == '\r' ||
                tag.charAt(pos) == '\n'
            )
        ) {
            pos++;
        }

        if (pos >= tag.length()) {
            return null;
        }

        char quote =
            tag.charAt(pos);

        if (quote == '"' || quote == '\'') {

            int start = pos + 1;

            int end =
                tag.indexOf(
                    quote,
                    start
                );

            if (end > start) {
                return tag.substring(
                    start,
                    end
                );
            }

        } else {

            int start = pos;
            int end = start;

            while (
                end < tag.length() &&
                tag.charAt(end) != ' ' &&
                tag.charAt(end) != '\t' &&
                tag.charAt(end) != '\r' &&
                tag.charAt(end) != '\n' &&
                tag.charAt(end) != '>'
            ) {
                end++;
            }

            if (end > start) {
                return tag.substring(
                    start,
                    end
                );
            }
        }

        return null;
    }

    // ---------------------------------------------------------
    // Удаление HTML-тегов
    // ---------------------------------------------------------

    private String stripTags(String input) {

        if (input == null) {
            return "";
        }

        StringBuffer clean =
            new StringBuffer();

        boolean inTag = false;

        for (
            int i = 0;
            i < input.length();
            i++
        ) {

            char c =
                input.charAt(i);

            if (c == '<') {

                inTag = true;

            } else if (c == '>') {

                inTag = false;

            } else if (!inTag) {

                clean.append(c);
            }
        }

        return clean.toString();
    }

    // ---------------------------------------------------------
    // Простое декодирование HTML
    // ---------------------------------------------------------

    private String decodeHtml(String input) {

        if (input == null) {
            return "";
        }

        String result = input;

        result = replace(
            result,
            "&nbsp;",
            " "
        );

        result = replace(
            result,
            "&amp;",
            "&"
        );

        result = replace(
            result,
            "&quot;",
            "\""
        );

        result = replace(
            result,
            "&#39;",
            "'"
        );

        result = replace(
            result,
            "&lt;",
            "<"
        );

        result = replace(
            result,
            "&gt;",
            ">"
        );

        return result;
    }

    private String replace(
        String source,
        String oldText,
        String newText
    ) {
        StringBuffer result =
            new StringBuffer();

        int pos = 0;

        while (true) {

            int found =
                source.indexOf(
                    oldText,
                    pos
                );

            if (found < 0) {
                result.append(
                    source.substring(pos)
                );
                break;
            }

            result.append(
                source.substring(
                    pos,
                    found
                )
            );

            result.append(newText);

            pos =
                found + oldText.length();
        }

        return result.toString();
    }

    // ---------------------------------------------------------
    // URL-кодирование UTF-8
    // ---------------------------------------------------------

    private String urlEncode(String s) {

        if (s == null) {
            return "";
        }

        try {

            byte[] bytes =
                s.getBytes("UTF-8");

            StringBuffer sb =
                new StringBuffer();

            for (
                int i = 0;
                i < bytes.length;
                i++
            ) {

                int b =
                    bytes[i] & 0xFF;

                if (
                    (b >= 'a' && b <= 'z') ||
                    (b >= 'A' && b <= 'Z') ||
                    (b >= '0' && b <= '9') ||
                    b == '-' ||
                    b == '_' ||
                    b == '.' ||
                    b == '~'
                ) {

                    sb.append(
                        (char)b
                    );

                } else if (b == ' ') {

                    sb.append("+");

                } else {

                    sb.append("%");
                    sb.append(
                        toHex(b)
                    );
                }
            }

            return sb.toString();

        } catch (Exception e) {

            // Запасной вариант
            StringBuffer sb =
                new StringBuffer();

            for (
                int i = 0;
                i < s.length();
                i++
            ) {

                char c =
                    s.charAt(i);

                if (
                    (c >= 'a' && c <= 'z') ||
                    (c >= 'A' && c <= 'Z') ||
                    (c >= '0' && c <= '9') ||
                    c == '-' ||
                    c == '_' ||
                    c == '.'
                ) {

                    sb.append(c);

                } else if (c == ' ') {

                    sb.append("+");

                } else {

                    sb.append("%3F");
                }
            }

            return sb.toString();
        }
    }

    private String toHex(int b) {

        String hex =
            "0123456789ABCDEF";

        return ""
            + hex.charAt(
                (b >> 4) & 0x0F
            )
            + hex.charAt(
                b & 0x0F
            );
    }

    // ---------------------------------------------------------
    // Поиск
    // ---------------------------------------------------------

    private void executeSearch() {

        String query =
            searchInput.getString().trim();

        if (query.length() == 0) {
            return;
        }

        StringBuffer searchUrl =
            new StringBuffer();

        searchUrl.append(baseUrl);
        searchUrl.append("search/?q=");
        searchUrl.append(
            urlEncode(query)
        );

        loadUrlData(
            searchUrl.toString(),
            true
        );
    }

    // ---------------------------------------------------------
    // Открытие информации о приложении
    // ---------------------------------------------------------

    private void openAppDetails(int index) {

        if (
            index < 0 ||
            index >= itemCount
        ) {
            return;
        }

        String title =
            itemNames[index];

        selectedDownloadUrl =
            itemUrls[index];

        appTitleLabel.setText(
            title
        );

        appDescLabel.setText(
            "Объект каталога"
        );

        appUrlLabel.setText(
            selectedDownloadUrl
        );

        statusLabel.setText(
            "Готово к скачиванию"
        );

        display.setCurrent(
            detailForm
        );
    }

    // ---------------------------------------------------------
    // Формирование полного URL
    // ---------------------------------------------------------

    private String resolveUrl(
        String link
    ) {

        if (link == null) {
            return null;
        }

        link = link.trim();

        if (link.length() == 0) {
            return null;
        }

        if (
            link.startsWith("http://") ||
            link.startsWith("https://")
        ) {
            return link;
        }

        if (link.startsWith("/")) {

            int schemeEnd =
                baseUrl.indexOf("://");

            if (schemeEnd >= 0) {

                int hostEnd =
                    baseUrl.indexOf(
                        "/",
                        schemeEnd + 3
                    );

                if (hostEnd < 0) {
                    return baseUrl.substring(
                        0,
                        baseUrl.length()
                    ) + link;
                }

                return baseUrl.substring(
                    0,
                    hostEnd
                ) + link;
            }
        }

        String cleanBase =
            baseUrl;

        if (
            !cleanBase.endsWith("/")
        ) {
            cleanBase += "/";
        }

        while (
            link.startsWith("/")
        ) {
            link =
                link.substring(1);
        }

        return cleanBase + link;
    }

    // ---------------------------------------------------------
    // Скачивание / открытие ссылки
    // ---------------------------------------------------------

    private void startDownload() {

        if (
            selectedDownloadUrl == null ||
            selectedDownloadUrl.length() == 0
        ) {
            statusLabel.setText(
                "Ссылка отсутствует"
            );
            return;
        }

        try {

            String targetUrl =
                resolveUrl(
                    selectedDownloadUrl
                );

            if (
                targetUrl == null ||
                targetUrl.length() == 0
            ) {
                statusLabel.setText(
                    "Неверная ссылка"
                );
                return;
            }

            appendLog(
                "Открытие: " +
                targetUrl
            );

            statusLabel.setText(
                "Открытие ссылки..."
            );

            platformRequest(
                targetUrl
            );

        } catch (Exception e) {

            statusLabel.setText(
                "Ошибка: " +
                (
                    e.getMessage() != null
                    ? e.getMessage()
                    : e.toString()
                )
            );
        }
    }

    // ---------------------------------------------------------
    // Ошибка
    // ---------------------------------------------------------

    private void showErrorScreen(
        String message
    ) {

        Form errForm =
            new Form("Ошибка");

        errForm.append(
            new StringItem(
                "Детали: ",
                message
            )
        );

        errForm.addCommand(
            backCmd
        );

        errForm.setCommandListener(
            this
        );

        display.setCurrent(
            errForm
        );
    }

    // ---------------------------------------------------------
    // Критическая ошибка
    // ---------------------------------------------------------

    private void showFatalError(
        Throwable t
    ) {

        final Form errForm =
            new Form("Сбой запуска");

        String msg =
            t.getMessage();

        if (msg == null) {
            msg =
                t.getClass().getName();
        }

        errForm.append(
            new StringItem(
                "Ошибка: ",
                msg
            )
        );

        errForm.addCommand(
            exitCmd
        );

        errForm.setCommandListener(
            this
        );

        if (display != null) {
            display.setCurrent(
                errForm
            );
        }
    }

    // ---------------------------------------------------------
    // Жизненный цикл MIDlet
    // ---------------------------------------------------------

    protected void pauseApp() {
    }

    protected void destroyApp(
        boolean unconditional
    ) {
    }

    // ---------------------------------------------------------
    // Обработка команд
    // ---------------------------------------------------------

    public void commandAction(
        Command c,
        Displayable d
    ) {

        // Выход
        if (c == exitCmd) {

            try {
                destroyApp(true);
            } catch (Exception e) {
                // Игнорируем
            }

            notifyDestroyed();
            return;
        }

        // Выбор элемента каталога
        if (
            c == List.SELECT_COMMAND &&
            d == catalogList
        ) {

            int selected =
                catalogList.getSelectedIndex();

            if (
                selected >= 0 &&
                selected < itemCount
            ) {
                openAppDetails(
                    selected
                );
            }

            return;
        }

        // Назад
        if (c == backCmd) {

            if (
                d == catalogList ||
                d == searchForm ||
                d == settingsForm ||
                d == aboutForm ||
                d == debugForm
            ) {

                display.setCurrent(
                    mainMenuList
                );

            } else if (d == detailForm) {

                if (catalogList != null) {

                    display.setCurrent(
                        catalogList
                    );

                } else {

                    display.setCurrent(
                        mainMenuList
                    );
                }

            } else {

                display.setCurrent(
                    mainMenuList
                );
            }

            return;
        }

        // Поиск
        if (c == searchCmd) {

            display.setCurrent(
                searchForm
            );

            return;
        }

        // Настройки
        if (c == settingsCmd) {

            display.setCurrent(
                settingsForm
            );

            return;
        }

        // О программе
        if (c == aboutCmd) {

            display.setCurrent(
                aboutForm
            );

            return;
        }

        // Логи
        if (c == debugCmd) {

            display.setCurrent(
                debugForm
            );

            return;
        }

        // Обновление каталога или результатов поиска
        if (
            c == refreshCmd &&
            currentCatalogUrl != null
        ) {

            loadUrlData(
                currentCatalogUrl,
                currentIsSearch
            );

            return;
        }

        // Выполнение поиска
        if (
            c == executeSearchCmd &&
            d == searchForm
        ) {

            executeSearch();
            return;
        }

        // Скачать
        if (
            c == downloadCmd &&
            d == detailForm
        ) {

            startDownload();
            return;
        }

        // Главное меню
        if (d == mainMenuList) {

            int selected =
                mainMenuList.getSelectedIndex();

            switch (selected) {

                case 0:
                    loadUrlData(
                        baseUrl,
                        false
                    );
                    break;

                case 1:
                    loadUrlData(
                        baseUrl + "games/",
                        false
                    );
                    break;

                case 2:
                    loadUrlData(
                        baseUrl + "themes/",
                        false
                    );
                    break;

                case 3:
                    loadUrlData(
                        baseUrl + "new/",
                        false
                    );
                    break;

                case 4:
                    display.setCurrent(
                        searchForm
                    );
                    break;

                case 5:
                    display.setCurrent(
                        settingsForm
                    );
                    break;

                case 6:
                    display.setCurrent(
                        aboutForm
                    );
                    break;

                case 7:
                    display.setCurrent(
                        debugForm
                    );
                    break;
            }

            return;
        }
    }
}
```

**Важно:** это уже полный файл, а не отдельные исправления. Основные проблемы из предыдущей версии исправлены, в том числе выбор пункта каталога через `List.SELECT_COMMAND`, UTF-8 для поиска, `Обновить` для результатов поиска и обработка `href` с одинарными кавычками.
