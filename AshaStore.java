import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import java.io.*;

public class AshaStore extends MIDlet implements CommandListener {
    private Display display;

    // Экраны
    private List mainMenuList;
    private List catalogList;
    private Form searchForm;
    private Form detailForm;
    private Form settingsForm;
    private Form aboutForm;
    private Form debugForm;

    // Элементы ввода и вывода
    private TextField searchInput;
    private StringItem statusLabel;
    private StringItem appTitleLabel;
    private StringItem appDescLabel;
    private StringItem appUrlLabel;
    private StringItem debugText;

    // Команды
    private Command exitCmd;
    private Command backCmd;
    private Command selectCmd;
    private Command searchCmd;
    private Command executeSearchCmd;
    private Command downloadCmd;
    private Command settingsCmd;
    private Command aboutCmd;
    private Command debugCmd;
    private Command refreshCmd;

    // Данные и состояние
    private String baseUrl;
    private String currentCatalogUrl;
    private String selectedDownloadUrl;
    private String[] itemUrls;
    private String[] itemNames;
    private int itemCount;
    private int currentPage;

    public AshaStore() {
        baseUrl = "http://series40.kiev.ua/";
        itemUrls = new String[100];
        itemNames = new String[100];
        itemCount = 0;
        currentPage = 1;
    }

    protected void startApp() {
        if (display == null) {
            display = Display.getDisplay(this);
            initCommands();
            initMainMenu();
            initSearchForm();
            initDetailForm();
            initSettingsForm();
            initAboutForm();
            initDebugForm();
            
            display.setCurrent(mainMenuList);
        }
    }

    private void initCommands() {
        exitCmd = new Command("Выход", Command.EXIT, 1);
        backCmd = new Command("Назад", Command.BACK, 1);
        selectCmd = new Command("Выбрать", Command.OK, 1);
        searchCmd = new Command("Поиск", Command.SCREEN, 2);
        executeSearchCmd = new Command("Искать", Command.OK, 1);
        downloadCmd = new Command("Скачать", Command.SCREEN, 1);
        settingsCmd = new Command("Настройки", Command.SCREEN, 3);
        aboutCmd = new Command("О программе", Command.SCREEN, 4);
        debugCmd = new Command("Логи", Command.SCREEN, 5);
        refreshCmd = new Command("Обновить", Command.SCREEN, 2);
    }

    private void initMainMenu() {
        mainMenuList = new List("Asha Store", List.IMPLICIT);
        mainMenuList.append("Каталог приложений", null);
        mainMenuList.append("Игры", null);
        mainMenuList.append("Темы", null);
        mainMenuList.append("Новинки", null);
        mainMenuList.append("Поиск", null);
        mainMenuList.append("Настройки", null);
        mainMenuList.append("О программе", null);
        mainMenuList.append("Отладка", null);

        mainMenuList.addCommand(exitCmd);
        mainMenuList.addCommand(searchCmd);
        mainMenuList.addCommand(settingsCmd);
        mainMenuList.addCommand(aboutCmd);
        mainMenuList.setCommandListener(this);
    }

    private void initSearchForm() {
        searchForm = new Form("Поиск в Store");
        searchInput = new TextField("Введите запрос:", "", 32, TextField.ANY);
        searchForm.append(searchInput);
        
        searchForm.addCommand(executeSearchCmd);
        searchForm.addCommand(backCmd);
        searchForm.setCommandListener(this);
    }

    private void initDetailForm() {
        detailForm = new Form("Информация");
        statusLabel = new StringItem("Статус: ", "Готово");
        appTitleLabel = new StringItem("Название: ", "");
        appDescLabel = new StringItem("Описание: ", "");
        appUrlLabel = new StringItem("Ссылка: ", "");

        detailForm.append(statusLabel);
        detailForm.append(appTitleLabel);
        detailForm.append(appDescLabel);
        detailForm.append(appUrlLabel);

        detailForm.addCommand(downloadCmd);
        detailForm.addCommand(backCmd);
        detailForm.setCommandListener(this);
    }

    private void initSettingsForm() {
        settingsForm = new Form("Настройки");
        StringItem info = new StringItem("Сервер: ", baseUrl);
        StringItem userAgent = new StringItem("User-Agent: ", "NokiaS40/AshaStore");
        settingsForm.append(info);
        settingsForm.append(userAgent);
        settingsForm.addCommand(backCmd);
        settingsForm.setCommandListener(this);
    }

