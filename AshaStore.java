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
    private ImageItem appIconItem;
    private StringItem debugText;

    // Команды навигации
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

    // Переменные состояния и данных
    private String baseUrl;
    private String currentCatalogUrl;
    private String selectedDownloadUrl;
    private String[] itemUrls;
    private String[] itemNames;
    private Image[] itemImages;
    private int itemCount;

    public AshaStore() {
        // Пустой конструктор для предотвращения OutOfMemory на старте KVM
    }

    protected void startApp() {
        if (display == null) {
            display = Display.getDisplay(this);

            // Показываем заставку, чтобы телефон не решил, что приложение зависло
            Form splash = new Form("Asha Store");
            splash.append(new StringItem("", "Загрузка..."));
            display.setCurrent(splash);

            // Запускаем сборку интерфейса в отдельном потоке
            Thread initThread = new Thread(this);
            initThread.start();
        }
    }

    public void run() {
        try {
            baseUrl = "http://series40.kiev.ua/";
            itemUrls = new String[40];
            itemNames = new String[40];
            itemImages = new Image[40];
            itemCount = 0;

            initCommands();
            initMainMenu();
            initSearchForm();
            initDetailForm();
            initSettingsForm();
            initAboutForm();
            initDebugForm();

            display.callSerially(new Runnable() {
                public void run() {
                    display.setCurrent(mainMenuList);
                }
            });
        } catch (Throwable t) {
            showFatalError(t);
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
        mainMenuList.append("Системный лог", null);

        mainMenuList.addCommand(exitCmd);
        mainMenuList.addCommand(searchCmd);
        mainMenuList.addCommand(settingsCmd);
        mainMenuList.addCommand(aboutCmd);
        mainMenuList.setCommandListener(this);
    }

    private void initSearchForm() {
        searchForm = new Form("Поиск приложений");
        searchInput = new TextField("Поиск:", "", 32, TextField.ANY);
        searchForm.append(searchInput);

        searchForm.addCommand(executeSearchCmd);
        searchForm.addCommand(backCmd);
        searchForm.setCommandListener(this);
    }

    private void initDetailForm() {
        detailForm = new Form("Программа");
        statusLabel = new StringItem("Статус: ", "Готово");
        appTitleLabel = new StringItem("Название: ", "");
        appDescLabel = new StringItem("Описание: ", "");
        appUrlLabel = new StringItem("Ссылка: ", "");
        appIconItem = new ImageItem(null, null, ImageItem.LAYOUT_CENTER, "Изображение недоступно");

        detailForm.append(appIconItem);
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
        settingsForm.append(new StringItem("Сервер: ", baseUrl));
        settingsForm.append(new StringItem("Платформа: ", "Nokia Asha Platform 14.0.4"));
        settingsForm.addCommand(backCmd);
        settingsForm.setCommandListener(this);
    }

    private void initAboutForm() {
        aboutForm = new Form("О программе");

        // Вытягиваем версию из MANIFEST.MF
        String version = getAppProperty("MIDlet-Version");
        if (version == null || version.trim().length() == 0) {
            version = "1.01.1";
        }

        StringBuffer aboutText = new StringBuffer();
        aboutText.append("Магазин приложений для Nokia Asha\nВерсия: ");
        aboutText.append(version);
        aboutText.append("\nПлатформа: MIDP 2.1 / CLDC 1.1");

        aboutForm.append(new StringItem("Asha Store", aboutText.toString()));
        aboutForm.addCommand(backCmd);
        aboutForm.setCommandListener(this);
    }

    private void initDebugForm() {
        debugForm = new Form("Отладчик");
        debugText = new StringItem("Статус: ", "Система инициализирована.\n");
        debugForm.append(debugText);
        debugForm.addCommand(backCmd);
        debugForm.setCommandListener(this);
    }

    private void appendLog(final String msg) {
        display.callSerially(new Runnable() {
            public void run() {
                if (debugText != null) {
                    StringBuffer sb = new StringBuffer();
                    sb.append(debugText.getText());
                    sb.append("\n> ");
                    sb.append(msg);
                    debugText.setText(sb.toString());
                }
            }
        });
    }

    private void loadUrlData(final String requestUrl, final boolean isSearch) {
        currentCatalogUrl = requestUrl;
        catalogList = new List("Загрузка...", List.IMPLICIT);
        catalogList.addCommand(backCmd);
        catalogList.addCommand(searchCmd);
        catalogList.addCommand(refreshCmd);
        catalogList.setCommandListener(this);
        display.setCurrent(catalogList);

        appendLog("Запрос данных...");

        new Thread(new Runnable() {
            public void run() {
                HttpConnection conn = null;
                InputStream is = null;
                try {
                    conn = (HttpConnection) Connector.open(requestUrl);
                    conn.setRequestMethod(HttpConnection.GET);
                    conn.setRequestProperty("User-Agent", "NokiaAsha");

                    int responseCode = conn.getResponseCode();

                    if (responseCode == HttpConnection.HTTP_OK) {
                        is = conn.openInputStream();
                        StringBuffer sb = new StringBuffer();
                        int ch;
                        int readLimit = 0;
                        while ((ch = is.read()) != -1 && readLimit < 32000) {
                            sb.append((char) ch);
                            readLimit++;
                        }

                        final String responseData = sb.toString();
                        display.callSerially(new Runnable() {
                            public void run() {
                                parseData(responseData, isSearch);
                            }
                        });
                    } else {
                        final String errCode = "Ошибка сервера: " + responseCode;
                        display.callSerially(new Runnable() {
                            public void run() {
                                showErrorScreen(errCode);
                            }
                        });
                    }
                } catch (Throwable t) {
                    final String errMsg = "Ошибка сети: " + (t.getMessage() != null ? t.getMessage() : t.getClass().getName());
                    display.callSerially(new Runnable() {
                        public void run() {
                            showErrorScreen(errMsg);
                        }
                    });
                } finally {
                    try {
                        if (is != null) is.close();
                        if (conn != null) conn.close();
                    } catch (Exception e) {}
                }
            }
        }).start();
    }

    private void parseData(String data, boolean isSearch) {
        catalogList.setTitle(isSearch ? "Результаты поиска" : "Каталог");
        itemCount = 0;

        int index = 0;
        while ((index = data.indexOf("<a href=", index)) != -1) {
            int startUrl = data.indexOf("\"", index) + 1;
            int endUrl = data.indexOf("\"", startUrl);
            int startText = data.indexOf(">", endUrl) + 1;
            int endText = data.indexOf("</a>", startText);

            if (startUrl > 0 && endUrl > startUrl && startText > 0 && endText > startText) {
                String link = data.substring(startUrl, endUrl).trim();
                String title = stripTags(data.substring(startText, endText).trim());

                if (title.length() > 0 && itemCount < 40) {
                    itemUrls[itemCount] = link;
                    itemNames[itemCount] = title;
                    catalogList.append(title, null);
                    itemCount++;
                }
            }
            index = endText + 4;
            if (itemCount >= 40) break;
        }

        if (catalogList.size() == 0) {
            catalogList.append("Ничего не найдено", null);
        }
    }

    private String stripTags(String input) {
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
            appDescLabel.setText("Объект каталога");
            appUrlLabel.setText(selectedDownloadUrl);
            statusLabel.setText("Готово к скачиванию");

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

                appendLog("Запуск скачивания...");
                platformRequest(targetUrl.toString());
            } catch (Exception e) {
                statusLabel.setText("Ошибка: " + e.getMessage());
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

    private void showFatalError(Throwable t) {
        final Form errForm = new Form("Сбой запуска");
        String msg = t.getMessage() != null ? t.getMessage() : t.getClass().getName();
        errForm.append(new StringItem("Ошибка: ", msg));
        errForm.addCommand(exitCmd);
        errForm.setCommandListener(this);

        display.callSerially(new Runnable() {
            public void run() {
                display.setCurrent(errForm);
            }
        });
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
                    loadUrlData(baseUrl + "games", false);
                    break;
                case 2:
                    loadUrlData(baseUrl + "themes", false);
                    break;
                case 3:
                    loadUrlData(baseUrl + "new", false);
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