    private void initAboutForm() {
        aboutForm = new Form("О программе");
        aboutForm.append(new StringItem("Asha Store", "Клиент каталога для Nokia S40\nВерсия: 1.0.0\nПлатформа: MIDP 2.1 / CLDC 1.1"));
        aboutForm.addCommand(backCmd);
        aboutForm.setCommandListener(this);
    }

    private void initDebugForm() {
        debugForm = new Form("Системный лог");
        debugText = new StringItem("Лог: ", "Запуск системы прошел успешно.\n");
        debugForm.append(debugText);
        debugForm.addCommand(backCmd);
        debugForm.setCommandListener(this);
    }

    private void logDebug(String message) {
        StringBuffer sb = new StringBuffer();
        sb.append(debugText.getText());
        sb.append("\n> ");
        sb.append(message);
        debugText.setText(sb.toString());
    }

    private void loadUrlData(final String requestUrl, final boolean isSearch) {
        currentCatalogUrl = requestUrl;
        catalogList = new List("Загрузка...", List.IMPLICIT);
        catalogList.addCommand(backCmd);
        catalogList.addCommand(searchCmd);
        catalogList.addCommand(refreshCmd);
        catalogList.setCommandListener(this);
        display.setCurrent(catalogList);

        logDebug("Запрос: ");
        logDebug(requestUrl);

        new Thread(new Runnable() {
            public void run() {
                HttpConnection conn = null;
                InputStream is = null;
                try {
                    conn = (HttpConnection) Connector.open(requestUrl);
                    conn.setRequestMethod(HttpConnection.GET);
                    conn.setRequestProperty("User-Agent", "NokiaS40/AshaStore");

                    int responseCode = conn.getResponseCode();
                    logDebug("Ответ от сервера получен.");

                    if (responseCode == HttpConnection.HTTP_OK) {
                        is = conn.openInputStream();
                        StringBuffer sb = new StringBuffer();
                        int ch;
                        int readLimit = 0;
                        while ((ch = is.read()) != -1 && readLimit < 64000) {
                            sb.append((char) ch);
                            readLimit++;
                        }
                        
                        logDebug("Размер полученных данных обработан.");
                        if (isSearch) {
                            parseSearchResults(sb.toString());
                        } else {
                            parseCatalogHtml(sb.toString());
                        }
                    } else {
                        StringBuffer errSb = new StringBuffer();
                        errSb.append("Ошибка сервера: ");
                        errSb.append(responseCode);
                        logDebug(errSb.toString());
                        showErrorScreen(errSb.toString());
                    }
                } catch (Throwable t) {
                    StringBuffer tSb = new StringBuffer();
                    tSb.append("Ошибка подключения: ");
                    if (t.getMessage() != null) {
                        tSb.append(t.getMessage());
                    } else {
                        tSb.append(t.getClass().getName());
                    }
                    logDebug(tSb.toString());
                    showErrorScreen(tSb.toString());
                } finally {
                    try {
                        if (is != null) is.close();
                        if (conn != null) conn.close();
                    } catch (Exception e) {}
                }
            }
        }).start();
    }

    private void parseCatalogHtml(String html) {
        catalogList.setTitle("Каталог");
        itemCount = 0;
        int index = 0;

        while ((index = html.indexOf("<a href=", index)) != -1) {
            int startUrl = html.indexOf("\"", index) + 1;
            int endUrl = html.indexOf("\"", startUrl);
            int startText = html.indexOf(">", endUrl) + 1;
            int endText = html.indexOf("</a>", startText);

            if (startUrl > 0 && endUrl > startUrl && startText > 0 && endText > startText) {
                String link = html.substring(startUrl, endUrl).trim();
                String title = cleanHtmlTags(html.substring(startText, endText).trim());

                if (title.length() > 0 && itemCount < 100) {
                    itemUrls[itemCount] = link;
                    itemNames[itemCount] = title;
                    catalogList.append(title, null);
                    itemCount++;
                }
            }
            index = endText + 4;
            if (itemCount >= 100) break;
        }

        if (catalogList.size() == 0) {
            catalogList.append("Пусто или нет данных", null);
        }
    }

    private String cleanHtmlTags(String input) {
        StringBuffer clean = new StringBuffer();
        boolean inTag = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
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

    private void parseSearchResults(String html) {
        catalogList.setTitle("Результаты поиска");
        parseCatalogHtml(html);
    }

    private void executeSearch() {
        String query = searchInput.getString().trim();
        if (query.length() > 0) {
            StringBuffer searchUrlSb = new StringBuffer();
            searchUrlSb.append(baseUrl);
            searchUrlSb.append("search?q=");
            searchUrlSb.append(query);
            
            loadUrlData(searchUrlSb.toString(), true);
        }
    }

    private void openAppDetails(int index) {
        if (index >= 0 && index < itemCount) {
            String title = itemNames[index];
            selectedDownloadUrl = itemUrls[index];

            appTitleLabel.setText(title);
            appDescLabel.setText("Объект из базы данных Asha Store.");
            appUrlLabel.setText(selectedDownloadUrl);
            statusLabel.setText("Готов к скачиванию");

            display.setCurrent(detailForm);
        }
    }

    private void startDownload() {
        if (selectedDownloadUrl != null && selectedDownloadUrl.length() > 0) {
            try {
                StringBuffer targetUrl = new StringBuffer();
                if (!selectedDownloadUrl.startsWith("http")) {
                    targetUrl.append(baseUrl);
                    if (!selectedDownloadUrl.startsWith("/")) {
                        targetUrl.append("/");
                    }
                }
                targetUrl.append(selectedDownloadUrl);

                logDebug("Передача в браузер: ");
                logDebug(targetUrl.toString());

                platformRequest(targetUrl.toString());
            } catch (Exception e) {
                StringBuffer errSb = new StringBuffer();
                errSb.append("Сбой передачи: ");
                errSb.append(e.getMessage());
                statusLabel.setText(errSb.toString());
                logDebug(errSb.toString());
            }
        }
    }

    private void showErrorScreen(String message) {
        Form errForm = new Form("Ошибка");
        errForm.append(new StringItem("Детали: ", message));
        errForm.addCommand(backCmd);
        errForm.setCommandListener(this);
        display.setCurrent(errForm);
    }

    protected void pauseApp() {}

    protected void destroyApp(boolean unconditional) {}

    public void commandAction(Command c, Displayable d) {
        if (c == exitCmd) {
            notifyDestroyed();
        } else if (c == backCmd) {
            if (d == catalogList || d == searchForm || d == settingsForm || d == aboutForm || d == debugForm) {
                display.setCurrent(mainMenuList);
            } else if (d == detailForm) {
                if (catalogList != null) {
                    display.setCurrent(catalogList);
                } else {
                    display.setCurrent(mainMenuList);
                }
            } else {
                display.setCurrent(mainMenuList);
            }
        } else if (c == searchCmd) {
            display.setCurrent(searchForm);
        } else if (c == settingsCmd) {
            display.setCurrent(settingsForm);
        } else if (c == aboutCmd) {
            display.setCurrent(aboutForm);
        } else if (c == debugCmd) {
            display.setCurrent(debugForm);
        } else if (c == refreshCmd && currentCatalogUrl != null) {
            loadUrlData(currentCatalogUrl, false);
        } else if (c == executeSearchCmd && d == searchForm) {
            executeSearch();
        } else if (c == downloadCmd && d == detailForm) {
            startDownload();
        } else if (d == mainMenuList && c == List.SELECT_COMMAND) {
            int selected = mainMenuList.getSelectedIndex();
            switch (selected) {
                case 0:
                    loadUrlData(baseUrl, false);
                    break;
                case 1:
                    StringBuffer gamesSb = new StringBuffer();
                    gamesSb.append(baseUrl);
                    gamesSb.append("games");
                    loadUrlData(gamesSb.toString(), false);
                    break;
                case 2:
                    StringBuffer themesSb = new StringBuffer();
                    themesSb.append(baseUrl);
                    themesSb.append("themes");
                    loadUrlData(themesSb.toString(), false);
                    break;
                case 3:
                    StringBuffer newSb = new StringBuffer();
                    newSb.append(baseUrl);
                    newSb.append("new");
                    loadUrlData(newSb.toString(), false);
                    break;
                case 4:
                    display.setCurrent(searchForm);
                    break;
                case 5:
                    display.setCurrent(settingsForm);
                    break;
                case 6:
                    display.setCurrent(aboutForm);
                    break;
                case 7:
                    display.setCurrent(debugForm);
                    break;
            }
        } else if (d == catalogList && c == List.SELECT_COMMAND) {
            int selected = catalogList.getSelectedIndex();
            openAppDetails(selected);
        }
    }
}
